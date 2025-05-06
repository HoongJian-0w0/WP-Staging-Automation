package com.catosolutions.ui;

import javax.swing.*;

public class BackupSectionBuilder {
    public static void build(JPanel panel, JTextField backupDirField, JFrame frame) {
        panel.add(Box.createVerticalStrut(5));
        panel.add(FileChooserUtils.createDirectoryChooserField("Backup Directory:", backupDirField, frame));
        panel.add(Box.createVerticalStrut(10));

    }
}
