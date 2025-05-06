package com.catosolutions.ui;
import com.catosolutions.utils.FileManagerUtil;

import javax.swing.*;
import java.awt.*;

public class Ui {
    public static volatile boolean shouldStop = false;

    public static void loadUi() {
        JFrame frame = MainFrameFactory.createMainFrame();
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JTextField urlField = new JTextField();
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JTextArea dirField = new JTextArea(20, 20);
        dirField.setPreferredSize(new Dimension(0, 60));
        dirField.setWrapStyleWord(true);
        JCheckBox installAIOPluginCheckbox = new JCheckBox("Install 'All-in-One WP Migration and Backup'");
        JCheckBox installUltimateCheckbox = new JCheckBox("Install 'All-in-One WP Migration Unlimited Extension'");
        JTextField ultimatePluginDirField = new JTextField();
        JTextField backupDirField = new JTextField();
        JCheckBox quitAfterFinishCheckbox = new JCheckBox("Quit-Complete");

        LoginPanelBuilder.build(contentPanel, urlField, userField, passField, dirField);

        FileManagerUtil.loadDataFromFile(
                urlField, userField, passField,
                installAIOPluginCheckbox, installUltimateCheckbox, ultimatePluginDirField, quitAfterFinishCheckbox, backupDirField
        );

        PluginSectionBuilder.build(contentPanel, installAIOPluginCheckbox, installUltimateCheckbox, ultimatePluginDirField, frame);
        BackupSectionBuilder.build(contentPanel, backupDirField, frame);

        ActionButtonPanelBuilder.build(contentPanel, urlField, userField, passField,
                installAIOPluginCheckbox, installUltimateCheckbox, ultimatePluginDirField, backupDirField, quitAfterFinishCheckbox);


        frame.add(contentPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }
}
