package com.catosolutions.ui;

import com.catosolutions.chromedriver.ChromeDriver;
import com.catosolutions.cpanel.CPanelLogin;
import com.catosolutions.utils.Dialog;
import com.catosolutions.utils.DomainUitls;
import com.catosolutions.utils.Log;
import org.openqa.selenium.WebDriver;

import javax.swing.*;

public class CPanelLoginHandler {
    public static void handleLogin(JTextField urlField, JTextField userField, JPasswordField passField) {
        String url = urlField.getText().trim();
        String username = userField.getText().trim();
        String password = new String(passField.getPassword()).trim();

        if (url.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Dialog.ErrorDialog("Please enter URL, username, and password before logging in.");
            return;
        }

        String token = CPanelLogin.ensureValidLogin(url, username, password);
        if (!token.isEmpty()) {
            try {
                Log.redirectOutputTo();
                WebDriver driver = ChromeDriver.getDriver();
                String cpanelHomeUrl = DomainUitls.normalizeCpanelUrl(url) + token + "/frontend/jupiter/index.html";
                driver.get(cpanelHomeUrl);
                System.out.println("[ACTION] 🖥️ Opened cPanel dashboard: " + cpanelHomeUrl);
            } catch (Exception e) {
                System.out.println("[WARN] ⚠️ Failed to open cPanel dashboard: " + e.getMessage());
            }
        } else {
            Dialog.ErrorDialog("Login failed. Please check your credentials and try again.");
        }
    }
}
