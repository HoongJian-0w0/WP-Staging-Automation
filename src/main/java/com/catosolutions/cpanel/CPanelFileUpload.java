package com.catosolutions.cpanel;

import com.catosolutions.chromedriver.ChromeDriver;
import com.catosolutions.utils.Dialog;
import com.catosolutions.utils.DomainUitls;
import com.catosolutions.utils.WordPressBackupFinder;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.*;

public class CPanelFileUpload {

    public static void uploadBackupFiles(String url, String username, String password,
                                         List<String> originalDomains, List<String> domains, String backupDir) {
        new Thread(() -> {
            try {
                // Step 1: Ensure session is valid BEFORE launching upload driver
                String sessionToken = CPanelLogin.ensureValidLogin(url, username, password);
                if (sessionToken.isEmpty()) {
                    Dialog.ErrorDialog("❌ Login failed or session could not be established.");
                    return;
                }

                // Step 2: Now launch upload browser and sync cookies
                WebDriver driver = ChromeDriver.getUploadDriver();
                if (driver == null) {
                    Dialog.ErrorDialog("Failed to launch upload browser.");
                    return;
                }

                String loginUrl = DomainUitls.normalizeCpanelUrl(url);
                driver.get(loginUrl); // Load the base URL before syncing cookies
                ChromeDriver.syncCookiesToUploadDriver(); // Sync cookies from main to upload driver
                driver.navigate().refresh(); // Refresh to apply session

                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

                String fileManagerUrl = loginUrl + sessionToken + "/frontend/jupiter/filemanager/index.html";
                driver.get(fileManagerUrl);
                wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[text()='public_html']"))).click();

                Map<String, String> tabMap = new LinkedHashMap<>();
                Map<String, String> skippedDomains = new LinkedHashMap<>();

                // Phase 1: Trigger uploads
                for (int i = 0; i < originalDomains.size(); i++) {
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
                        uploadWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[text()='public_html']"))).click();
                        Thread.sleep(500);

                        try {
                            uploadWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[text()='" + folder + "']"))).click();
                        } catch (Exception e) {
                            skippedDomains.put(domain, "Folder '" + folder + "' not found.");
                            driver.close();
                            continue;
                        }

                        Thread.sleep(500);
                        try {
                            uploadWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[text()='wp-content']"))).click();
                        } catch (Exception e) {
                            skippedDomains.put(domain, "Folder 'wp-content' not found.");
                            driver.close();
                            continue;
                        }

                        Thread.sleep(500);
                        try {
                            uploadWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[text()='ai1wm-backups']"))).click();
                        } catch (Exception e) {
                            skippedDomains.put(domain, "Folder 'ai1wm-backups' not found. Plugin may not be installed.");
                            driver.close();
                            continue;
                        }

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
                        System.out.println("[" + domain + "] ✅ Upload triggered in upload tab.");

                        driver.switchTo().window(navTab);
                        driver.close();

                        driver.switchTo().window(uploadTab);
                        tabMap.put(uploadTab, domain);

                    } catch (Exception e) {
                        skippedDomains.put(domain, "Upload setup failed: " + e.getMessage());
                        driver.close();
                        driver.switchTo().window(allTabs.get(0));
                    }
                }

                // Phase 2: Sequential monitoring loop
                Map<String, Boolean> completed = new HashMap<>();
                for (String tab : tabMap.keySet()) {
                    completed.put(tab, false);
                }

                long start = System.currentTimeMillis();
                long timeout = Duration.ofMinutes(20).toMillis();

                while (true) {
                    boolean allDone = true;

                    for (Map.Entry<String, String> entry : tabMap.entrySet()) {
                        String tabHandle = entry.getKey();
                        String domain = entry.getValue();

                        if (completed.get(tabHandle)) continue;

                        try {
                            driver.switchTo().window(tabHandle);

                            try {
                                WebDriverWait tabWait = new WebDriverWait(driver, Duration.ofSeconds(15));
                                tabWait.until(ExpectedConditions.presenceOfElementLocated(By.id("progress1")));
                            } catch (TimeoutException te) {
                                System.out.println("[" + domain + "] ⚠️ Timeout: #progress1 not found after 15s.");
                                completed.put(tabHandle, true);
                                continue;
                            }

                            WebElement progressContainer = driver.findElement(By.id("progress1"));
                            String style = progressContainer.getAttribute("style");

                            String barClass = "";
                            try {
                                WebElement innerBar = progressContainer.findElement(By.className("progress-bar"));
                                barClass = innerBar.getAttribute("class");
                                System.out.println("[" + domain + "] Progress bar class: " + barClass);
                            } catch (Exception e) {
                            }

                            String percentText = progressContainer.getText().replace("%", "").trim();
                            int percent = percentText.matches("\\d+") ? Integer.parseInt(percentText) : -1;

                            System.out.println("[" + domain + "] ⏳ Uploading... " + percent + "%");

                            boolean isSuccess = barClass.contains("progress-bar-success");
                            boolean isFailed = barClass.contains("progress-bar-danger");

                            WebElement stats = null;
                            try {
                                stats = driver.findElement(By.id("uploaderstats1"));
                            } catch (Exception ignored) {}

                            String status = stats != null ? stats.getText().toLowerCase() : "";
                            boolean isHttp500 = status.contains("http error 500");
                            boolean isCancelled = status.contains("cancelled");

                            boolean isComplete = isSuccess || isFailed || isCancelled || isHttp500;

                            if (style.contains("width: 100%") && !isComplete) {
                                System.out.println("[" + domain + "] ⚠️ Progress full but bar not green/red yet.");
                            }

                            if (isComplete) {
                                if (isHttp500) {
                                    System.out.println("[" + domain + "] ⚠️ HTTP 500 but full bar – assuming success.");
                                } else if (isCancelled) {
                                    System.out.println("[" + domain + "] ❌ Upload was cancelled.");
                                } else if (isFailed) {
                                    String errorDetail = "(no error message)";
                                    try {
                                        WebElement errorStats = driver.findElement(By.id("uploaderstats1"));
                                        String text = errorStats.getText().trim();
                                        if (!text.isEmpty()) errorDetail = text;
                                    } catch (Exception ignored) {}
                                    System.out.println("[" + domain + "] ❌ Upload failed (red bar): " + errorDetail);
                                } else {
                                    System.out.println("[" + domain + "] ✅ Upload completed.");
                                }

                                completed.put(tabHandle, true);

                                try {
                                    driver.close();
                                } catch (Exception ignored) {}

                                try {
                                    Set<String> handles = driver.getWindowHandles();
                                    if (!handles.isEmpty()) {
                                        driver.switchTo().window(handles.iterator().next());
                                    }
                                } catch (Exception ignored) {}
                            } else {
                                allDone = false;
                            }

                        } catch (Exception e) {
                            System.out.println("[" + domain + "] ❌ Monitor crashed: " + e.getMessage());
                            completed.put(tabHandle, true);
                        }
                    }

                    if (allDone || (System.currentTimeMillis() - start > timeout)) break;
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
                }

            } catch (Exception e) {
                e.printStackTrace();
                Dialog.ErrorDialog("Upload error:\n" + e.getMessage());
            }
        }).start();
    }
}
