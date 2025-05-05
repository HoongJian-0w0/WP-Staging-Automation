package com.catosolutions.cpanel;

import com.catosolutions.chromedriver.ChromeDriver;
import com.catosolutions.utils.Dialog;
import com.catosolutions.utils.DomainUitls;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class CPanelLogin {

    // ==== SESSION STATE ====
    private static String sessionToken = "";
    private static boolean isLoggedIn = false;

    // ==== PUBLIC METHODS ====

    /**
     * Main login function. Attempts to reuse token or perform full login.
     */
    public static String login(String url, String username, String password) {
        WebDriver driver = ChromeDriver.getDriver();

        if (driver == null || ChromeDriver.isDriverInvalid()) {
            System.out.println("[WARN] 🚨 ChromeDriver is null or dead during login. Recreating...");
            driver = ChromeDriver.getDriver(); // recreate it
            if (driver == null) {
                System.out.println("[ERR] ❌ ChromeDriver creation failed. Aborting login.");
                return "";
            }
        }

        if (!sessionToken.isEmpty() && isTokenStillValid(url, sessionToken)) {
            System.out.println("[INFO] 🔁 Reusing existing valid cPanel session.");
            isLoggedIn = true;
            return sessionToken;
        } else {
            System.out.println("[WARN] ⚠️ No valid session found. Re-authenticating...");
            resetSession();
        }

        try {
            if (stopIfKilled()) return "";

            String normalizedUrl = DomainUitls.normalizeCpanelUrl(url);
            System.out.println("[ACTION] 🌐 Navigating to cPanel login page: " + normalizedUrl);
            driver.get(normalizedUrl);

            ChromeDriver.getWait().until(ExpectedConditions.presenceOfElementLocated(By.name("user")));
            if (stopIfKilled()) return "";

            driver.findElement(By.name("user")).sendKeys(username);
            System.out.println("[DONE] ✅ Username entered.");
            driver.findElement(By.name("pass")).sendKeys(password);
            System.out.println("[DONE] ✅ Password entered.");
            driver.findElement(By.id("login_submit")).click();
            System.out.println("[ACTION] 🔐 Submitting login...");

            Thread.sleep(3000); // Let page redirect
            sessionToken = extractToken(driver.getCurrentUrl());

            if (sessionToken.isEmpty()) {
                throw new RuntimeException("❌ Unable to find session token in URL after login.");
            }

            isLoggedIn = true;
            System.out.println("[DONE] ✅ Login successful. Session token extracted: " + sessionToken);
            return sessionToken;

        } catch (TimeoutException e) {
            System.out.println("[WARN] ℹ️ Login page not detected. Assuming session still active.");
            sessionToken = extractToken(driver.getCurrentUrl());
            isLoggedIn = !sessionToken.isEmpty();
            return sessionToken;

        } catch (Exception e) {
            System.out.println("[ERR] ❌ Login failed: " + e.getMessage());
            Dialog.ErrorDialog("cPanel login failed:\n" + e.getMessage());
            return "";
        }
    }

    /**
     * Ensures session is valid or performs login.
     */
    public static String ensureValidLogin(String url, String username, String password) {
        if (ChromeDriver.isDriverInvalid()) {
            System.out.println("[WARN] 🛑 ChromeDriver session dead. Recreating...");
            ChromeDriver.getDriver();
        }

        if (!sessionToken.isEmpty() && isTokenStillValid(url, sessionToken)) {
            System.out.println("[INFO] 🔁 Existing session still valid. Using token.");
            return sessionToken;
        }

        System.out.println("[ACTION] 🔄 Session invalid. Logging in again...");
        return login(url, username, password);
    }

    /**
     * Returns the current session token (can be empty).
     */
    public static String getSessionToken() {
        return sessionToken;
    }

    /**
     * Clears the login state and token.
     */
    public static void resetSession() {
        System.out.println("[INFO] 🔁 Resetting cPanel session state.");
        isLoggedIn = false;
        sessionToken = "";
    }

    // ==== INTERNAL UTILITY METHODS ====

    /**
     * Checks if the session token is valid by visiting a protected page.
     */
    private static boolean isTokenStillValid(String baseUrl, String token) {
        try {
            WebDriver driver = ChromeDriver.getDriver();
            if (driver == null) {
                System.out.println("[WARN] 🚨 ChromeDriver was null. Recreating for token validation...");
                driver = ChromeDriver.getDriver();
            }

            if (driver == null) {
                System.out.println("[ERR] ❌ Unable to create ChromeDriver for token validation.");
                return false;
            }

            String testUrl = DomainUitls.normalizeCpanelUrl(baseUrl) + token + "/frontend/jupiter/index.html";
            System.out.println("[ACTION] 🔍 Checking if existing session token is valid...");
            driver.get(testUrl);
            Thread.sleep(1000);

            boolean loginFormVisible = !driver.findElements(By.name("user")).isEmpty();
            if (loginFormVisible) {
                System.out.println("[INFO] ❌ Session token invalid (login form detected).");
                return false;
            } else {
                System.out.println("[INFO] ✅ Session token still valid.");
                return true;
            }

        } catch (Exception e) {
            System.out.println("[WARN] ⚠️ Token validation error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Extracts cpsess token from a URL.
     */
    private static String extractToken(String url) {
        for (String part : url.split("/")) {
            if (part.startsWith("cpsess")) {
                return part;
            }
        }
        return "";
    }

    /**
     * Kills logic if the stop flag is raised.
     */
    private static boolean stopIfKilled() {
        return ChromeDriver.checkIfKilled();
    }
}
