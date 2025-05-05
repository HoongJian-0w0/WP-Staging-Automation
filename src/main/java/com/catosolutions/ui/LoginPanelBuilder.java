package com.catosolutions.ui;

import com.catosolutions.utils.Dialog;
import com.catosolutions.utils.FileManagerUtil;

import javax.swing.*;
import java.awt.*;

public class LoginPanelBuilder {

    public static void build(JPanel panel, JTextField urlField, JTextField userField,
                             JPasswordField passField, JTextArea dirField) {

        JPanel urlPanel = new JPanel(new BorderLayout());
        urlPanel.add(new JLabel("Cpanel URL:"), BorderLayout.NORTH);
        urlPanel.add(urlField, BorderLayout.CENTER);

        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> CPanelLoginHandler.handleLogin(urlField, userField, passField));
        urlPanel.add(loginButton, BorderLayout.EAST);
        panel.add(urlPanel);

        panel.add(Box.createVerticalStrut(15));
        panel.add(FileManagerUtil.createLabeledPanel("Username:", userField));
        panel.add(Box.createVerticalStrut(15));
        panel.add(FileManagerUtil.createLabeledPanel("Password:", passField));
        panel.add(Box.createVerticalStrut(15));

        JPanel dirRowPanel = new JPanel();
        dirRowPanel.setLayout(new BoxLayout(dirRowPanel, BoxLayout.Y_AXIS));
        dirRowPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        JPanel labelWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        JLabel label = new JLabel("Install Directory:");
        label.setFont(new Font("Arial", Font.BOLD, 12));
        labelWrapper.add(label);
        dirRowPanel.add(labelWrapper);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        TabManager.initialize(tabbedPane, dirField);

        JPanel tabWrapper = new JPanel(new BorderLayout());
        tabWrapper.setPreferredSize(new Dimension(Integer.MAX_VALUE, 190)); // Control max height
        tabWrapper.add(tabbedPane, BorderLayout.CENTER);
        dirRowPanel.add(tabWrapper);

        JPanel bottomControlWrapper = new JPanel(new BorderLayout());

        JPanel bottomLeftWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        JButton saveButton = new JButton("Save");
        saveButton.setFont(new Font("Arial", Font.PLAIN, 11));
        saveButton.setMargin(new Insets(4, 10, 4, 10));
        saveButton.addActionListener(e -> {
            FileManagerUtil.saveOnlyTabDataToFile();
        });

        JButton clearButton = new JButton("Clear");
        clearButton.setFont(new Font("Arial", Font.PLAIN, 11));
        clearButton.setMargin(new Insets(4, 10, 4, 10));
        clearButton.addActionListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex < 0 || selectedIndex >= TabManager.getAllTextAreas().size()) {
                Dialog.ErrorDialog("No active tab to clear.");
                return;
            }

            boolean confirm = Dialog.ConfirmationDialog("Clear current tab content?");
            if (confirm) {
                JTextArea area = TabManager.getAllTextAreas().get(selectedIndex);
                JCheckBox mm = TabManager.getAllMMCheckboxes().get(selectedIndex);
                area.setText("");
                mm.setSelected(false);
                Dialog.SuccessDialog("Current tab cleared.");
            }
        });

        JLabel automationNote = new JLabel("(Automation for domain splitting applied)");
        automationNote.setFont(new Font("Arial", Font.ITALIC, 10));

        bottomLeftWrapper.add(saveButton);
        bottomLeftWrapper.add(clearButton);
        bottomLeftWrapper.add(Box.createHorizontalStrut(10));
        bottomLeftWrapper.add(automationNote);
        bottomControlWrapper.add(bottomLeftWrapper, BorderLayout.WEST);

        JPanel bottomRightWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton addTabButton = new JButton("+");
        addTabButton.setFont(new Font("Arial", Font.BOLD, 12));
        addTabButton.setMargin(new Insets(4, 10, 4, 10));
        addTabButton.addActionListener(e -> {
            TabManager.createAndAddTab("");
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 2);
        });

        bottomRightWrapper.add(addTabButton);
        bottomControlWrapper.add(bottomRightWrapper, BorderLayout.EAST);

        dirRowPanel.add(bottomControlWrapper);
        panel.add(dirRowPanel);
        panel.add(Box.createVerticalStrut(5));
    }
}
