package com.catosolutions.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TabManager {
    private static final int MAX_TAB_COUNT = 64;
    private static boolean isAddingTab = false;
    private static final List<Component> tabComponents = new ArrayList<>();
    private static final List<JTextArea> textAreas = new ArrayList<>();
    private static final List<JCheckBox> mmCheckboxes = new ArrayList<>();

    private static JPanel addTabPanel;
    private static JLabel plusLabel;
    private static JTabbedPane currentTabbedPane;

    public static void initialize(JTabbedPane tabbedPane, JTextArea firstDirField) {
        currentTabbedPane = tabbedPane;

        addTabPanel = new JPanel();
        plusLabel = new JLabel("+");
        plusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        tabbedPane.addTab("", addTabPanel);
        tabbedPane.setTabComponentAt(tabbedPane.indexOfComponent(addTabPanel), plusLabel);

        textAreas.clear();
        mmCheckboxes.clear();

        textAreas.add(firstDirField);
        firstDirField.setRows(20);
        JCheckBox mmCheck = new JCheckBox("MM");
        mmCheck.setFont(new Font("Arial", Font.PLAIN, 10));
        mmCheck.setFocusable(false);
        mmCheckboxes.add(mmCheck);

        addClosableTab(tabbedPane, "Dir1", firstDirField, false);
        tabbedPane.setSelectedIndex(0);

        tabbedPane.addChangeListener(e -> {
            if (isAddingTab) return;

            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex == tabbedPane.indexOfComponent(addTabPanel)) {
                isAddingTab = true;
                int tabIndex = tabComponents.size() + 1;
                JTextArea newArea = new JTextArea(20, 30);
                newArea.setLineWrap(true);
                newArea.setWrapStyleWord(true);

                textAreas.add(newArea);
                JCheckBox mm = new JCheckBox("MM");
                mm.setFont(new Font("Arial", Font.PLAIN, 10));
                mm.setFocusable(false);
                mmCheckboxes.add(mm);

                addClosableTab(tabbedPane, "Dir" + tabIndex, newArea, true);
                tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 2);
                isAddingTab = false;
            }
        });
    }

    private static void addClosableTab(JTabbedPane tabbedPane, String title, JTextArea area, boolean closeable) {
        int plusTabIndex = tabbedPane.indexOfComponent(addTabPanel);

        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JPanel mmPanel = new JPanel();
        mmPanel.setLayout(new BoxLayout(mmPanel, BoxLayout.Y_AXIS));
        mmPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        int index = textAreas.size() - 1;
        JCheckBox mm = mmCheckboxes.get(index);
        mm.setAlignmentY(Component.TOP_ALIGNMENT);
        mmPanel.add(mm);
        mmPanel.add(Box.createVerticalGlue());

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.add(mmPanel, BorderLayout.EAST);
        contentPanel.setPreferredSize(new Dimension(0, 200));
        contentPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        JPanel tabContent = new JPanel(new BorderLayout());
        tabContent.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        tabContent.add(contentPanel, BorderLayout.CENTER);

        tabbedPane.insertTab(title, null, tabContent, null, plusTabIndex);
        tabComponents.add(tabContent);

        JPanel tabHeader = new JPanel(new BorderLayout());
        tabHeader.setOpaque(false);
        tabHeader.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

        JLabel tabTitle = new JLabel(title);
        tabTitle.setFont(new Font("Arial", Font.PLAIN, 12));
        tabHeader.add(tabTitle, BorderLayout.WEST);

        if (closeable) {
            JButton closeBtn = createCloseButton(tabbedPane, tabContent);
            JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            closePanel.setOpaque(false);
            closePanel.add(closeBtn);
            tabHeader.add(closePanel, BorderLayout.EAST);
        }

        int newIndex = tabbedPane.indexOfComponent(tabContent);
        tabbedPane.setTabComponentAt(newIndex, tabHeader);

        renumberTabs(tabbedPane);
        ensurePlusTabVisible(tabbedPane);
    }

    private static JButton createCloseButton(JTabbedPane tabbedPane, Component content) {
        JButton closeBtn = new JButton("x");
        closeBtn.setMargin(new Insets(0, 0, 0, 0));
        closeBtn.setBorder(BorderFactory.createEmptyBorder());
        closeBtn.setContentAreaFilled(false);
        closeBtn.setFocusable(false);
        closeBtn.setFont(new Font("Arial", Font.BOLD, 14));
        closeBtn.setForeground(Color.RED);

        closeBtn.addActionListener(e -> {
            int index = tabbedPane.indexOfComponent(content);
            if (index != -1 && tabComponents.size() > 1) {
                if (tabbedPane.getSelectedIndex() == index) {
                    tabbedPane.setSelectedIndex(Math.max(0, index - 1));
                }

                SwingUtilities.invokeLater(() -> {
                    int tabDataIndex = tabComponents.indexOf(content);
                    if (tabDataIndex != -1) {
                        if (tabDataIndex < textAreas.size()) textAreas.remove(tabDataIndex);
                        if (tabDataIndex < mmCheckboxes.size()) mmCheckboxes.remove(tabDataIndex);
                        tabComponents.remove(tabDataIndex);
                    }

                    tabbedPane.remove(content);
                    renumberTabs(tabbedPane);
                    ensurePlusTabVisible(tabbedPane);
                });
            }
        });

        return closeBtn;
    }

    private static void renumberTabs(JTabbedPane tabbedPane) {
        for (int i = 0; i < tabComponents.size(); i++) {
            Component c = tabComponents.get(i);
            int index = tabbedPane.indexOfComponent(c);
            if (index == -1) continue;

            JPanel header = new JPanel();
            header.setOpaque(false);
            header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
            header.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

            JLabel label = new JLabel("Dir" + (i + 1));
            header.add(label);
            header.add(Box.createHorizontalGlue());

            if (i != 0) {
                header.add(Box.createRigidArea(new Dimension(4, 0)));
                header.add(createCloseButton(tabbedPane, c));
            }

            tabbedPane.setTabComponentAt(index, header);
        }
    }

    private static void ensurePlusTabVisible(JTabbedPane tabbedPane) {
        if (tabComponents.size() >= MAX_TAB_COUNT) {
            int plusTabIndex = tabbedPane.indexOfComponent(addTabPanel);
            if (plusTabIndex != -1) {
                tabbedPane.remove(addTabPanel);
            }
        } else {
            if (tabbedPane.indexOfComponent(addTabPanel) == -1) {
                tabbedPane.addTab("", addTabPanel);
                tabbedPane.setTabComponentAt(tabbedPane.indexOfComponent(addTabPanel), plusLabel);
            }
        }
    }

    public static List<JTextArea> getAllTextAreas() {
        return new ArrayList<>(textAreas);
    }

    public static JTabbedPane getTabPane() {
        return currentTabbedPane;
    }

    public static void createAndAddTab(String content) {
        int tabIndex = textAreas.size() + 1;
        JTextArea newArea = new JTextArea(20, 30);
        newArea.setLineWrap(true);
        newArea.setWrapStyleWord(true);
        newArea.setText(content);

        textAreas.add(newArea);

        JCheckBox mmCheck = new JCheckBox("MM");
        mmCheck.setFont(new Font("Arial", Font.PLAIN, 10));
        mmCheck.setFocusable(false);
        mmCheckboxes.add(mmCheck);

        addClosableTab(currentTabbedPane, "Dir" + tabIndex, newArea, true);
    }

    public static List<JCheckBox> getAllMMCheckboxes() {
        return new ArrayList<>(mmCheckboxes);
    }

    public static void selectFirstTabAndScroll() {
        if (currentTabbedPane == null || currentTabbedPane.getTabCount() == 0) return;

        try {
            // Workaround: temporarily switch to another tab (if exists), then switch back to index 0
            if (currentTabbedPane.getTabCount() > 1) {
                currentTabbedPane.setSelectedIndex(1);
            }

            // Select Dir1 (index 0)
            currentTabbedPane.setSelectedIndex(0);

            // Scroll to Dir1 (forces left-most tab into view)
            SwingUtilities.invokeLater(() -> {
                Rectangle rect = currentTabbedPane.getBoundsAt(0);
                currentTabbedPane.scrollRectToVisible(rect);
            });

        } catch (Exception e) {
            e.printStackTrace(); // Just in case something weird happens
        }
    }

    public static JTabbedPane getTabbedPane() {
        return currentTabbedPane;
    }

    public static List<JCheckBox> getMmCheckboxes() {
        return mmCheckboxes;
    }

    public static List<JTextArea> getTextAreas() {
        return textAreas;
    }

}
