package com.catosolutions.ui;

import com.catosolutions.Main;

import javax.swing.*;
import java.awt.*;

public class MainFrameFactory {
    public static JFrame createMainFrame() {
        JFrame frame = new JFrame("WordPress Staging Setup");
        frame.setResizable(false);
        frame.setSize(600, 750);
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
}
