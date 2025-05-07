package com.catosolutions.utils;

import com.catosolutions.Main;

import javax.swing.*;
import java.net.URL;

public class Dialog {

    private static final String DEFAULT_ERROR_TITLE = "Error";
    private static final String DEFAULT_SUCCESS_TITLE = "Success";
    private static final String DEFAULT_ALERT_TITLE = "Alert";

    private static final URL ERROR_ICON = Main.class.getResource("/image/Error.png");
    private static final URL SUCCESS_ICON = Main.class.getResource("/image/Success.png");
    private static final URL ALERT_ICON = Main.class.getResource("/image/Alert.png");

    /**
     * Prompt Error Dialog
     * @param message Message to show
     */
    public static void ErrorDialog(String message) {
        JOptionPane.showMessageDialog(null, message, DEFAULT_ERROR_TITLE, JOptionPane.ERROR_MESSAGE, loadIcon(ERROR_ICON));
    }

    /**
     * Prompt Error Dialog with custom title
     * @param title Dialog title
     * @param message Message to show
     */
    public static void ErrorDialog(String title, String message) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE, loadIcon(ERROR_ICON));
    }

    /**
     * Prompt Success Dialog
     * @param message Message to show
     */
    public static void SuccessDialog(String message) {
        JOptionPane.showMessageDialog(null, message, DEFAULT_SUCCESS_TITLE, JOptionPane.INFORMATION_MESSAGE, loadIcon(SUCCESS_ICON));
    }

    /**
     * Prompt Success Dialog with custom title
     * @param title Dialog title
     * @param message Message to show
     */
    public static void SuccessDialog(String title, String message) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE, loadIcon(SUCCESS_ICON));
    }

    /**
     * Prompt Confirmation Dialog (Yes/No)
     * @param message Message to ask
     * @return True if Yes, False if No
     */
    public static boolean ConfirmationDialog(String message) {
        int result = JOptionPane.showConfirmDialog(null, message, DEFAULT_ALERT_TITLE, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, loadIcon(ALERT_ICON));
        return result == JOptionPane.YES_OPTION;
    }

    /**
     * Prompt Confirmation Dialog with custom title
     * @param title Dialog title
     * @param message Message to ask
     * @return True if Yes, False if No
     */
    public static boolean ConfirmationDialog(String title, String message) {
        int result = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, loadIcon(ALERT_ICON));
        return result == JOptionPane.YES_OPTION;
    }

    /**
     * Prompt Alert Dialog
     * @param message Message to show
     */
    public static void AlertDialog(String message) {
        JOptionPane.showMessageDialog(null, message, DEFAULT_ALERT_TITLE, JOptionPane.WARNING_MESSAGE, loadIcon(ALERT_ICON));
    }

    /**
     * Helper method to load ImageIcon from URL safely
     * @param url Image URL
     * @return ImageIcon if URL is valid, null otherwise
     */
    public static ImageIcon loadIcon(URL url) {
        return (url != null) ? new ImageIcon(url) : null;
    }

    public static ImageIcon getAlertIcon() {
        return loadIcon(ALERT_ICON);
    }

}
