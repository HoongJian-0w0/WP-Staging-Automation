package com.catosolutions.wordpress;

import com.catosolutions.chromedriver.ChromeDriver;
import com.catosolutions.cpanel.CPanelLogin;
import com.catosolutions.ui.Ui;
import com.catosolutions.utils.Dialog;
import com.catosolutions.utils.DomainUitls;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URL;
import java.time.Duration;
import java.util.*;

public class WordPressRestoreBackup {

    private static void terminateWithMessage(WebDriver driver, String message) {
        System.out.println("[ERR] ❌ " + message);
        Dialog.AlertDialog("🛑 Restore terminated: " + message);
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception ignored) {}
        }
    }

    public static void restoreUploadedBackups(String url, String username, String password, List<String> domains) {
        new Thread(() -> {
            Map<String, String> failedDomains = new HashMap<>();

            System.out.println("[INFO] 🔄 Restore process started...");

            if (Ui.restoreShouldStop) {
                terminateWithMessage(null, "Cancelled before start.");
                return;
            }

            System.out.println("[INFO] 🔐 Validating session login...");
            String sessionToken = CPanelLogin.ensureValidLogin(url, username, password);
            if (Ui.restoreShouldStop || sessionToken.isEmpty()) {
                terminateWithMessage(null, "Login failed or session could not be established.");
                return;
            }

            WebDriver driver = ChromeDriver.getRestoreDriver();
            if (Ui.restoreShouldStop || driver == null) {
                terminateWithMessage(driver, "Browser launch failed.");
                return;
            }

            String loginUrl = DomainUitls.normalizeCpanelUrl(url);
            System.out.println("[INFO] 🌐 Navigating to cPanel login URL...");
            driver.get(loginUrl);
            ChromeDriver.syncCookiesToRestoreDriver(url, username, password);
            driver.navigate().refresh();

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                System.out.println("[ERR] ⚠️ Sleep interrupted: " + e.getMessage());
            }

            String managerTabHandle = driver.getWindowHandle();

            for (String targetDir : domains) {
                if (Ui.restoreShouldStop) {
                    terminateWithMessage(driver, "Terminated during restore loop.");
                    return;
                }

                System.out.println("\n[INFO] 🔁 Processing: " + targetDir);
                try {
                    String wpManagerUrl = loginUrl + sessionToken + "/frontend/jupiter/softaculous/index.live.php?act=wordpress";
                    driver.switchTo().window(managerTabHandle);
                    driver.get(wpManagerUrl);

                    List<WebElement> forms = driver.findElements(By.cssSelector("form[id^='details_']"));
                    boolean matched = false;

                    for (WebElement form : forms) {
                        if (Ui.restoreShouldStop) {
                            terminateWithMessage(driver, "Terminated during form scan.");
                            return;
                        }

                        try {
                            String siteUrl = form.findElement(By.cssSelector("a.title.d-block")).getText().trim();
                            String installDir = new URL(siteUrl).getPath().replace("/", "").trim();
                            String siteName = form.findElement(By.cssSelector("div.col-12.col-md-2 > span.title")).getText().trim();

                            if (!installDir.equals(targetDir)) {
                                System.out.println("[WARM] ⏭️ Domain does not match target: " + siteUrl);
                                continue;
                            }

                            if (!siteName.equalsIgnoreCase("My Blog")) {
                                System.out.println("[WARM] ⏩ Already customized: " + siteUrl + " (Title: " + siteName + ")");
                                matched = true;
                                break;
                            }

                            Set<String> handlesBefore = new HashSet<>(driver.getWindowHandles());
                            WebElement loginBtn = form.findElement(By.cssSelector("input#login_button"));
                            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", loginBtn);
                            Thread.sleep(1000);
                            loginBtn.click();

                            String newTab = null;
                            for (int i = 0; i < 10; i++) {
                                Set<String> currentHandles = driver.getWindowHandles();
                                currentHandles.removeAll(handlesBefore);
                                if (!currentHandles.isEmpty()) {
                                    newTab = currentHandles.iterator().next();
                                    break;
                                }
                                Thread.sleep(500);
                            }

                            if (newTab == null) {
                                throw new RuntimeException("No new admin tab detected.");
                            }

                            driver.switchTo().window(newTab);
                            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("wpadminbar")));

                            boolean pluginInstalled = driver.findElements(By.id("toplevel_page_ai1wm_export")).size() > 0;
                            if (!pluginInstalled) {
                                System.out.println("[WARM] ⚠️ Plugin not installed for: " + targetDir);
                                failedDomains.put(targetDir, "Plugin not installed");
                                matched = true;
                                break;
                            }

                            String baseUrl = driver.getCurrentUrl().split("/wp-admin")[0];
                            String backupUrl = baseUrl + "/wp-admin/admin.php?page=ai1wm_backups";
                            driver.get(backupUrl);

                            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".ai1wm-backups")));
                            List<WebElement> archiveRows = driver.findElements(By.cssSelector(".ai1wm-backups tbody > tr"));

                            WebElement latestRow = null;
                            for (WebElement row : archiveRows) {
                                if (!row.getAttribute("class").contains("ai1wm-backups-list-spinner-holder")) {
                                    if (row.findElements(By.cssSelector(".ai1wm-backup-restore")).size() > 0) {
                                        latestRow = row;
                                        break;
                                    }
                                }
                            }

                            if (latestRow == null) {
                                System.out.println("[WARM] ⚠️ No backup found to restore.");
                                failedDomains.put(targetDir, "No backup file found to restore");
                                continue;
                            }

                            WebElement dotsButton = wait.until(ExpectedConditions.elementToBeClickable(
                                    latestRow.findElement(By.cssSelector(".ai1wm-backup-dots"))));
                            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", dotsButton);
                            Thread.sleep(500);
                            dotsButton.click();

                            wait.until(ExpectedConditions.visibilityOfElementLocated(
                                    By.cssSelector(".ai1wm-backup-dots-menu[style*='block']")));

                            WebElement restoreBtn = wait.until(ExpectedConditions.elementToBeClickable(
                                    latestRow.findElement(By.cssSelector(".ai1wm-backup-restore"))));
                            Thread.sleep(500);
                            try {
                                restoreBtn.click();
                            } catch (Exception clickFail) {
                                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", restoreBtn);
                                Thread.sleep(500);
                            }

                            wait.until(ExpectedConditions.presenceOfElementLocated(
                                    By.cssSelector(".ai1wm-modal-container p")));
                            wait.until(ExpectedConditions.presenceOfElementLocated(
                                    By.cssSelector(".ai1wm-import-modal-actions .ai1wm-button-green")));

                            WebElement proceedBtn = driver.findElement(By.cssSelector(".ai1wm-import-modal-actions .ai1wm-button-green"));
                            Thread.sleep(1000);
                            proceedBtn.click();

                            System.out.println("[INFO] ✅ Restore triggered successfully for: " + targetDir);
                            matched = true;

                            Thread.sleep(3000);
                            break;

                        } catch (Exception ex) {
                            String reason = "Form handling error: " + ex.getMessage();
                            System.out.println("[ERR] ❌ " + reason);
                            failedDomains.put(targetDir, reason);
                        }
                    }

                    if (!matched) {
                        String msg = "No matching WordPress installation found.";
                        System.out.println("[WARM] ⚠️ " + msg);
                        failedDomains.put(targetDir, msg);
                    }

                } catch (Exception e) {
                    String reason = "Restore failed: " + e.getMessage();
                    System.out.println("[ERR] ❌ " + reason);
                    failedDomains.put(targetDir, reason);
                }
            }

            if (!failedDomains.isEmpty()) {
                StringBuilder summary = new StringBuilder("[SUMMARY] 🧾 Skipped or Failed Domains:\n");
                failedDomains.forEach((domain, reason) -> {
                    summary.append(" - ").append(domain).append(": ").append(reason).append("\n");
                });
                System.out.println(summary);
                Dialog.ErrorDialog(summary.toString());
            }

            System.out.println("[INFO] 🎉 All restore operations attempted.");
            Dialog.SuccessDialog("All restore operations attempted.");

        }).start();
    }
}
