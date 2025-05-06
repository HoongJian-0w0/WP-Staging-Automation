package com.catosolutions.ui;

import com.catosolutions.utils.Dialog;
import com.catosolutions.utils.DomainUitls;
import com.catosolutions.utils.FileManagerUtil;
import com.catosolutions.utils.Log;
import com.catosolutions.wordpress.WordPressInstaller;
import com.catosolutions.wordpress.WordPressRemover;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

public class ActionButtonPanelBuilder {

    public static void build(JPanel panel, JTextField urlField, JTextField userField, JPasswordField passField,
                             JTextArea dirField, JCheckBox aioPlugin, JCheckBox ultimatePlugin,
                             JTextField ultimateDir, JCheckBox quitCheckbox) {

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton submitBtn = new JButton("Install");
        JButton killButton = new JButton("Stop");
        JButton removeButton = new JButton("Remove");
        JButton logButton = new JButton("Log");

        Thread[] automationThread = new Thread[1];

        killButton.addActionListener(k -> {
            if (automationThread[0] != null && automationThread[0].isAlive()) {
                boolean confirm = Dialog.ConfirmationDialog("Are you sure you want to terminate the running automation?");
                if (confirm) {
                    Ui.shouldStop = true;
                    Dialog.AlertDialog("⚠️ Kill signal sent. The automation will stop shortly.");
                }
            } else {
                Dialog.AlertDialog("ℹ️ No process running.");
            }
        });

        logButton.addActionListener(e -> LogWindowManager.open());

        submitBtn.addActionListener(e -> {
            Ui.shouldStop = false;
            Log.redirectOutputTo();

            String url = urlField.getText().trim();
            String username = userField.getText().trim();
            String password = new String(passField.getPassword()).trim();
            boolean installAIO = aioPlugin.isSelected();
            boolean installUltimate = ultimatePlugin.isSelected();
            String ultimatePath = ultimateDir.getText().trim();
            boolean quitAfter = quitCheckbox.isSelected();

            int activeTabIndex = TabManager.getTabbedPane().getSelectedIndex();

            List<String> dirs = TabManager.getCheckedDirectoriesFromActiveTab();
            StringBuilder result = new StringBuilder();
            for (String dir : dirs) {
                result.append(dir).append(", ");
            }
            if (result.length() >= 2) {
                result.setLength(result.length() - 2);
            }
            String directory = String.valueOf(result);

            if (directory == null) return;

            if (url.isEmpty() || username.isEmpty() || password.isEmpty() || directory.isEmpty()) {
                Dialog.ErrorDialog("Please fill in all required fields.");
                return;
            }

            if (installUltimate && (ultimatePath.isEmpty() || !ultimatePath.toLowerCase().endsWith(".zip"))) {
                Dialog.ErrorDialog("Please select a valid .zip file for the Ultimate Plugin.");
                return;
            }

            if (!validateUltimatePluginDirectory(ultimateDir.getText())) return;

            boolean isMm = false;
            List<JCheckBox> mmCheckboxes = TabManager.getMmCheckboxes();
            if (activeTabIndex < mmCheckboxes.size()) {
                isMm = mmCheckboxes.get(activeTabIndex).isSelected();
            }

            List<String> domains = DomainUitls.splitDomainName(isMm, directory);
            FileManagerUtil.saveDataToFile(url, username, password, installAIO, installUltimate, ultimatePath, quitAfter);

            automationThread[0] = new Thread(() -> WordPressInstaller.runInstallAutomation(url, username, password, domains, installAIO, installUltimate, ultimatePath, quitAfter));
            automationThread[0].start();
        });

        removeButton.addActionListener(e -> {
            boolean confirmRemove = Dialog.ConfirmationDialog("Are you sure you want to remove the selected installations?");
            if (!confirmRemove) return;

            Ui.shouldStop = false;
            Log.redirectOutputTo();

            String url = urlField.getText().trim();
            String username = userField.getText().trim();
            String password = new String(passField.getPassword()).trim();
            boolean installAIO = aioPlugin.isSelected();
            boolean installUltimate = ultimatePlugin.isSelected();
            String ultimatePath = ultimateDir.getText().trim();
            boolean quitAfter = quitCheckbox.isSelected();

            int activeTabIndex = TabManager.getTabbedPane().getSelectedIndex();

            List<String> dirs = TabManager.getCheckedDirectoriesFromActiveTab();
            StringBuilder result = new StringBuilder();
            for (String dir : dirs) {
                result.append(dir).append(", ");
            }
            if (result.length() >= 2) {
                result.setLength(result.length() - 2);
            }
            String directory = String.valueOf(result);

            if (directory == null) return;

            if (url.isEmpty() || username.isEmpty() || password.isEmpty() || directory.isEmpty()) {
                Dialog.ErrorDialog("Please fill in all required fields.");
                return;
            }

            boolean isMm = false;
            List<JCheckBox> mmCheckboxes = TabManager.getMmCheckboxes();
            if (activeTabIndex < mmCheckboxes.size()) {
                isMm = mmCheckboxes.get(activeTabIndex).isSelected();
            }

            List<String> domains = DomainUitls.splitDomainName(isMm, directory);
            FileManagerUtil.saveDataToFile(url, username, password, installAIO, installUltimate, ultimatePath, quitAfter);

            automationThread[0] = new Thread(() ->
                    WordPressRemover.runRemoveAutomation(url, username, password, domains, quitAfter));
            automationThread[0].start();
        });

        JPanel leftBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftBtnPanel.add(submitBtn);
        leftBtnPanel.add(killButton);
        leftBtnPanel.add(removeButton);
        leftBtnPanel.add(logButton);

        JPanel rightCheck = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightCheck.add(quitCheckbox);

        buttonPanel.add(leftBtnPanel, BorderLayout.WEST);
        buttonPanel.add(rightCheck, BorderLayout.EAST);

        panel.add(Box.createVerticalStrut(10));
        panel.add(buttonPanel);
    }

    private static boolean validateUltimatePluginDirectory(String path) {
        if (path == null || path.isEmpty()) return true;

        File file = new File(path);
        File dir = file.isDirectory() ? file : file.getParentFile();

        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            Dialog.ErrorDialog("The specified directory does not exist.");
            return false;
        }

        File[] zipFiles = dir.listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".zip"));
        if (zipFiles == null || zipFiles.length == 0) {
            Dialog.ErrorDialog("The directory must contain at least one .zip file.");
            return false;
        }

        return true;
    }
}
