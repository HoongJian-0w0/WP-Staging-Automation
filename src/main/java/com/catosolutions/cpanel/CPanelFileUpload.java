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

public class CPanelFileUpload {

    private static void terminateWithMessage(WebDriver driver, String message) {
        System.out.println("[ERR] ❌ " + message);
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
            Ui.uploadDriverLock = true;

            WebDriver driver = null;
            try {
                System.out.println("[INFO] 🟢 Upload thread started");

                if (Ui.uploadShouldStop) {
                    terminateWithMessage(driver, "Cancelled before start.");
                    return;
                }

                System.out.println("[INFO] 🔐 Attempting to login and ensure valid session...");
                String sessionToken = CPanelLogin.ensureValidLogin(url, username, password);
                if (Ui.uploadShouldStop || sessionToken.isEmpty()) {
                    terminateWithMessage(driver, "Login failed or session could not be established.");
                    return;
                }

                System.out.println("[INFO] 🚀 Launching ChromeDriver for upload...");
                driver = ChromeDriver.getUploadDriver();
                if (Ui.uploadShouldStop || driver == null) {
                    terminateWithMessage(driver, "Browser launch failed.");
                    return;
                }

                String loginUrl = DomainUitls.normalizeCpanelUrl(url);
                driver.get(loginUrl);
                ChromeDriver.syncCookiesToUploadDriver(url, username, password);
                driver.navigate().refresh();
                System.out.println("[INFO] 🔄 Cookies synced and page refreshed.");

                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
                String fileManagerUrl = loginUrl + sessionToken + "/frontend/jupiter/filemanager/index.html";

                System.out.println("[INFO] 📁 Navigating to File Manager: " + fileManagerUrl);
                driver.get(fileManagerUrl);
                if (Ui.uploadShouldStop) {
                    terminateWithMessage(driver, "Terminated before navigation.");
                    return;
                }

                try {
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[text()='public_html']"))).click();
                    System.out.println("[INFO] ✅ Found and clicked 'public_html' folder.");
                } catch (TimeoutException e) {
                    Dialog.ErrorDialog("❌ 'public_html' folder not found in File Manager. Cannot proceed with upload.");
                    return;
                }

                Map<String, String> skippedDomains = new LinkedHashMap<>();

                for (int i = 0; i < originalDomains.size(); i++) {
                    if (Ui.uploadShouldStop) {
                        terminateWithMessage(driver, "Terminated during upload loop.");
                        return;
                    }

                    String domain = originalDomains.get(i);
                    String folder = (i < domains.size()) ? domains.get(i) : null;
                    if (folder == null) continue;

                    System.out.println("[INFO] 🔍 Looking for backup of domain: " + domain);
                    String backupPath = WordPressBackupFinder.findLatestBackupForDomain(backupDir, domain);
                    if (backupPath == null) {
                        skippedDomains.put(domain, "Backup file not found in backup directory.");
                        System.out.println("[WARN] [" + domain + "] ❌ Backup not found. Skipping.");
                        continue;
                    }

                    System.out.println("[INFO] 📦 Found backup: " + backupPath);

                    ((JavascriptExecutor) driver).executeScript("window.open('about:blank','_blank');");
                    Thread.sleep(1000);
                    List<String> allTabs = new ArrayList<>(driver.getWindowHandles());
                    String navTab = allTabs.get(allTabs.size() - 1);
                    driver.switchTo().window(navTab);
                    driver.get(fileManagerUrl);

                    try {
                        WebDriverWait uploadWait = new WebDriverWait(driver, Duration.ofSeconds(30));
                        uploadWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[text()='public_html']"))).click();
                        Thread.sleep(1000);
                        uploadWait.until(ExpectedConditions.elementToBeClickable(By.xpath("//span[normalize-space()='" + folder + "']"))).click();
                        Thread.sleep(1000);
                        uploadWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[text()='wp-content']"))).click();
                        Thread.sleep(1000);
                        uploadWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[text()='ai1wm-backups']"))).click();
                        Thread.sleep(1000);

                        System.out.println("[INFO] 📂 Navigated to: public_html/" + folder + "/wp-content/ai1wm-backups");

                        Set<String> tabsBeforeClick = new HashSet<>(driver.getWindowHandles());
                        driver.findElement(By.id("action-upload")).click();
                        Thread.sleep(1500);

                        Set<String> tabsAfterClick = new HashSet<>(driver.getWindowHandles());
                        tabsAfterClick.removeAll(tabsBeforeClick);
                        if (tabsAfterClick.isEmpty()) {
                            skippedDomains.put(domain, "Upload tab not opened.");
                            System.out.println("[ERR] [" + domain + "] ❌ Upload tab failed to open. Skipping.");
                            driver.close();
                            driver.switchTo().window(allTabs.get(0));
                            continue;
                        }

                        String uploadTab = tabsAfterClick.iterator().next();
                        driver.switchTo().window(uploadTab);
                        System.out.println("[INFO] [" + domain + "] 📤 Upload tab opened (Handle: " + uploadTab + ")");
                        System.out.println("[INFO] [" + domain + "] ⏫ Sending file to upload input...");

                        uploadWait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[type='file']"))).sendKeys(backupPath);
                        Thread.sleep(2000);

                        System.out.println("[INFO] [" + domain + "] ✅ Upload triggered for: " + backupPath);

                        driver.switchTo().window(navTab);
                        driver.close();
                        driver.switchTo().window(uploadTab);

                    } catch (Exception e) {
                        skippedDomains.put(domain, "Upload setup failed: " + e.getMessage());
                        System.out.println("[ERR] [" + domain + "] ❌ Upload setup failed: " + e.getMessage());
                        try { driver.close(); } catch (Exception ignored) {}
                        driver.switchTo().window(allTabs.get(0));
                    }
                }

                driver.switchTo().window(new ArrayList<>(driver.getWindowHandles()).get(0));
                System.out.println("[INFO] 📁 All uploads triggered. Returning to File Manager.");

                if (!skippedDomains.isEmpty()) {
                    StringBuilder summary = new StringBuilder();
                    summary.append("\n--- Skipped Domains Summary ---\n");
                    for (Map.Entry<String, String> skip : skippedDomains.entrySet()) {
                        summary.append("[").append(skip.getKey()).append("] ❌ ").append(skip.getValue()).append("\n");
                    }
                    summary.append("--- End of Skipped List ---\n");
                    Dialog.AlertDialog(summary.toString());
                    System.out.println("[WARN] ⚠️ Upload completed with skipped domains.");
                } else {
                    Dialog.SuccessDialog("✅ All backup uploads triggered successfully!");
                    System.out.println("[INFO] ✅ All uploads triggered successfully.");
                }

            } catch (Exception e) {
                e.printStackTrace();
                Dialog.ErrorDialog("Upload error:\n" + e.getMessage());
                System.out.println("[ERR] ❌ Upload failed with exception: " + e.getMessage());
            } finally {
                Ui.uploadDriverLock = false;
            }
        }).start();
    }
}
