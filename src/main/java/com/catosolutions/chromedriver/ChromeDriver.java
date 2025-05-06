package com.catosolutions.chromedriver;

import com.catosolutions.cpanel.CPanelLogin;
import com.catosolutions.ui.Ui;
import com.catosolutions.utils.Dialog;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Set;

public class ChromeDriver {

    private static WebDriver driver;
    private static WebDriver uploadDriver;

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

    /**
     * Creates a completely independent ChromeDriver instance for upload or temp operations.
     * Does NOT affect the main driver or session.
     */
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

    public static boolean checkIfKilled() {
        if (Ui.shouldStop) {
            System.out.println("[STOP] ⛔ Automation manually killed by user.");
            quitDriver();
            return true;
        }
        return false;
    }

    public static WebDriver getUploadDriver() {
        if (uploadDriver == null || isUploadDriverInvalid()) {
            System.out.println("[UPLOAD] ♻️ Upload driver missing or dead. Recreating...");
            quitUploadDriver();
            uploadDriver = createNewUploadDriver();
        }
        return uploadDriver;
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
            }
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

    public static void syncCookiesToUploadDriver() {
        try {
            WebDriver main = getDriver();
            WebDriver upload = getUploadDriver();

            if (main == null || upload == null) return;

            Set<Cookie> cookies = main.manage().getCookies();
            for (Cookie cookie : cookies) {
                upload.manage().addCookie(cookie);
            }

            System.out.println("[UPLOAD] 🍪 Cookies synced from main driver to upload driver.");
        } catch (Exception e) {
            System.out.println("[UPLOAD-WARN] ⚠️ Failed to sync cookies: " + e.getMessage());
        }
    }

}
