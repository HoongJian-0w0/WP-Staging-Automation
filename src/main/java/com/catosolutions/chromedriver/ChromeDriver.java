package com.catosolutions.chromedriver;

import com.catosolutions.cpanel.CPanelLogin;
import com.catosolutions.ui.Ui;
import com.catosolutions.utils.Dialog;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class ChromeDriver {

    private static WebDriver driver;

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
}

