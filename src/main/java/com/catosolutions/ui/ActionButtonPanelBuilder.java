package com.catosolutions.ui;

import com.catosolutions.utils.Dialog;
import com.catosolutions.utils.DomainUitls;
import com.catosolutions.utils.FileManagerUtil;
import com.catosolutions.utils.Log;
import com.catosolutions.wordpress.WordPressInstaller;
import com.catosolutions.wordpress.WordPressRemover;
import com.catosolutions.cpanel.CPanelFileUpload;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ActionButtonPanelBuilder {

    public static void build(JPanel panel, JTextField urlField, JTextField userField, JPasswordField passField,
                             JCheckBox aioPlugin, JCheckBox ultimatePlugin,
                             JTextField ultimateDir, JTextField backupDirField, JCheckBox quitCheckbox) {

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        JPanel backupBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        backupBtnPanel.add(new JLabel("Backup :"));
        JButton uploadButton = new JButton("Upload");
        JButton restoreButton = new JButton("Restore");
        backupBtnPanel.add(uploadButton);
        backupBtnPanel.add(restoreButton);
        buttonPanel.add(backupBtnPanel);

        JPanel actionBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actionBtnPanel.add(new JLabel("Action   :"));
        JButton submitBtn = new JButton("Install");
        JButton killButton = new JButton("Stop");
        JButton removeButton = new JButton("Remove");
        JButton logButton = new JButton("Log");
        actionBtnPanel.add(submitBtn);
        actionBtnPanel.add(killButton);
        actionBtnPanel.add(removeButton);
        actionBtnPanel.add(logButton);
        actionBtnPanel.add(quitCheckbox);
        buttonPanel.add(actionBtnPanel);

        panel.add(Box.createVerticalStrut(10));
        panel.add(buttonPanel);

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
            if (TabManager.hasDuplicateDirectories()) return;

            Ui.shouldStop = false;
            Log.redirectOutputTo();

            String url = urlField.getText().trim();
            String username = userField.getText().trim();
            String password = new String(passField.getPassword()).trim();
            boolean installAIO = aioPlugin.isSelected();
            boolean installUltimate = ultimatePlugin.isSelected();
            String ultimatePath = ultimateDir.getText().trim();
            String backupDir = backupDirField.getText().trim();
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
            if (!validateBackupDirectory(backupDir)) return;

            boolean isMm = false;
            List<JCheckBox> mmCheckboxes = TabManager.getMmCheckboxes();
            if (activeTabIndex < mmCheckboxes.size()) {
                isMm = mmCheckboxes.get(activeTabIndex).isSelected();
            }

            List<String> domains = DomainUitls.splitDomainName(isMm, directory);
            if (hasDuplicateBaseDomains(domains)) return;
            FileManagerUtil.saveDataToFile(url, username, password, installAIO, installUltimate, ultimatePath, backupDir, quitAfter);

            automationThread[0] = new Thread(() -> WordPressInstaller.runInstallAutomation(url, username, password, domains, installAIO, installUltimate, ultimatePath, quitAfter));
            automationThread[0].start();
        });

        removeButton.addActionListener(e -> {
            if (TabManager.hasDuplicateDirectories()) return;

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
            String backupDir = backupDirField.getText().trim();
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

            if (!validateBackupDirectory(backupDir)) return;

            boolean isMm = false;
            List<JCheckBox> mmCheckboxes = TabManager.getMmCheckboxes();
            if (activeTabIndex < mmCheckboxes.size()) {
                isMm = mmCheckboxes.get(activeTabIndex).isSelected();
            }

            List<String> domains = DomainUitls.splitDomainName(isMm, directory);
            if (hasDuplicateBaseDomains(domains)) return;
            FileManagerUtil.saveDataToFile(url, username, password, installAIO, installUltimate, ultimatePath, backupDir, quitAfter);

            automationThread[0] = new Thread(() ->
                    WordPressRemover.runRemoveAutomation(url, username, password, domains, quitAfter));
            automationThread[0].start();
        });

        uploadButton.addActionListener(e -> {
            Ui.shouldStop = false;
            Log.redirectOutputTo();

            String url = urlField.getText().trim();
            String username = userField.getText().trim();
            String password = new String(passField.getPassword()).trim();
            String backupDir = backupDirField.getText().trim();

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
            List<String> originalDomains = parseOriginalDomains(directory);

            if (directory == null) return;

            if (url.isEmpty() || username.isEmpty() || password.isEmpty() || directory.isEmpty()) {
                Dialog.ErrorDialog("Please fill in all required fields.");
                return;
            }

            if (!validateBackupDirectory(backupDir)) return;

            boolean isMm = false;
            List<JCheckBox> mmCheckboxes = TabManager.getMmCheckboxes();
            if (activeTabIndex < mmCheckboxes.size()) {
                isMm = mmCheckboxes.get(activeTabIndex).isSelected();
            }

            List<String> domains = DomainUitls.splitDomainName(isMm, directory);
            if (hasDuplicateBaseDomains(domains)) return;

            CPanelFileUpload.uploadBackupFiles(url, username, password, originalDomains, domains, backupDir);
        });
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

    public static boolean validateBackupDirectory(String path) {
        if (path == null || path.trim().isEmpty()) return true;

        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            Dialog.ErrorDialog("The specified backup directory does not exist or is not a valid folder.");
            return false;
        }

        return true;
    }

    private static boolean hasDuplicateBaseDomains(List<String> domains) {
        List<String> baseNames = new ArrayList<>();
        for (String domain : domains) {
            String base = domain.trim();
            if (base.isEmpty()) continue;

            int dotIndex = base.indexOf('.');
            if (dotIndex != -1) {
                base = base.substring(0, dotIndex); // e.g. from xxx.sg -> xxx
            }

            if (baseNames.contains(base)) {
                Dialog.ErrorDialog("Duplicate base domain name found: " + base);
                return true;
            }
            baseNames.add(base);
        }
        return false;
    }

    private static List<String> parseOriginalDomains(String directory) {
        List<String> originalDomains = new ArrayList<>();
        if (directory == null || directory.trim().isEmpty()) return originalDomains;

        for (String entry : directory.split(",")) {
            String cleaned = entry.trim();
            if (!cleaned.isEmpty()) {
                originalDomains.add(cleaned);
            }
        }
        return originalDomains;
    }
}