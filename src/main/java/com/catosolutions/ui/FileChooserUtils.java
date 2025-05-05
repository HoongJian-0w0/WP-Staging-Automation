package com.catosolutions.ui;

import javax.swing.*;
import java.awt.*;

public class FileChooserUtils {
    public static JPanel createBrowseField(String label, JTextField field, String fileType, JFrame frame) {
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

    private static String openExeFileChooser(Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select an EXE File");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Executable Files (*.exe)", "exe"));

        return (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION)
                ? chooser.getSelectedFile().getAbsolutePath() : null;
    }

    private static String openZipFileChooser(Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a ZIP File");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("ZIP Files", "zip"));

        return (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION)
                ? chooser.getSelectedFile().getAbsolutePath() : null;
    }
}
