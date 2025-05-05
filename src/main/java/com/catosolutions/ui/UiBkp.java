package com.catosolutions.ui;

import com.catosolutions.Main;
import com.catosolutions.chromedriver.ChromeDriver;
import com.catosolutions.cpanel.CPanelLogin;
import com.catosolutions.utils.Dialog;
import com.catosolutions.utils.DomainUitls;
import com.catosolutions.utils.FileManagerUtil;
import com.catosolutions.utils.Log;
import com.catosolutions.wordpress.WordPressInstaller;
import com.catosolutions.wordpress.WordPressRemover;
import org.openqa.selenium.WebDriver;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class UiBkp {

    public static volatile boolean shouldStop;
    private static JFrame logWindowRef = null;
    private static JTextArea logArea = null;

    public static void loadUi() {
        JFrame frame = initMainFrame();
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JTextField urlField = new JTextField();
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JTextArea dirField = new JTextArea(20, 20);
        dirField.setPreferredSize(new Dimension(0, 60));
        dirField.setWrapStyleWord(true);
        JCheckBox mmPrefixCheckbox = new JCheckBox("MM");
        JCheckBox installAIOPluginCheckbox = new JCheckBox("Install 'All-in-One WP Migration and Backup'");
        JCheckBox installUltimateCheckbox = new JCheckBox("Install 'All-in-One WP Migration Unlimited Extension'");
        JTextField ultimatePluginDirField = new JTextField();
        JCheckBox quitAfterFinishCheckbox = new JCheckBox("Quit-Complete");

        FileManagerUtil.loadDataFromFile(urlField, userField, passField,
                installAIOPluginCheckbox, installUltimateCheckbox, ultimatePluginDirField,
                quitAfterFinishCheckbox);

        addFormFields(contentPanel, urlField, userField, passField, dirField, mmPrefixCheckbox);
        addPluginSection(contentPanel, installAIOPluginCheckbox, installUltimateCheckbox,
                ultimatePluginDirField, frame);
        addButtons(contentPanel, urlField, userField, passField, dirField, mmPrefixCheckbox,
                installAIOPluginCheckbox, installUltimateCheckbox,
                ultimatePluginDirField, quitAfterFinishCheckbox);

        frame.add(contentPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private static JFrame initMainFrame() {
        JFrame frame = new JFrame("WordPress Staging Setup");
        frame.setResizable(false);
        frame.setSize(500, 600);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        try {
            Image icon = new ImageIcon(Main.class.getResource("/image/wordpress-64x64.png")).getImage();
            frame.setIconImage(icon);
        } catch (Exception e) {
            System.out.println("⚠️ Icon not found or failed to load.");
        }
        return frame;
    }

    private static void addFormFields(JPanel panel, JTextField urlField, JTextField userField, JPasswordField passField,
                                      JTextArea dirField, JCheckBox mmPrefixCheckbox) {

        // URL + Login Button Panel
        JPanel urlPanel = new JPanel(new BorderLayout());
        urlPanel.add(new JLabel("Cpanel URL:"), BorderLayout.NORTH);
        urlPanel.add(urlField, BorderLayout.CENTER);

        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> handleCPanelLogin(urlField, userField, passField));
        urlPanel.add(loginButton, BorderLayout.EAST);
        panel.add(urlPanel);

        panel.add(Box.createVerticalStrut(15));
        panel.add(FileManagerUtil.createLabeledPanel("Username:", userField));
        panel.add(Box.createVerticalStrut(15));
        panel.add(FileManagerUtil.createLabeledPanel("Password:", passField));
        panel.add(Box.createVerticalStrut(15));

        JPanel dirPanel = new JPanel(new BorderLayout());
        dirPanel.add(new JLabel("Install Directory:"), BorderLayout.NORTH);
        dirPanel.add(new JScrollPane(dirField), BorderLayout.CENTER);
        dirPanel.add(mmPrefixCheckbox, BorderLayout.EAST);
        panel.add(dirPanel);

        JLabel installNote = new JLabel("(Automation for domain splitting applied)");
        installNote.setFont(new Font("Arial", Font.ITALIC, 10));
        panel.add(installNote);
        panel.add(Box.createVerticalStrut(5));
    }

    private static void addPluginSection(JPanel panel, JCheckBox aioPlugin, JCheckBox ultimatePlugin,
                                         JTextField ultimateDir, JFrame frame) {
        JLabel pluginSectionLabel = new JLabel("Plugins");
        pluginSectionLabel.setFont(new Font("Arial", Font.BOLD, 12));
        panel.add(new JPanel(new FlowLayout(FlowLayout.LEFT)) {{ add(pluginSectionLabel); }});
        panel.add(Box.createVerticalStrut(5));
        panel.add(new JPanel(new FlowLayout(FlowLayout.LEFT)) {{ add(aioPlugin); }});
        panel.add(Box.createVerticalStrut(5));
        panel.add(new JPanel(new FlowLayout(FlowLayout.LEFT)) {{ add(ultimatePlugin); }});
        panel.add(Box.createVerticalStrut(5));
        panel.add(createBrowseField("Ultimate Plugin Directory:", ultimateDir, "zip", frame));
        panel.add(Box.createVerticalStrut(10));
    }

    private static JPanel createBrowseField(String label, JTextField field, String fileType, JFrame frame) {
        JPanel container = new JPanel(new BorderLayout());
        container.add(new JLabel(label), BorderLayout.NORTH);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(field, BorderLayout.CENTER);
        JButton browseButton = new JButton("Browse");
        browseButton.addActionListener(e -> {
            String path = fileType.equals("zip") ? openZipFileChooser(frame) : openExeFileChooser(frame);
            if (path != null) field.setText(path);
        });
        inputPanel.add(browseButton, BorderLayout.EAST);
        container.add(inputPanel, BorderLayout.SOUTH);
        return container;
    }

    private static void addButtons(JPanel panel, JTextField urlField, JTextField userField, JPasswordField passField,
                                   JTextArea dirField, JCheckBox mmPrefixCheckbox,
                                   JCheckBox aioPlugin, JCheckBox ultimatePlugin,
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
                    shouldStop = true;
                    Dialog.AlertDialog("⚠️ Kill signal sent. The automation will stop shortly.");
                }
            } else {
                Dialog.AlertDialog("ℹ️ No process running.");
            }
        });

        logButton.addActionListener(e -> openLogWindow());

        submitBtn.addActionListener(e -> {
            shouldStop = false;
            Log.redirectOutputTo();

            String url = urlField.getText().trim();
            String username = userField.getText().trim();
            String password = new String(passField.getPassword()).trim();
            String directory = dirField.getText().trim();
            boolean installAIO = aioPlugin.isSelected();
            boolean installUltimate = ultimatePlugin.isSelected();
            String ultimatePath = ultimateDir.getText().trim();
            boolean quitAfter = quitCheckbox.isSelected();

            if (url.isEmpty() || username.isEmpty() || password.isEmpty() || directory.isEmpty()) {
                Dialog.ErrorDialog("Please fill in all required fields.");
                return;
            }

            if (installUltimate && (ultimatePath.isEmpty() || !ultimatePath.toLowerCase().endsWith(".zip"))) {
                Dialog.ErrorDialog("Please select a valid .zip file for the Ultimate Plugin.");
                return;
            }

            List<String> domains = DomainUitls.splitDomainName(mmPrefixCheckbox.isSelected(), directory);
            FileManagerUtil.saveDataToFile(url, username, password, installAIO, installUltimate,
                    ultimatePath, quitAfter);

            automationThread[0] = new Thread(() -> WordPressInstaller.runInstallAutomation(
                    url, username, password, domains, installAIO, installUltimate,
                    ultimatePath, quitAfter));
            automationThread[0].start();
        });

        removeButton.addActionListener(e -> {
            boolean confirmRemove = Dialog.ConfirmationDialog("Are you sure you want to remove the selected installations?");
            if (!confirmRemove) {
                return;
            }

            shouldStop = false;
            Log.redirectOutputTo();

            String url = urlField.getText().trim();
            String username = userField.getText().trim();
            String password = new String(passField.getPassword()).trim();
            String directory = dirField.getText().trim();
            boolean installAIO = aioPlugin.isSelected();
            boolean installUltimate = ultimatePlugin.isSelected();
            String ultimatePath = ultimateDir.getText().trim();
            boolean quitAfter = quitCheckbox.isSelected();

            if (url.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Dialog.ErrorDialog("Please fill in all required fields.");
                return;
            }

            if (installUltimate && !ultimatePath.toLowerCase().endsWith(".zip")) {
                Dialog.ErrorDialog("Please select a valid .zip file for the Ultimate Plugin.");
                return;
            }

            List<String> domains = DomainUitls.splitDomainName(mmPrefixCheckbox.isSelected(), directory);
            FileManagerUtil.saveDataToFile(url, username, password, installAIO, installUltimate,
                    ultimatePath, quitAfter);

            automationThread[0] = new Thread(() -> WordPressRemover.runRemoveAutomation(
                    url, username, password, domains, quitAfter));
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

    private static void handleCPanelLogin(JTextField urlField, JTextField userField, JPasswordField passField) {
        String url = urlField.getText().trim();
        String username = userField.getText().trim();
        String password = new String(passField.getPassword()).trim();

        if (url.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Dialog.ErrorDialog("Please enter URL, username, and password before logging in.");
            return;
        }


        Log.redirectOutputTo();
        String token = CPanelLogin.ensureValidLogin(url, username, password);
        if (!token.isEmpty()) {
            try {
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

    private static void openLogWindow() {
        if (logWindowRef != null) {
            logWindowRef.setVisible(true);
            logWindowRef.toFront();
            return;
        }

        logWindowRef = new JFrame("WordPress Staging Setup - Log");
        logWindowRef.setSize(500, 450);
        logWindowRef.setResizable(false);
        logWindowRef.setLocationRelativeTo(null);
        logWindowRef.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        try {
            Image icon = new ImageIcon(Main.class.getResource("/image/wordpress-64x64.png")).getImage();
            logWindowRef.setIconImage(icon);
        } catch (Exception e) {
            System.out.println("⚠️ Icon not found or failed to load.");
        }

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        logArea.setMargin(new Insets(20, 20, 0, 20));

        JScrollPane scrollPane = new JScrollPane(logArea);
        logWindowRef.add(scrollPane, BorderLayout.CENTER);

        JButton quitBtn = new JButton("Close Log");
        quitBtn.addActionListener(ev -> logWindowRef.setVisible(false));
        JButton clearBtn = new JButton("Clear Log");
        clearBtn.addActionListener(ev -> logArea.setText(""));

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.add(quitBtn);
        bottomPanel.add(clearBtn);
        logWindowRef.add(bottomPanel, BorderLayout.SOUTH);

        logWindowRef.setVisible(true);
        Log.bindTextArea(logArea);
    }

    private static String openExeFileChooser(Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select an EXE File");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Executable Files (*.exe)", "exe"));

        return (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION)
                ? chooser.getSelectedFile().getAbsolutePath() : null;
    }

    private static String openZipFileChooser(Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a ZIP File");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("ZIP Files", "zip"));

        return (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION)
                ? chooser.getSelectedFile().getAbsolutePath() : null;
    }
}
