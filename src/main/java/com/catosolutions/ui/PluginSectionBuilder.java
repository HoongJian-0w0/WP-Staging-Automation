package com.catosolutions.ui;

import javax.swing.*;
import java.awt.*;

public class PluginSectionBuilder {
    public static void build(JPanel panel, JCheckBox aioPlugin, JCheckBox ultimatePlugin,
                             JTextField ultimateDir, JFrame frame) {
        JLabel pluginSectionLabel = new JLabel("Plugins");
        pluginSectionLabel.setFont(new Font("Arial", Font.BOLD, 12));
        panel.add(new JPanel(new FlowLayout(FlowLayout.LEFT)) {{ add(pluginSectionLabel); }});
        panel.add(Box.createVerticalStrut(5));
        panel.add(new JPanel(new FlowLayout(FlowLayout.LEFT)) {{ add(aioPlugin); }});
        panel.add(Box.createVerticalStrut(5));
        panel.add(new JPanel(new FlowLayout(FlowLayout.LEFT)) {{ add(ultimatePlugin); }});
        panel.add(Box.createVerticalStrut(5));
        panel.add(FileChooserUtils.createBrowseField("Ultimate Plugin Directory:", ultimateDir, "zip", frame));
        panel.add(Box.createVerticalStrut(10));
    }
}
