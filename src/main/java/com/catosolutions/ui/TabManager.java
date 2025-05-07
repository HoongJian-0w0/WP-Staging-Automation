package com.catosolutions.ui;

import com.catosolutions.utils.Dialog;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        attachEnterMoveToEnd(firstDirField);
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

        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));

        area.setPreferredSize(new Dimension(400, 400));

        JPanel mmPanel = new JPanel();
        mmPanel.setLayout(new BoxLayout(mmPanel, BoxLayout.Y_AXIS));
        mmPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        int index = textAreas.size() - 1;
        JCheckBox mm = mmCheckboxes.get(index);
        mm.setAlignmentY(Component.TOP_ALIGNMENT);
        mmPanel.add(mm);
        mmPanel.add(Box.createVerticalGlue());

        JPanel horizontalPanel = new JPanel();
        horizontalPanel.setLayout(new BoxLayout(horizontalPanel, BoxLayout.X_AXIS));
        horizontalPanel.add(checkboxPanel);
        horizontalPanel.add(Box.createRigidArea(new Dimension(4, 0)));
        horizontalPanel.add(area);

        JScrollPane scrollPane = new JScrollPane(horizontalPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(8);
        scrollPane.getVerticalScrollBar().setBlockIncrement(60);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(mmPanel, BorderLayout.EAST);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(-4, 0, -8, 0));
        JCheckBox allCheck = new JCheckBox("All");
        allCheck.addActionListener(e -> {
            boolean selected = allCheck.isSelected();
            for (int i = 0; i < checkboxPanel.getComponentCount(); i++) {
                Component comp = checkboxPanel.getComponent(i);
                if (comp instanceof JCheckBox cb) {
                    try {
                        int start = area.getLineStartOffset(i);
                        int end = area.getLineEndOffset(i);
                        String line = area.getText(start, end - start).trim();
                        cb.setSelected(selected && !line.isEmpty());
                    } catch (Exception ex) {
                        cb.setSelected(false); // fallback
                    }
                }
            }
        });
        bottomPanel.add(allCheck);
        bottomPanel.add(new JLabel(" Select/Deselect All"));

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(centerPanel, BorderLayout.CENTER);
        contentPanel.add(bottomPanel, BorderLayout.SOUTH);
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

        area.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                SwingUtilities.invokeLater(() -> syncCheckboxesWithText(area, checkboxPanel, allCheck));
            }
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                SwingUtilities.invokeLater(() -> syncCheckboxesWithText(area, checkboxPanel, allCheck));
            }
            public void changedUpdate(javax.swing.event.DocumentEvent e) {}
        });

        syncCheckboxesWithText(area, checkboxPanel, allCheck);

        renumberTabs(tabbedPane);
        ensurePlusTabVisible(tabbedPane);
    }

    public static void syncCheckboxesWithText(JTextArea textArea, JPanel checkboxPanel, JCheckBox allCheck) {
        int newLineCount = textArea.getLineCount();

        List<Boolean> oldStates = new ArrayList<>();
        Component[] oldComponents = checkboxPanel.getComponents();
        for (Component comp : oldComponents) {
            if (comp instanceof JCheckBox cb) {
                oldStates.add(cb.isSelected());
            }
        }

        checkboxPanel.removeAll();

        GridBagLayout layout = new GridBagLayout();
        checkboxPanel.setLayout(layout);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        for (int i = 0; i < newLineCount; i++) {
            final int index = i;
            JCheckBox checkBox = new JCheckBox();
            checkBox.setMargin(new Insets(0, -2, 0, 0));
            checkBox.setPreferredSize(new Dimension(18, textArea.getFontMetrics(textArea.getFont()).getHeight()));
            if (i < oldStates.size()) checkBox.setSelected(oldStates.get(i));

            checkBox.addActionListener(e -> {
                try {
                    String lineText = textArea.getText(
                            textArea.getLineStartOffset(index),
                            textArea.getLineEndOffset(index) - textArea.getLineStartOffset(index)
                    ).trim();

                    if (lineText.isEmpty()) {
                        checkBox.setSelected(false);
                        Dialog.AlertDialog("This directory line is empty.");
                    }

                    // Only count checkboxes with non-empty lines
                    int totalValidCheckboxes = 0;
                    int checkedValidCheckboxes = 0;

                    for (int j = 0; j < checkboxPanel.getComponentCount(); j++) {
                        Component comp = checkboxPanel.getComponent(j);
                        if (comp instanceof JCheckBox cb) {
                            String line = "";
                            try {
                                int start = textArea.getLineStartOffset(j);
                                int end = textArea.getLineEndOffset(j);
                                line = textArea.getText(start, end - start).trim();
                            } catch (Exception ignored) {}

                            if (!line.isEmpty()) {
                                totalValidCheckboxes++;
                                if (cb.isSelected()) checkedValidCheckboxes++;
                            }
                        }
                    }

                    // Only set "All" checkbox if all non-empty lines are checked
                    allCheck.setSelected(totalValidCheckboxes > 0 && checkedValidCheckboxes == totalValidCheckboxes);

                } catch (Exception ex) {
                    checkBox.setSelected(false);
                    Dialog.AlertDialog("Invalid line selection.");
                }
            });

            gbc.gridy = i;
            checkboxPanel.add(checkBox, gbc);
        }

        gbc.weighty = 1;
        gbc.gridy = newLineCount;
        checkboxPanel.add(Box.createVerticalGlue(), gbc);

        checkboxPanel.revalidate();
        checkboxPanel.repaint();
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

    private static void attachEnterMoveToEnd(JTextArea area) {
        area.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                if (e.getKeyChar() == '\n') {
                    String[] lines = area.getText().split("\\n");
                    StringBuilder cleaned = new StringBuilder();
                    for (String line : lines) {
                        if (!line.trim().isEmpty()) {
                            cleaned.append(line.trim()).append("\n");
                        }
                    }
                    area.setText(cleaned.toString().trim() + "\n");
                    area.setCaretPosition(area.getText().length());
                }
            }
        });
    }

    public static List<JTextArea> getAllTextAreas() {
        return new ArrayList<>(textAreas);
    }

    public static List<JCheckBox> getAllMMCheckboxes() {
        return new ArrayList<>(mmCheckboxes);
    }

    public static List<JCheckBox> getMmCheckboxes() {
        return mmCheckboxes;
    }

    public static List<JTextArea> getTextAreas() {
        return textAreas;
    }

    public static JTabbedPane getTabPane() {
        return currentTabbedPane;
    }

    public static void createAndAddTab(String content) {
        int tabIndex = textAreas.size() + 1;
        JTextArea newArea = new JTextArea(20, 30);
        newArea.setLineWrap(true);
        newArea.setWrapStyleWord(true);
        attachEnterMoveToEnd(newArea);
        newArea.setText(content);
        textAreas.add(newArea);
        JCheckBox mmCheck = new JCheckBox("MM");
        mmCheck.setFont(new Font("Arial", Font.PLAIN, 10));
        mmCheck.setFocusable(false);
        mmCheckboxes.add(mmCheck);
        addClosableTab(currentTabbedPane, "Dir" + tabIndex, newArea, true);
    }

    public static JTabbedPane getTabbedPane() {
        return currentTabbedPane;
    }

    public static List<String> getCheckedDirectoriesFromActiveTab() {
        int activeIndex = currentTabbedPane.getSelectedIndex();

        // Prevent accessing the '+' tab
        if (activeIndex >= tabComponents.size()) return new ArrayList<>();

        JTextArea area = textAreas.get(activeIndex);
        List<String> checkedLines = new ArrayList<>();

        JPanel tabContent = (JPanel) tabComponents.get(activeIndex);
        JScrollPane scrollPane = (JScrollPane) ((JPanel)((JPanel) tabContent.getComponent(0)).getComponent(0)).getComponent(0);
        JViewport viewport = scrollPane.getViewport();
        JPanel horizontalPanel = (JPanel) viewport.getView();
        JPanel checkboxPanel = (JPanel) horizontalPanel.getComponent(0); // first component = checkboxes

        int lineCount = area.getLineCount();

        int checkboxIdx = 0;
        for (Component comp : checkboxPanel.getComponents()) {
            if (comp instanceof JCheckBox cb && checkboxIdx < lineCount) {
                try {
                    int start = area.getLineStartOffset(checkboxIdx);
                    int end = area.getLineEndOffset(checkboxIdx);
                    String line = area.getText(start, end - start).trim();
                    if (!line.isEmpty() && cb.isSelected()) {
                        checkedLines.add(line);
                    }
                } catch (Exception ignored) {}
                checkboxIdx++;
            }
        }

        return checkedLines;
    }

    public static void removeCheckedLinesFromTab(int tabIndex) {
        if (tabIndex < 0 || tabIndex >= textAreas.size()) {
            Dialog.ErrorDialog("Invalid tab selected.");
            return;
        }

        JTextArea area = textAreas.get(tabIndex);
        JPanel tabContent = (JPanel) tabComponents.get(tabIndex);
        JScrollPane scrollPane = (JScrollPane) ((JPanel)((JPanel) tabContent.getComponent(0)).getComponent(0)).getComponent(0);
        JPanel checkboxPanel = (JPanel) ((JPanel) scrollPane.getViewport().getView()).getComponent(0);
        JPanel bottomPanel = (JPanel) ((JPanel) tabContent.getComponent(0)).getComponent(1); // Bottom panel
        JCheckBox allCheck = null;

        for (Component comp : bottomPanel.getComponents()) {
            if (comp instanceof JCheckBox cb) {
                allCheck = cb;
                break;
            }
        }

        int lineCount = area.getLineCount();
        List<String> remainingLines = new ArrayList<>();
        boolean hasSelected = false;

        try {
            for (int i = 0; i < lineCount; i++) {
                int start = area.getLineStartOffset(i);
                int end = area.getLineEndOffset(i);
                String line = area.getText(start, end - start).trim();

                Component comp = checkboxPanel.getComponent(i);
                if (comp instanceof JCheckBox cb) {
                    if (!line.isEmpty() && cb.isSelected()) {
                        hasSelected = true;
                    } else if (!line.isEmpty()) {
                        remainingLines.add(line);
                    }
                }
            }

            if (!hasSelected) {
                Dialog.AlertDialog("⚠️ Please select at least one item to delete.");
                return;
            }

            if (!Dialog.ConfirmationDialog("Remove all checked entries?")) return;

            area.setText(String.join("\n", remainingLines));
            if (allCheck != null) allCheck.setSelected(false);

            checkboxPanel.removeAll();
            checkboxPanel.revalidate();
            checkboxPanel.repaint();

        } catch (Exception e) {
            Dialog.AlertDialog("Error during deletion: " + e.getMessage());
        }
    }

    public static boolean hasDuplicateDirectories() {
        List<String> checkedDirs = getCheckedDirectoriesFromActiveTab(); // only from the active tab
        Set<String> seen = new HashSet<>();

        for (String dir : checkedDirs) {
            String trimmed = dir.trim();
            if (!trimmed.isEmpty()) {
                if (!seen.add(trimmed)) {
                    Dialog.ErrorDialog("Duplicate checked directory/domain found: " + trimmed);
                    return true;
                }
            }
        }

        return false;
    }

}
