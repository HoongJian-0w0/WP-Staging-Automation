package com.catosolutions.cpanel;

import com.catosolutions.chromedriver.ChromeDriver;
import com.catosolutions.ui.Ui;
import com.catosolutions.utils.Dialog;
import com.catosolutions.utils.DomainUitls;
import com.catosolutions.utils.WordPressBackupFinder;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.*;
import java.util.NoSuchElementException;

public class CPanelFileUpload {

    private static void terminateWithMessage(WebDriver driver, String message) {
        System.out.println("[UPLOAD] ❌ " + message);
        Dialog.AlertDialog("🛑 Upload terminated: " + message);
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception ignored) {}
        }
    }

    public static void uploadBackupFiles(String url, String username, String password,
                                         List<String> originalDomains, List<String> domains, String backupDir) {
        new Thread(() -> {
            WebDriver driver = null;
            try {
                if (Ui.uploadShouldStop) {
                    terminateWithMessage(driver, "Cancelled before start.");
                    return;
                }

                String sessionToken = CPanelLogin.ensureValidLogin(url, username, password);
                if (Ui.uploadShouldStop || sessionToken.isEmpty()) {
                    terminateWithMessage(driver, "Login failed or session could not be established.");
                    return;
                }

                driver = ChromeDriver.getUploadDriver();
                if (Ui.uploadShouldStop || driver == null) {
                    terminateWithMessage(driver, "Browser launch failed.");
                    return;
                }

                String loginUrl = DomainUitls.normalizeCpanelUrl(url);
                driver.get(loginUrl);
                ChromeDriver.syncCookiesToUploadDriver(url, username, password);
                driver.navigate().refresh();

                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

                String fileManagerUrl = loginUrl + sessionToken + "/frontend/jupiter/filemanager/index.html";
                driver.get(fileManagerUrl);
                if (Ui.uploadShouldStop) {
                    terminateWithMessage(driver, "Terminated before navigation.");
                    return;
                }

                try {
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[text()='public_html']"))).click();
                } catch (TimeoutException e) {
                    Dialog.ErrorDialog("❌ 'public_html' folder not found in File Manager. Cannot proceed with upload.");
                    return;
                }

                Map<String, String> tabMap = new LinkedHashMap<>();
                Map<String, String> skippedDomains = new LinkedHashMap<>();

                for (int i = 0; i < originalDomains.size(); i++) {
                    if (Ui.uploadShouldStop) {
                        terminateWithMessage(driver, "Terminated during upload loop.");
                        return;
                    }

                    String domain = originalDomains.get(i);
                    String folder = (i < domains.size()) ? domains.get(i) : null;
                    if (folder == null) continue;

                    String backupPath = WordPressBackupFinder.findLatestBackupForDomain(backupDir, domain);
                    if (backupPath == null) {
                        skippedDomains.put(domain, "Backup file not found in backup directory.");
                        continue;
                    }

                    ((JavascriptExecutor) driver).executeScript("window.open('about:blank','_blank');");
                    Thread.sleep(1000);
                    List<String> allTabs = new ArrayList<>(driver.getWindowHandles());
                    String navTab = allTabs.get(allTabs.size() - 1);

                    driver.switchTo().window(navTab);
                    driver.get(fileManagerUrl);

                    try {
                        WebDriverWait uploadWait = new WebDriverWait(driver, Duration.ofSeconds(30));
                        if (Ui.uploadShouldStop) {
                            terminateWithMessage(driver, "Terminated during upload prep.");
                            return;
                        }

                        uploadWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[text()='public_html']"))).click();
                        Thread.sleep(500);

                        WebElement folderElement = uploadWait.until(
                                ExpectedConditions.elementToBeClickable(By.xpath("//span[normalize-space()='" + folder + "']")));
                        folderElement.click();

                        Thread.sleep(500);
                        uploadWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[text()='wp-content']"))).click();
                        Thread.sleep(500);
                        uploadWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[text()='ai1wm-backups']"))).click();
                        Thread.sleep(500);

                        Set<String> tabsBeforeClick = new HashSet<>(driver.getWindowHandles());
                        driver.findElement(By.id("action-upload")).click();
                        Thread.sleep(1500);

                        Set<String> tabsAfterClick = new HashSet<>(driver.getWindowHandles());
                        tabsAfterClick.removeAll(tabsBeforeClick);
                        if (tabsAfterClick.isEmpty()) {
                            skippedDomains.put(domain, "Upload tab not opened.");
                            driver.close();
                            driver.switchTo().window(allTabs.get(0));
                            continue;
                        }

                        String uploadTab = tabsAfterClick.iterator().next();
                        driver.switchTo().window(uploadTab);
                        uploadWait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[type='file']"))).sendKeys(backupPath);
                        Thread.sleep(2000);

                        boolean fileExistsDialog = false;
                        try {
                            WebElement overwriteDialog = driver.findElement(By.id("sdlg1"));
                            if (overwriteDialog.isDisplayed()) {
                                skippedDomains.put(domain, "File already exists. Assumed already uploaded.");
                                fileExistsDialog = true;
                            }
                        } catch (NoSuchElementException ignored) {}

                        if (fileExistsDialog) {
                            try { driver.close(); } catch (Exception ignored) {}
                            try { driver.switchTo().window(navTab); driver.close(); } catch (Exception ignored) {}
                            driver.switchTo().window(allTabs.get(0));
                            continue;
                        }

                        System.out.println("[" + domain + "] ✅ Upload triggered in upload tab.");
                        driver.switchTo().window(navTab); driver.close();
                        driver.switchTo().window(uploadTab);
                        tabMap.put(uploadTab, domain);

                    } catch (Exception e) {
                        skippedDomains.put(domain, "Upload setup failed: " + e.getMessage());
                        try { driver.close(); } catch (Exception ignored) {}
                        driver.switchTo().window(allTabs.get(0));
                    }
                }

                Map<String, Boolean> completed = new HashMap<>();
                for (String tab : tabMap.keySet()) {
                    completed.put(tab, false);
                }

                long start = System.currentTimeMillis();
                long timeout = Duration.ofMinutes(20).toMillis();

                while (true) {
                    if (Ui.uploadShouldStop) {
                        terminateWithMessage(driver, "Terminated during monitoring.");
                        return;
                    }

                    boolean allDone = true;

                    for (Map.Entry<String, String> entry : tabMap.entrySet()) {
                        String tabHandle = entry.getKey();
                        String domain = entry.getValue();

                        if (completed.get(tabHandle)) continue;
                        try {
                            driver.switchTo().window(tabHandle);
                            WebDriverWait tabWait = new WebDriverWait(driver, Duration.ofSeconds(15));
                            tabWait.until(ExpectedConditions.presenceOfElementLocated(By.id("progress1")));

                            WebElement progressContainer = driver.findElement(By.id("progress1"));
                            String barClass = progressContainer.findElement(By.className("progress-bar")).getAttribute("class");
                            String percentText = progressContainer.getText().replace("%", "").trim();
                            int percent = percentText.matches("\\d+") ? Integer.parseInt(percentText) : -1;
                            System.out.println("[" + domain + "] ⏳ Uploading... " + percent + "%");

                            boolean isComplete = barClass.contains("progress-bar-success") ||
                                    barClass.contains("progress-bar-danger") ||
                                    driver.findElement(By.id("uploaderstats1")).getText().toLowerCase().contains("cancelled");

                            if (isComplete) {
                                System.out.println("[" + domain + "] ✅ Upload completed.");
                                completed.put(tabHandle, true);
                                try { driver.close(); } catch (Exception ignored) {}
                                driver.switchTo().window(driver.getWindowHandles().iterator().next());
                            } else {
                                allDone = false;
                            }

                        } catch (Exception e) {
                            System.out.println("[" + domain + "] ❌ Monitor crashed: " + e.getMessage());
                            completed.put(tabHandle, true);
                        }
                    }

                    if (allDone || (System.currentTimeMillis() - start > timeout)) break;
                    if (Ui.uploadShouldStop) return;
                    Thread.sleep(6000);
                }

                driver.switchTo().window(new ArrayList<>(driver.getWindowHandles()).get(0));
                System.out.println("[UPLOAD] ✅ All uploads monitored. Returning to File Manager.");

                if (!skippedDomains.isEmpty()) {
                    StringBuilder summary = new StringBuilder();
                    summary.append("\n--- Skipped Domains Summary ---\n");
                    for (Map.Entry<String, String> skip : skippedDomains.entrySet()) {
                        summary.append("[" + skip.getKey() + "] ❌ " + skip.getValue() + "\n");
                    }
                    summary.append("--- End of Skipped List ---\n");
                    Dialog.AlertDialog(summary.toString());
                    System.out.println("[UPLOAD] ⚠️ Upload completed with skipped domains.");
                } else {
                    Dialog.SuccessDialog("✅ All backups uploaded successfully!");
                    System.out.println("[UPLOAD] ✅ All uploads completed successfully.");
                }

            } catch (Exception e) {
                e.printStackTrace();
                Dialog.ErrorDialog("Upload error:\n" + e.getMessage());
            } finally {
                if (driver != null) {
                    try {
                        driver.quit();
                    } catch (Exception ignored) {}
                }
            }
        }).start();
    }
}
