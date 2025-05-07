package com.catosolutions.chromedriver;

import com.catosolutions.cpanel.CPanelLogin;
import com.catosolutions.ui.Ui;
import com.catosolutions.utils.Dialog;
import com.catosolutions.utils.DomainUitls;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Set;

public class ChromeDriver {

    private static WebDriver driver;
    private static WebDriver uploadDriver;
    private static WebDriver restoreDriver;

    public static WebDriver getDriver() {
        if (driver == null || isDriverInvalid()) {
            System.out.println("[INFO] ♻️ Driver missing or dead. Recreating...");
            quitDriver();
            driver = create();
        }
        return driver;
    }

    public static WebDriverWait getWait() {
        return new WebDriverWait(getDriver(), Duration.ofSeconds(180));
    }

    public static WebDriver create() {
        try {
            System.out.println("[ACTION] 🚀 Initializing ChromeDriver...");
            WebDriver newDriver = new org.openqa.selenium.chrome.ChromeDriver();
            System.out.println("[DONE] ✅ ChromeDriver launched.");
            return newDriver;
        } catch (Exception e) {
            System.out.println("[ERR] ❌ Failed to initialize ChromeDriver: " + e.getMessage());
            Dialog.ErrorDialog("❌ Failed to initialize ChromeDriver:\n" + e.getMessage());
            return null;
        }
    }

    public static WebDriver createNewUploadDriver() {
        try {
            System.out.println("[UPLOAD] 🚀 Launching separate ChromeDriver for upload...");
            WebDriver newDriver = new org.openqa.selenium.chrome.ChromeDriver();
            System.out.println("[UPLOAD] ✅ Independent ChromeDriver launched.");
            return newDriver;
        } catch (Exception e) {
            System.out.println("[UPLOAD-ERR] ❌ Failed to initialize upload driver: " + e.getMessage());
            Dialog.ErrorDialog("❌ Upload browser failed:\n" + e.getMessage());
            return null;
        }
    }

    public static WebDriver createNewRestoreDriver() {
        try {
            System.out.println("[RESTORE] 🚀 Launching separate ChromeDriver for restore...");
            WebDriver newDriver = new org.openqa.selenium.chrome.ChromeDriver();
            System.out.println("[RESTORE] ✅ Independent ChromeDriver launched.");
            return newDriver;
        } catch (Exception e) {
            System.out.println("[RESTORE-ERR] ❌ Failed to initialize restore driver: " + e.getMessage());
            Dialog.ErrorDialog("❌ Restore browser failed:\n" + e.getMessage());
            return null;
        }
    }

    public static void quitDriver() {
        if (driver != null) {
            try {
                System.out.println("[EXIT] 🛑 Quitting ChromeDriver...");
                driver.quit();
                System.out.println("[DONE] ✅ ChromeDriver closed.");
            } catch (Exception e) {
                System.out.println("[WARN] ⚠️ Failed to quit ChromeDriver cleanly: " + e.getMessage());
            } finally {
                driver = null;
                CPanelLogin.resetSession();
                Ui.shouldStop = false;
                System.out.println("[INFO] 🔁 CPanel session reset and KillSwitch reset after browser exit.");
            }
        } else {
            System.out.println("[INFO] 🚫 No ChromeDriver session found to quit.");
        }
    }

    public static void quitUploadDriver() {
        if (uploadDriver != null) {
            try {
                System.out.println("[UPLOAD] 🛑 Quitting upload ChromeDriver...");
                uploadDriver.quit();
                System.out.println("[UPLOAD] ✅ Upload driver closed.");
            } catch (Exception e) {
                System.out.println("[UPLOAD-WARN] ⚠️ Failed to quit upload driver: " + e.getMessage());
            } finally {
                uploadDriver = null;
                Ui.uploadShouldStop = false;
            }
        }
    }

    public static void quitRestoreDriver() {
        if (restoreDriver != null) {
            try {
                System.out.println("[RESTORE] 🛑 Quitting restore ChromeDriver...");
                restoreDriver.quit();
                System.out.println("[RESTORE] ✅ Restore driver closed.");
            } catch (Exception e) {
                System.out.println("[RESTORE-WARN] ⚠️ Failed to quit restore driver: " + e.getMessage());
            } finally {
                restoreDriver = null;
            }
        }
    }

    public static boolean isDriverInvalid() {
        try {
            if (driver == null) return true;
            driver.getTitle();
            return false;
        } catch (Exception e) {
            System.out.println("[WARN] 🚨 Detected invalid WebDriver session: " + e.getMessage());
            return true;
        }
    }

    public static boolean isUploadDriverInvalid() {
        try {
            if (uploadDriver == null) return true;
            uploadDriver.getTitle();
            return false;
        } catch (Exception e) {
            System.out.println("[UPLOAD-WARN] 🚨 Invalid upload WebDriver session: " + e.getMessage());
            return true;
        }
    }

    public static boolean isRestoreDriverInvalid() {
        try {
            if (restoreDriver == null) return true;
            restoreDriver.getTitle();
            return false;
        } catch (Exception e) {
            System.out.println("[RESTORE-WARN] 🚨 Invalid restore WebDriver session: " + e.getMessage());
            return true;
        }
    }

    public static WebDriver getUploadDriver() {
        if (uploadDriver == null || isUploadDriverInvalid()) {
            System.out.println("[UPLOAD] ♻️ Upload driver missing or dead. Recreating...");
            quitUploadDriver();
            uploadDriver = createNewUploadDriver();
        }
        return uploadDriver;
    }

    public static WebDriver getRestoreDriver() {
        if (restoreDriver == null || isRestoreDriverInvalid()) {
            System.out.println("[RESTORE] ♻️ Restore driver missing or dead. Recreating...");
            quitRestoreDriver();
            restoreDriver = createNewRestoreDriver();
        }
        return restoreDriver;
    }

    public static boolean checkIfKilled() {
        if (Ui.shouldStop) {
            System.out.println("[STOP] ⛔ Automation manually killed by user.");
            quitDriver();
            return true;
        }
        return false;
    }

    public static void syncCookiesToUploadDriver(String baseUrl, String username, String password) {
        syncCookiesToTargetDriver(getUploadDriver(), baseUrl, username, password, "UPLOAD");
    }

    public static void syncCookiesToRestoreDriver(String baseUrl, String username, String password) {
        syncCookiesToTargetDriver(getRestoreDriver(), baseUrl, username, password, "RESTORE");
    }

    private static void syncCookiesToTargetDriver(WebDriver target, String baseUrl, String username, String password, String label) {
        try {
            WebDriver main = getDriver();
            if (main == null || target == null) {
                System.out.println("[" + label + "] ❌ One of the drivers is null. Cannot sync cookies.");
                return;
            }

            if (!isMainSessionStillValid(baseUrl)) {
                System.out.println("[" + label + "] ❌ Main session invalid. Attempting re-login...");
                String newToken = CPanelLogin.login(baseUrl, username, password);
                if (newToken.isEmpty()) {
                    System.out.println("[" + label + "-FAIL] ❌ Re-login failed. Cannot sync cookies.");
                    return;
                }
                main = getDriver();
            }

            target.get(DomainUitls.normalizeCpanelUrl(baseUrl));
            Set<Cookie> cookies = main.manage().getCookies();
            for (Cookie cookie : cookies) {
                target.manage().addCookie(cookie);
            }

            System.out.println("[" + label + "] 🍪 Cookies synced from main driver to " + label.toLowerCase() + " driver.");
        } catch (Exception e) {
            System.out.println("[" + label + "-WARN] ⚠️ Failed to sync cookies: " + e.getMessage());
        }
    }

    public static boolean isMainSessionStillValid(String baseUrl) {
        try {
            WebDriver driver = getDriver();
            if (driver == null) return false;

            String token = CPanelLogin.getSessionToken();
            if (token.isEmpty()) {
                System.out.println("[CHECK] ❌ No session token found.");
                return false;
            }

            String testUrl = DomainUitls.normalizeCpanelUrl(baseUrl) + token + "/frontend/jupiter/index.html";
            System.out.println("[CHECK] 🔍 Checking session validity via: " + testUrl);

            driver.get(testUrl);
            Thread.sleep(1000);

            boolean loginFormVisible = !driver.findElements(By.name("user")).isEmpty();
            if (loginFormVisible) {
                System.out.println("[CHECK] ❌ Main session invalid (login form visible).");
                return false;
            }

            System.out.println("[CHECK] ✅ Main session is still valid.");
            return true;

        } catch (Exception e) {
            System.out.println("[CHECK-ERR] ⚠️ Error during session validation: " + e.getMessage());
            return false;
        }
    }

}
