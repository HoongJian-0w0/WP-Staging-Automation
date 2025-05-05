package com.catosolutions.utils;

import com.catosolutions.ui.TabManager;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.List;

public class FileManagerUtil {
    public static JPanel createLabeledPanel(String label, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(label), BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    public static void loadDataFromFile(JTextField urlField, JTextField userField, JPasswordField passField,
                                        JCheckBox aioCheck, JCheckBox aiouCheck, JTextField aiouPathField,
                                        JCheckBox quitAfterFinishCheckbox) {
        File dataFile = new File("data.txt");
        if (!dataFile.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(dataFile))) {
            String line;
            int lastFilledTabIndex = -1;

            while ((line = reader.readLine()) != null) {
                try {
                    line = CryptoUtil.decrypt(line);

                    if (line.startsWith("cpanel_url=")) urlField.setText(line.replace("cpanel_url=", "").trim());
                    else if (line.startsWith("username=")) userField.setText(line.replace("username=", "").trim());
                    else if (line.startsWith("password=")) passField.setText(line.replace("password=", "").trim());
                    else if (line.startsWith("tab")) {
                        int tabIndex = Integer.parseInt(line.substring(3, line.indexOf("=")).replace("[mm]", "")) - 1;
                        boolean isMM = line.contains("[mm]");
                        String content = line.substring(line.indexOf("=") + 1).trim().replace(",", "\n");

                        while (TabManager.getAllTextAreas().size() <= tabIndex) {
                            TabManager.createAndAddTab("");
                        }

                        TabManager.getAllTextAreas().get(tabIndex).setText(content);

                        if (isMM && tabIndex < TabManager.getAllMMCheckboxes().size()) {
                            TabManager.getAllMMCheckboxes().get(tabIndex).setSelected(true);
                        }

                        if (!content.isBlank()) lastFilledTabIndex = tabIndex;
                    } else if (line.startsWith("installAIO=")) aioCheck.setSelected(Boolean.parseBoolean(line.replace("installAIO=", "").trim()));
                    else if (line.startsWith("installAIOU=")) aiouCheck.setSelected(Boolean.parseBoolean(line.replace("installAIOU=", "").trim()));
                    else if (line.startsWith("ultimatePluginDir=")) aiouPathField.setText(line.replace("ultimatePluginDir=", "").trim());
                    else if (line.startsWith("quitAfterFinish=")) quitAfterFinishCheckbox.setSelected(Boolean.parseBoolean(line.replace("quitAfterFinish=", "").trim()));
                } catch (Exception e) {
                    // Skip invalid encrypted line
                }
            }

            if (lastFilledTabIndex >= 0) {
                JTabbedPane tabPane = TabManager.getTabPane();
                if (tabPane != null && lastFilledTabIndex < TabManager.getAllTextAreas().size()) {
                    tabPane.setSelectedIndex(lastFilledTabIndex);
                }
            }

        } catch (IOException e) {
            System.out.println("❌ Failed to load saved data: " + e.getMessage());
        }
    }

    public static void saveDataToFile(String url, String user, String pass,
                                      boolean installAIO, boolean installAIOU, String ultimateDir,
                                      boolean quitAfterFinish) {
        File outFile = new File("data.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFile))) {
            writer.write(CryptoUtil.encrypt("cpanel_url=" + url) + "\n");
            writer.write(CryptoUtil.encrypt("username=" + user) + "\n");
            writer.write(CryptoUtil.encrypt("password=" + pass) + "\n");

            List<JTextArea> tabDirFields = TabManager.getAllTextAreas();
            List<JCheckBox> mmChecks = TabManager.getAllMMCheckboxes();

            for (int i = 0; i < tabDirFields.size(); i++) {
                String content = tabDirFields.get(i).getText().replaceAll("\r?\n", ",");
                boolean isMM = i < mmChecks.size() && mmChecks.get(i).isSelected();
                String label = "tab" + (i + 1) + (isMM ? "[mm]" : "") + "=" + content;
                writer.write(CryptoUtil.encrypt(label) + "\n");
            }

            writer.write(CryptoUtil.encrypt("installAIO=" + installAIO) + "\n");
            writer.write(CryptoUtil.encrypt("installAIOU=" + installAIOU) + "\n");
            writer.write(CryptoUtil.encrypt("ultimatePluginDir=" + ultimateDir) + "\n");
            writer.write(CryptoUtil.encrypt("quitAfterFinish=" + quitAfterFinish) + "\n");

        } catch (Exception ex) {
            System.out.println("❌ Failed to save data: " + ex.getMessage());
        }
    }

    public static void saveOnlyTabDataToFile() {
        File dataFile = new File("data.txt");
        File tempFile = new File("data_temp.txt");

        try {
            try (BufferedReader reader = new BufferedReader(new FileReader(dataFile));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        String decrypted = CryptoUtil.decrypt(line);
                        if (!decrypted.startsWith("tab")) {
                            writer.write(line);
                            writer.newLine();
                        }
                    } catch (Exception ignored) {
                    }
                }

                List<JTextArea> tabDirFields = TabManager.getAllTextAreas();
                List<JCheckBox> mmChecks = TabManager.getAllMMCheckboxes();

                for (int i = 0; i < tabDirFields.size(); i++) {
                    String content = tabDirFields.get(i).getText().replaceAll("\r?\n", ",");
                    boolean isMM = i < mmChecks.size() && mmChecks.get(i).isSelected();
                    String label = "tab" + (i + 1) + (isMM ? "[mm]" : "") + "=" + content;
                    writer.write(CryptoUtil.encrypt(label));
                    writer.newLine();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            if (!dataFile.delete()) {
                System.out.println("⚠️ Warning: Couldn't delete original file. Trying overwrite.");
            }

            if (!tempFile.renameTo(dataFile)) {
                try (InputStream in = new FileInputStream(tempFile);
                     OutputStream out = new FileOutputStream(dataFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        out.write(buffer, 0, len);
                    }
                }

                tempFile.delete();
            }

            Dialog.SuccessDialog("Tab data saved successfully.");

        } catch (IOException e) {
            System.out.println("❌ Failed to save tab data: " + e.getMessage());
        }
    }
}
