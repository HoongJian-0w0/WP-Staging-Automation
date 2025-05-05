package com.catosolutions.wordpress;

import com.catosolutions.chromedriver.ChromeDriver;
import com.catosolutions.cpanel.CPanelLogin;
import com.catosolutions.ui.Ui;
import com.catosolutions.utils.Dialog;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WordPressInstaller {

    static List<String> skippedDirs = new ArrayList<>();

    public static void runInstallAutomation(String url, String username, String password, List<String> domains,
                                            boolean installAIO, boolean installAIOU, String pathAIOU,
                                            boolean quitAfterFinish) {

        skippedDirs.clear();

        System.out.println("[START] 🛠️ WordPress Install Automation Running...");

        WebDriver driver = ChromeDriver.getDriver();
        WebDriverWait wait = ChromeDriver.getWait();

        if (driver == null) {
            System.out.println("[ERR] ❌ ChromeDriver not available. Exiting.");
            return;
        }

        try {
            if (ChromeDriver.checkIfKilled()) return;

            String loginResult = CPanelLogin.ensureValidLogin(url, username, password);
            if (loginResult.isEmpty()) return;

            driver = ChromeDriver.getDriver();
            wait = ChromeDriver.getWait();

            String token = CPanelLogin.getSessionToken();
            if (token.isEmpty()) {
                System.out.println("[ERR] ❌ Token not found after login.");
                return;
            }

            for (String directory : domains) {
                if (ChromeDriver.checkIfKilled()) return;
                System.out.println("\n[DOMAIN] 🌐 Installing to: " + directory);

                try {
                    String installUrl = url + token + "/frontend/jupiter/softaculous/index.live.php?act=software&soft=26&tab=install";
                    System.out.println("[ACTION] ➡️ Opening WordPress installer page...");

                    driver.get(installUrl);

                    WebElement dirField = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("softdirectory")));
                    dirField.clear();
                    dirField.sendKeys(directory);
                    System.out.println("[DONE] 📂 Directory set: " + directory);

                    wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//*[contains(text(), 'Select Plugins')]")));

                    String[] pluginIds = {"softaculous-pro", "backuply", "speedycache", "siteseo", "loginizer", "pagelayer", "gosmtp", "fileorganizer"};
                    for (String id : pluginIds) {
                        if (ChromeDriver.checkIfKilled()) return;
                        try {
                            WebElement input = driver.findElement(By.id(id));
                            if (input.isSelected()) {
                                WebElement label = input.findElement(By.xpath("./ancestor::label[1]"));
                                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", label);
                                label.click();
                            }
                        } catch (Exception e) {
                            System.out.println("[WARN] ⚠️ Plugin not found or already unchecked: " + id);
                        }
                    }
                    System.out.println("[DONE] ✅ Default plugins unchecked.");

                    WebElement installBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("softsubmitbut")));
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", installBtn);
                    installBtn.click();
                    System.out.println("[ACTION] 🚀 Installing WordPress for: " + directory);

                    Thread.sleep(3000);

                    List<WebElement> errorElems = driver.findElements(By.cssSelector("#error_handler li"));
                    boolean isDuplicateDirError = errorElems.stream().anyMatch(
                            el -> el.getText().contains("The directory you typed already exists")
                    );
                    if (isDuplicateDirError) {
                        System.out.println("[SKIP] ⚠️ Directory already exists, skipping installation for: " + directory);
                        skippedDirs.add(directory);
                        continue;
                    }

                    try {
                        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                                By.cssSelector("#install_win h5"),
                                "Congratulations, the software was installed successfully"
                        ));
                        System.out.println("[DONE] 🎉 Installation successful!");

                        WebElement adminLink = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("adminurl")));
                        String adminUrl = adminLink.getAttribute("href");
                        driver.get(adminUrl);
                        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("wpadminbar")));
                        System.out.println("[DONE] ✅ Admin panel loaded for: " + directory);

                        // Optional: Install All-in-One plugin
                        if (installAIO) {
                            if (ChromeDriver.checkIfKilled()) return;
                            try {
                                System.out.println("[PLUGIN] 🔍 Installing 'All-in-One WP Migration and Backup'...");
                                driver.get(driver.getCurrentUrl().split("/wp-admin")[0] + "/wp-admin/plugin-install.php");
                                WebElement searchBox = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("search-plugins")));
                                searchBox.clear();
                                searchBox.sendKeys("All-in-One WP Migration and Backup");
                                searchBox.sendKeys(Keys.RETURN);
                                Thread.sleep(2500);

                                WebElement pluginTitleLink = wait.until(ExpectedConditions.presenceOfElementLocated(
                                        By.xpath("//div[contains(@class,'plugin-card')]//h3/a[normalize-space(text())='All-in-One WP Migration and Backup']")
                                ));

                                WebElement pluginCard = pluginTitleLink.findElement(By.xpath("ancestor::div[contains(@class,'plugin-card')]"));
                                WebElement pluginInstallBtn = pluginCard.findElement(By.cssSelector("a.install-now"));
                                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", pluginInstallBtn);
                                pluginInstallBtn.click();

                                WebElement activateBtn = wait.until(ExpectedConditions.elementToBeClickable(
                                        By.xpath("//a[contains(text(),'Activate')]")));
                                activateBtn.click();
                                System.out.println("[DONE] ✅ AIO plugin activated.");
                            } catch (Exception pluginEx) {
                                System.out.println("[ERR] ❌ Failed to install/activate AIO plugin.");
                                pluginEx.printStackTrace();
                            }
                        }

                        // Optional: Upload Unlimited extension
                        if (installAIOU) {
                            if (ChromeDriver.checkIfKilled()) return;
                            try {
                                System.out.println("[PLUGIN] ⬆️ Uploading Unlimited Extension...");
                                driver.get(driver.getCurrentUrl().split("/wp-admin")[0] + "/wp-admin/plugin-install.php?tab=upload");
                                WebElement uploadInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("pluginzip")));
                                uploadInput.sendKeys(new File(pathAIOU).getAbsolutePath());
                                driver.findElement(By.id("install-plugin-submit")).click();

                                WebElement activateBtn = wait.until(ExpectedConditions.elementToBeClickable(
                                        By.xpath("//a[contains(text(),'Activate Plugin')]")));
                                activateBtn.click();
                                System.out.println("[DONE] ✅ Unlimited Extension plugin uploaded and activated.");
                            } catch (Exception uploadEx) {
                                System.out.println("[ERR] ❌ Failed to upload/activate Unlimited Extension.");
                                uploadEx.printStackTrace();
                            }
                        }

                    } catch (TimeoutException te) {
                        System.out.println("[ERR] ❌ Installation timeout for: " + directory);
                    }

                } catch (Exception ex) {
                    System.out.println("[ERR] ❌ Installation failed for directory: " + directory);
                    ex.printStackTrace();
                }
            }

        } catch (Exception ex) {
            System.out.println("[ERR] ❌ General error during automation:");
            ex.printStackTrace();
            Dialog.ErrorDialog("An error occurred:\n" + ex.getMessage());
        } finally {
            if (!skippedDirs.isEmpty()) {
                String skipped = String.join("\n• ", skippedDirs);
                Dialog.AlertDialog("⚠️ The following directories were skipped due to existing installations:\n• " + skipped);
            }

            if (Ui.shouldStop) {
                System.out.println("[STOP] ⛔ Operation manually stopped.");
                Dialog.AlertDialog("⛔ Automation was terminated by user.");
                ChromeDriver.quitDriver();
            } else {
                System.out.println("[DONE] ✅ All installations completed.");
                Dialog.SuccessDialog("✅ Installation Complete");

                if (quitAfterFinish) {
                    System.out.println("[EXIT] 🔐 Closing browser...");
                    ChromeDriver.quitDriver();
                } else {
                    System.out.println("[INFO] 🌟 Keeping ChromeDriver alive after install.");
                }
            }
        }
    }
}
