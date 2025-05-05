package com.catosolutions.wordpress;

import com.catosolutions.chromedriver.ChromeDriver;
import com.catosolutions.cpanel.CPanelLogin;
import com.catosolutions.ui.Ui;
import com.catosolutions.utils.Dialog;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URL;
import java.util.List;

public class WordPressRemover {

    public static void runRemoveAutomation(String baseUrl, String username, String password, List<String> directories, boolean quitAfterFinish) {
        System.out.println("[START] 🗑️ Starting WordPress Removal Automation");

        WebDriver driver = ChromeDriver.getDriver();
        WebDriverWait wait = ChromeDriver.getWait();
        int removedCount = 0;

        if (driver == null) {
            System.out.println("[ERR] ❌ WebDriver initialization failed. Aborting.");
            return;
        }

        try {
            if (ChromeDriver.checkIfKilled()) {
                System.out.println("[ERR] ⛔ Automation was terminated before login.");
                return;
            }

            System.out.println("[INFO] 🔐 Attempting cPanel login...");
            String token = CPanelLogin.login(baseUrl, username, password);
            if (token.isEmpty()) {
                System.out.println("[ERR] ❌ Login failed. No token received.");
                return;
            }
            System.out.println("[DONE] ✅ cPanel login successful.");

            for (String dir : directories) {
                System.out.println("\n[DIR] 📁 Processing directory: " + dir);
                String expectedPath = "/" + dir;
                boolean matchFound = false;

                String installListUrl = baseUrl + token + "/frontend/jupiter/softaculous/index.live.php?act=installations";
                System.out.println("[INFO] 🌐 Loading Installations List...");
                driver.get(installListUrl);

                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("tr.installations-list")));
                List<WebElement> rows = driver.findElements(By.cssSelector("tr.installations-list"));
                System.out.println("[INFO] 📄 Total installations found: " + rows.size());

                for (WebElement row : rows) {
                    if (ChromeDriver.checkIfKilled()) {
                        System.out.println("[ERR] ⛔ Automation was manually stopped during search.");
                        return;
                    }

                    try {
                        WebElement anchor = row.findElement(By.cssSelector("td.endurl a"));
                        String href = anchor.getAttribute("href");

                        if (href != null) {
                            String actualPath = new URL(href).getPath();
                            System.out.println("[CHECK] 🔎 Found install at path: " + actualPath);

                            if (actualPath.equals(expectedPath)) {
                                System.out.println("[MATCH] ✅ Match found for: " + href);

                                WebElement removeLink = row.findElement(By.cssSelector("a[href*='act=remove']"));
                                String removeHref = removeLink.getAttribute("href");

                                if (!removeHref.startsWith("http")) {
                                    removeHref = baseUrl + "/" + removeHref;
                                }

                                System.out.println("[ACTION] ➡️ Navigating to uninstall page: " + removeHref);
                                driver.get(removeHref);

                                wait.until(ExpectedConditions.elementToBeClickable(By.id("softsubmitbut")));
                                System.out.println("[ACTION] 🖱️ Clicking 'Remove Installation'...");
                                WebElement confirmBtn = driver.findElement(By.id("softsubmitbut"));
                                confirmBtn.click();

                                wait.until(ExpectedConditions.alertIsPresent());
                                Alert confirmAlert = driver.switchTo().alert();
                                System.out.println("[ALERT] ⚠️ Confirm dialog shown: " + confirmAlert.getText());
                                confirmAlert.accept();

                                wait.until(ExpectedConditions.visibilityOfElementLocated(
                                        By.cssSelector("#install_win .alert.alert-warning center")
                                ));

                                System.out.println("[DONE] 🧹 Installation '" + dir + "' removed successfully.");
                                removedCount++;
                                matchFound = true;
                                break;
                            }
                        }

                    } catch (NoSuchElementException e) {
                        System.out.println("[WARN] ⚠️ Incomplete row data. Skipping...");
                    } catch (Exception ex) {
                        System.out.println("[ERR] ❌ Unexpected error during row scan: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }

                if (!matchFound) {
                    System.out.println("[WARN] ⚠️ No matching installation found for: " + dir);
                }
            }

        } catch (Exception ex) {
            System.out.println("[ERR] ❌ Fatal error occurred:");
            ex.printStackTrace();
            Dialog.ErrorDialog("An error occurred:\n" + ex.getMessage());
        } finally {
            if (Ui.shouldStop) {
                System.out.println("[STOP] ⛔ User manually stopped the automation.");
                Dialog.AlertDialog("⛔ Automation was terminated by user.");
            } else {
                System.out.println("\n[SUMMARY] ✅ Total installations removed: " + removedCount);
                Dialog.SuccessDialog("✅ Process Complete. " + removedCount + " installations removed.");
            }

            if (quitAfterFinish || Ui.shouldStop) {
                System.out.println("[EXIT] 🚪 Closing browser...");
                ChromeDriver.quitDriver();
            }
        }
    }
}
