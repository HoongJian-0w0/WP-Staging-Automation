package com.catosolutions.cpanel;

import com.catosolutions.chromedriver.ChromeDriver;
import com.catosolutions.utils.Dialog;
import com.catosolutions.utils.DomainUitls;
import com.catosolutions.utils.WordPressBackupFinder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class CPanelFileUploadBkp {

    public static void uploadBackupFiles(String url, String username, String password,
                                         List<String> originalDomains, List<String> domains, String backupDir) {
        new Thread(() -> {
            try {
                WebDriver uploadDriver = ChromeDriver.createNewUploadDriver();
                if (uploadDriver == null) {
                    Dialog.ErrorDialog("Failed to launch upload browser.");
                    return;
                }

                WebDriverWait wait = new WebDriverWait(uploadDriver, Duration.ofSeconds(30));

                // Navigate to login page
                String loginUrl = DomainUitls.normalizeCpanelUrl(url);
                uploadDriver.get(loginUrl);
                System.out.println("[UPLOAD] 🌐 Navigating to: " + loginUrl);

                // Login
                wait.until(ExpectedConditions.presenceOfElementLocated(By.name("user"))).sendKeys(username);
                uploadDriver.findElement(By.name("pass")).sendKeys(password);
                uploadDriver.findElement(By.id("login_submit")).click();
                System.out.println("[UPLOAD] 🔐 Login submitted...");

                Thread.sleep(3000); // Wait for redirect

                // Extract session token
                String currentUrl = uploadDriver.getCurrentUrl();
                String sessionToken = "";
                for (String part : currentUrl.split("/")) {
                    if (part.startsWith("cpsess")) {
                        sessionToken = part;
                        break;
                    }
                }

                if (sessionToken.isEmpty()) {
                    Dialog.ErrorDialog("❌ Upload login failed: session token not found.");
                    uploadDriver.quit();
                    return;
                }

                String fileManagerUrl = loginUrl + sessionToken + "/frontend/jupiter/filemanager/index.html";
                System.out.println("[UPLOAD] 📁 Opening File Manager at: " + fileManagerUrl);
                uploadDriver.get(fileManagerUrl);

                wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[text()='public_html']"))).click();
                System.out.println("[UPLOAD] ✅ public_html folder opened.");

                for (int i = 0; i < originalDomains.size(); i++) {
                    String originalDomain = originalDomains.get(i);
                    String mappedFolder = (i < domains.size()) ? domains.get(i) : null;

                    if (mappedFolder == null) {
                        System.out.println("[WARN] ⚠️ No matching domain directory for: " + originalDomain);
                        continue;
                    }

                    String backupPath = WordPressBackupFinder.findLatestBackupForDomain(backupDir, originalDomain);
                    if (backupPath == null) {
                        System.out.println("[WARN] ❌ No backup found for domain: " + originalDomain);
                        continue;
                    }

                    System.out.println("[MATCH] 📂 Domain: " + originalDomain + " → Folder: " + mappedFolder);
                    System.out.println("[MATCH] 📦 Backup file: " + backupPath);

                    wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[text()='" + mappedFolder + "']"))).click();
                    System.out.println("[NAV] 📂 Entered folder: " + mappedFolder);

                    wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[text()='wp-content']"))).click();
                    System.out.println("[NAV] 📂 Entered wp-content");

                    wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//span[text()='ai1wm-backups']"))).click();
                    System.out.println("[NAV] 📂 Entered ai1wm-backups");

                    uploadDriver.findElement(By.id("action-upload")).click();
                    System.out.println("[ACTION] ⬆️ Upload button clicked");

                    Thread.sleep(1000);

                    List<String> tabs = uploadDriver.getWindowHandles().stream().toList();
                    if (tabs.size() < 2) {
                        Dialog.ErrorDialog("Upload tab did not open properly.");
                        return;
                    }
                    uploadDriver.switchTo().window(tabs.get(1));
                    System.out.println("[TAB] 🔀 Switched to upload tab.");

                    WebDriverWait uploadWait = new WebDriverWait(uploadDriver, Duration.ofSeconds(30));
                    uploadWait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[type='file']")))
                            .sendKeys(backupPath);
                    System.out.println("[UPLOAD] ✅ Upload triggered: " + backupPath);

                    boolean uploaded = false;
                    long startTime = System.currentTimeMillis();
                    long timeout = Duration.ofMinutes(20).toMillis();

                    while (true) {
                        try {
                            WebElement progressBar = uploadDriver.findElement(By.id("progress1"));
                            String progressText = progressBar.getText().trim();
                            String progressStyle = progressBar.getAttribute("style");
                            String progressClass = progressBar.getAttribute("class");

                            boolean isSuccess = progressClass.contains("progress-bar-success");
                            boolean isFailed = progressClass.contains("progress-bar-danger");
                            boolean isFullWidth = progressStyle.contains("width: 100%");

                            WebElement cancelBtn = null;
                            try {
                                cancelBtn = uploadDriver.findElement(By.id("uploaderCancel1"));
                            } catch (Exception ignored) {}

                            boolean isCancelHidden = cancelBtn != null && cancelBtn.getAttribute("class").contains("hidden");

                            WebElement stats = null;
                            try {
                                stats = uploadDriver.findElement(By.id("uploaderstats1"));
                            } catch (Exception ignored) {}

                            String statusMessage = stats != null ? stats.getText().trim().toLowerCase() : "";
                            boolean isHttp500 = statusMessage.contains("http error 500");

                            if ((isSuccess || isFailed || isCancelHidden) && isFullWidth) {
                                if (isHttp500) {
                                    System.out.println("[WARN] ⚠️ Upload shows HTTP 500, but bar completed. Assuming success.");
                                } else if (isFailed) {
                                    System.out.println("[INFO] ❌ Upload bar shows red, upload might have failed.");
                                } else {
                                    System.out.println("[INFO] ✅ Upload finished (green bar or cancel hidden).");
                                }
                                uploaded = true;
                                break;
                            } else {
                                System.out.println("[WAIT] ⏳ Upload in progress... " + progressText + " | " + progressStyle);
                            }

                        } catch (org.openqa.selenium.NoSuchElementException e) {
                            System.out.println("[WAIT] ⏳ Waiting for upload progress bar to appear...");
                        }

                        if (System.currentTimeMillis() - startTime > timeout) {
                            System.out.println("[FAIL] ❌ Upload timed out: " + originalDomain);
                            break;
                        }

                        Thread.sleep(3000);
                    }
                    if (!uploaded) {
                        System.out.println("[FAIL] ❌ Upload did not complete: " + originalDomain);
                    }

                    uploadDriver.close();
                    uploadDriver.switchTo().window(tabs.get(0));
                    System.out.println("[TAB] 🔁 Switched back to File Manager tab.");
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                Dialog.ErrorDialog("Upload preparation failed:\n" + ex.getMessage());
            }
        }).start();
    }
}
