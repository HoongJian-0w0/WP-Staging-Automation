package com.catosolutions.utils;

import com.catosolutions.ui.TabManager;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileManagerUtil {
    private static File ensureDataDirectory() {
        String userHome = System.getProperty("user.home");
        File dir = new File(userHome, "Documents/CatoSolutions");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private static File getDataFile() {
        File dir = ensureDataDirectory();
        File dataFile = new File(dir, "data.txt");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                System.out.println("❌ Failed to create data.txt: " + e.getMessage());
            }
        }
        return dataFile;
    }

    private static File getTempDataFile() {
        File dir = ensureDataDirectory();
        File tempFile = new File(dir, "data_temp.txt");
        if (!tempFile.exists()) {
            try {
                tempFile.createNewFile();
            } catch (IOException e) {
                System.out.println("❌ Failed to create data_temp.txt: " + e.getMessage());
            }
        }
        return tempFile;
    }

    public static JPanel createLabeledPanel(String label, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(label), BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    public static void loadDataFromFile(JTextField urlField, JTextField userField, JPasswordField passField,
                                        JCheckBox aioCheck, JCheckBox aiouCheck, JTextField aiouPathField,
                                        JCheckBox quitAfterFinishCheckbox, JTextField backupField) {
        File dataFile = getDataFile();
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
                        int tabIndex = loadTabContent(line);
                        if (tabIndex > lastFilledTabIndex) lastFilledTabIndex = tabIndex;
                    } else if (line.startsWith("installAIO=")) aioCheck.setSelected(Boolean.parseBoolean(line.replace("installAIO=", "").trim()));
                    else if (line.startsWith("installAIOU=")) aiouCheck.setSelected(Boolean.parseBoolean(line.replace("installAIOU=", "").trim()));
                    else if (line.startsWith("ultimatePluginDir=")) aiouPathField.setText(line.replace("ultimatePluginDir=", "").trim());
                    else if (line.startsWith("backupDir=")) backupField.setText(line.replace("backupDir=", "").trim());
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
                                      String backupDir, boolean quitAfterFinish) {
        File outFile = getDataFile();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFile))) {
            writer.write(CryptoUtil.encrypt("cpanel_url=" + url) + "\n");
            writer.write(CryptoUtil.encrypt("username=" + user) + "\n");
            writer.write(CryptoUtil.encrypt("password=" + pass) + "\n");

            saveAllTabs(writer);

            writer.write(CryptoUtil.encrypt("installAIO=" + installAIO) + "\n");
            writer.write(CryptoUtil.encrypt("installAIOU=" + installAIOU) + "\n");
            writer.write(CryptoUtil.encrypt("ultimatePluginDir=" + ultimateDir) + "\n");
            writer.write(CryptoUtil.encrypt("backupDir=" + backupDir) + "\n");
            writer.write(CryptoUtil.encrypt("quitAfterFinish=" + quitAfterFinish) + "\n");

        } catch (Exception ex) {
            System.out.println("❌ Failed to save data: " + ex.getMessage());
        }
    }

    public static void saveOnlyTabDataToFile() {
        File dataFile = getDataFile();
        File tempFile = getTempDataFile();

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
                } catch (Exception ignored) {}
            }

            saveAllTabs(writer);

        } catch (IOException e) {
            System.out.println("❌ Failed to write new tab data: " + e.getMessage());
            return;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (!dataFile.delete()) {
            System.out.println("⚠️ Could not delete original data.txt. Attempting overwrite.");
        }

        if (!tempFile.renameTo(dataFile)) {
            try (InputStream in = new FileInputStream(tempFile);
                 OutputStream out = new FileOutputStream(dataFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
            } catch (IOException e) {
                System.out.println("❌ Backup copy to data.txt failed: " + e.getMessage());
                return;
            }
            tempFile.delete();
        }

        Dialog.SuccessDialog("✅ Tab data saved successfully.");
    }

    private static int loadTabContent(String decryptedLine) {
        int tabIndex = Integer.parseInt(decryptedLine.substring(3, decryptedLine.indexOf("=")).replace("[mm]", "")) - 1;
        boolean isMM = decryptedLine.contains("[mm]");
        String rawContent = decryptedLine.substring(decryptedLine.indexOf("=") + 1).trim();
        String[] lines = rawContent.split(",");

        StringBuilder cleanedText = new StringBuilder();
        List<Boolean> checkedStates = new ArrayList<>();

        for (String entry : lines) {
            if (entry.startsWith("[checked]")) {
                checkedStates.add(true);
                cleanedText.append(entry.replaceFirst("\\[checked\\]", "")).append("\n");
            } else {
                checkedStates.add(false);
                cleanedText.append(entry).append("\n");
            }
        }

        while (TabManager.getAllTextAreas().size() <= tabIndex) {
            TabManager.createAndAddTab("");
        }

        JTextArea area = TabManager.getAllTextAreas().get(tabIndex);
        area.setText(cleanedText.toString().trim());

        if (isMM && tabIndex < TabManager.getAllMMCheckboxes().size()) {
            TabManager.getAllMMCheckboxes().get(tabIndex).setSelected(true);
        }

        int finalTabIndex = tabIndex;
        SwingUtilities.invokeLater(() -> {
            JPanel tabContent = (JPanel) TabManager.getTabPane().getComponentAt(finalTabIndex);
            JScrollPane scrollPane = (JScrollPane) ((JPanel) ((JPanel) tabContent.getComponent(0)).getComponent(0)).getComponent(0);
            JPanel checkboxPanel = (JPanel) ((JPanel) scrollPane.getViewport().getView()).getComponent(0);

            for (int j = 0; j < checkedStates.size(); j++) {
                if (j < checkboxPanel.getComponentCount()) {
                    Component comp = checkboxPanel.getComponent(j);
                    if (comp instanceof JCheckBox cb) {
                        cb.setSelected(checkedStates.get(j));
                    }
                }
            }
        });

        return tabIndex;
    }

    private static void saveAllTabs(BufferedWriter writer) throws Exception {
        List<JTextArea> tabDirFields = TabManager.getAllTextAreas();
        List<JCheckBox> mmChecks = TabManager.getAllMMCheckboxes();

        for (int i = 0; i < tabDirFields.size(); i++) {
            JTextArea area = tabDirFields.get(i);
            boolean isMM = i < mmChecks.size() && mmChecks.get(i).isSelected();
            StringBuilder contentBuilder = new StringBuilder();

            JPanel tabContent = (JPanel) TabManager.getTabPane().getComponentAt(i);
            JScrollPane scrollPane = (JScrollPane) ((JPanel)((JPanel) tabContent.getComponent(0)).getComponent(0)).getComponent(0);
            JPanel checkboxPanel = (JPanel) ((JPanel) scrollPane.getViewport().getView()).getComponent(0);

            try {
                for (int j = 0; j < area.getLineCount(); j++) {
                    int start = area.getLineStartOffset(j);
                    int end = area.getLineEndOffset(j);
                    String line = area.getText(start, end - start).trim();
                    if (!line.isEmpty()) {
                        Component comp = checkboxPanel.getComponent(j);
                        boolean isChecked = comp instanceof JCheckBox cb && cb.isSelected();
                        contentBuilder.append(isChecked ? "[checked]" : "").append(line).append(",");
                    }
                }
            } catch (Exception ignored) {}

            String label = "tab" + (i + 1) + (isMM ? "[mm]" : "") + "=" + contentBuilder.toString().replaceAll(",$", "");
            writer.write(CryptoUtil.encrypt(label));
            writer.newLine();
        }
    }
}
