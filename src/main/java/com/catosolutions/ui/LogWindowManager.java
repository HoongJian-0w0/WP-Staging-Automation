package com.catosolutions.ui;

import com.catosolutions.Main;
import com.catosolutions.utils.Log;

import javax.swing.*;
import java.awt.*;

public class LogWindowManager {
    private static JFrame logWindowRef = null;
    private static JTextArea logArea = null;

    public static void open() {
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
}
