package com.josegonzalezal.filesearcher.ui;

import com.josegonzalezal.filesearcher.config.AppConfig;
import com.josegonzalezal.filesearcher.config.LogManager;
import com.josegonzalezal.filesearcher.config.ThemePreferences;
import com.josegonzalezal.filesearcher.model.DuplicateGroup;
import com.josegonzalezal.filesearcher.model.HashAlgorithm;
import com.josegonzalezal.filesearcher.model.MatchMode;
import com.josegonzalezal.filesearcher.model.SearchCriteria;
import com.josegonzalezal.filesearcher.model.SearchResultItem;
import com.josegonzalezal.filesearcher.model.SearchTarget;
import com.josegonzalezal.filesearcher.service.DuplicateDetectorService;
import com.josegonzalezal.filesearcher.service.FileActionService;
import com.josegonzalezal.filesearcher.service.FileSystemScannerService;
import com.josegonzalezal.filesearcher.service.ReportExportService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.plaf.basic.BasicToggleButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

public final class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String GITHUB_URL = "https://github.com/josegonzalezal/";

    private static final String[] SEARCH_COLUMNS = {
        "Name", "Type", "Ext", "Size", "Last Modified", "Full Path"
    };

    private static final String[] DUPLICATE_COLUMNS = {
        "Group / Hash", "File Name", "Size", "Reclaimable", "Last Modified", "Full Path"
    };

    private final AppConfig config;
    private final LogManager logManager;
    private final ThemePreferences themePreferences = new ThemePreferences();
    private final FileSystemScannerService scannerService;
    private final DuplicateDetectorService duplicateService;
    private final FileActionService fileActionService;
    private final ReportExportService reportExportService;

    private final DefaultListModel<String> rootsListModel = new DefaultListModel<>();
    private final JList<String> rootsList = new JList<>(rootsListModel);

    private final JTextField namePatternField = new JTextField(22);
    private final JComboBox<MatchMode> matchModeCombo = new JComboBox<>(MatchMode.values());
    private final JComboBox<SearchTarget> targetCombo = new JComboBox<>(SearchTarget.values());
    private final JTextField extensionsField = new JTextField(14);
    private final JTextField minSizeField = new JTextField(8);
    private final JTextField maxSizeField = new JTextField(8);
    private final JCheckBox caseSensitiveCheck = new JCheckBox("Case sensitive");
    private final JCheckBox includeHiddenCheck = new JCheckBox("Include hidden");
    private final JCheckBox skipSymlinksCheck = new JCheckBox("Skip symlinks", true);
    private final JCheckBox enforceLimitCheck = new JCheckBox(
            "Cap results at " + AppConfig.DEFAULT_DISPLAY_LIMIT, true);

    private final JButton startSearchBtn = new JButton("Search");
    private final JButton cancelSearchBtn = new JButton("Cancel");
    private final JToggleButton darkModeToggleBtn = new JToggleButton("Dark Mode");
    private final JButton aboutBtn = new JButton("About");
    private final JButton helpBtn = new JButton("Help");
    private final JButton exportCsvBtn = new JButton("Export CSV");
    private final JButton exportTxtBtn = new JButton("Export TXT");
    private final JButton clearSearchBtn = new JButton("Clear Results");

    private final List<SearchResultItem> currentSearchItems = new ArrayList<>();
    private final DefaultTableModel searchTableModel = new DefaultTableModel(SEARCH_COLUMNS, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable searchTable = new JTable(searchTableModel);

    private final JComboBox<HashAlgorithm> hashAlgoCombo = new JComboBox<>(HashAlgorithm.values());
    private final JButton startDupBtn = new JButton("Find Duplicates");
    private final JButton cancelDupBtn = new JButton("Cancel");
    private final JButton deleteSelectedDupBtn = new JButton("Delete Selected Files");
    private final JButton exportDupCsvBtn = new JButton("Export CSV");
    private final List<SearchResultItem> currentDuplicateRows = new ArrayList<>();
    private final DefaultTableModel duplicateTableModel = new DefaultTableModel(DUPLICATE_COLUMNS, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable duplicateTable = new JTable(duplicateTableModel);

    private final JProgressBar progressBar = new JProgressBar();
    private final JLabel statusLabel = new JLabel("Ready.");
    private final JTextArea logArea = new JTextArea(7, 80);

    private final AtomicBoolean activeCancelFlag = new AtomicBoolean(false);
    private SwingWorker<?, ?> activeWorker = null;
    private boolean darkMode;

    public MainFrame(AppConfig config, LogManager logManager) {
        super(AppConfig.APP_NAME);
        this.config = config;
        this.logManager = logManager;
        this.scannerService = new FileSystemScannerService(logManager);
        this.duplicateService = new DuplicateDetectorService(config, logManager);
        this.fileActionService = new FileActionService(logManager);
        this.reportExportService = new ReportExportService(logManager);

        this.darkMode = themePreferences.isDarkMode();
        this.skipSymlinksCheck.setSelected(config.isSkipSymbolicLinks());
        this.includeHiddenCheck.setSelected(config.isIncludeHidden());
        this.enforceLimitCheck.setSelected(config.isEnforceDisplayLimit());
        this.hashAlgoCombo.setSelectedItem(config.getDefaultHashAlgorithm());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1024, 680));
        setSize(1200, 780);
        setLocationRelativeTo(null);

        initComponents();
        wireEvents();
        loadInitialRoots();
        attachLogListener();
        applyTheme(this.darkMode, false);

        logManager.info("Application initialized (Java " + System.getProperty("java.version") + ").");
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setUI(new BasicTabbedPaneUI());
        tabs.addTab("File Search", buildSearchTab());
        tabs.addTab("Duplicate Finder", buildDuplicatesTab());

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabs, buildBottomPanel());
        mainSplit.setResizeWeight(0.76);
        mainSplit.setOneTouchExpandable(true);

        add(mainSplit, BorderLayout.CENTER);
    }

    private JPanel buildSearchTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.add(buildRootsPanel(), BorderLayout.WEST);
        topPanel.add(buildSearchCriteriaPanel(), BorderLayout.CENTER);

        searchTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        searchTable.setAutoCreateRowSorter(true);
        configureTableColumnWidths(searchTable, new int[]{220, 85, 70, 95, 145, 450});

        JScrollPane tableScroll = new JScrollPane(searchTable);
        JPanel centerPanel = new JPanel(new BorderLayout(6, 6));
        centerPanel.add(tableScroll, BorderLayout.CENTER);
        centerPanel.add(buildSearchActionsBar(), BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildRootsPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Search Roots"));
        panel.setPreferredSize(new Dimension(280, 185));

        rootsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(rootsList), BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton addRootBtn = new JButton("Add Folder...");
        JButton removeRootBtn = new JButton("Remove");

        addRootBtn.addActionListener(e -> chooseAndAddRoot());
        removeRootBtn.addActionListener(e -> {
            int idx = rootsList.getSelectedIndex();
            if (idx >= 0) {
                rootsListModel.remove(idx);
            }
        });

        btns.add(addRootBtn);
        btns.add(removeRootBtn);
        panel.add(btns, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildSearchCriteriaPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Search Criteria & Filters"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;

        namePatternField.setToolTipText("Name or pattern (e.g. report, *.pdf, ^IMG_\\d+)");
        extensionsField.setToolTipText("Comma-separated extensions without dot, e.g. pdf, jpg, txt");
        minSizeField.setToolTipText("e.g. 500B, 10KB, 5MB, 1GB");
        maxSizeField.setToolTipText("e.g. 50MB, 2GB");

        gbc.gridx = 0; gbc.gridy = 0; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Name / Pattern:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(namePatternField, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Match:"), gbc);
        gbc.gridx = 3;
        panel.add(matchModeCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Target:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(targetCombo, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("Extensions:"), gbc);
        gbc.gridx = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.6;
        panel.add(extensionsField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Min Size:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5;
        panel.add(minSizeField, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Max Size:"), gbc);
        gbc.gridx = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5;
        panel.add(maxSizeField, gbc);

        JPanel flagsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        flagsPanel.add(caseSensitiveCheck);
        flagsPanel.add(includeHiddenCheck);
        flagsPanel.add(skipSymlinksCheck);
        flagsPanel.add(enforceLimitCheck);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(flagsPanel, gbc);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        cancelSearchBtn.setEnabled(false);
        actionPanel.add(startSearchBtn);
        actionPanel.add(cancelSearchBtn);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(actionPanel, gbc);

        return panel;
    }

    private JPanel buildSearchActionsBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        bar.add(darkModeToggleBtn);
        bar.add(aboutBtn);
        bar.add(helpBtn);
        bar.add(exportCsvBtn);
        bar.add(exportTxtBtn);
        bar.add(clearSearchBtn);
        return bar;
    }

    private JPanel buildDuplicatesTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        topBar.setBorder(BorderFactory.createTitledBorder("Duplicate Detection Options"));
        topBar.add(new JLabel("Algorithm:"));
        topBar.add(hashAlgoCombo);
        cancelDupBtn.setEnabled(false);
        topBar.add(startDupBtn);
        topBar.add(cancelDupBtn);
        topBar.add(deleteSelectedDupBtn);
        topBar.add(exportDupCsvBtn);

        duplicateTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        duplicateTable.setAutoCreateRowSorter(true);
        configureTableColumnWidths(duplicateTable, new int[]{240, 220, 95, 100, 145, 420});

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(duplicateTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildBottomPanel() {
        JPanel bottom = new JPanel(new BorderLayout(6, 6));
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

        JPanel statusBar = new JPanel(new BorderLayout(8, 0));
        progressBar.setPreferredSize(new Dimension(220, 18));
        progressBar.setStringPainted(true);
        statusBar.add(statusLabel, BorderLayout.CENTER);
        statusBar.add(progressBar, BorderLayout.EAST);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Logs"));

        bottom.add(statusBar, BorderLayout.NORTH);
        bottom.add(logScroll, BorderLayout.CENTER);
        return bottom;
    }

    private void wireEvents() {
        startSearchBtn.addActionListener(e -> startSearch());
        cancelSearchBtn.addActionListener(e -> cancelActiveTask());
        darkModeToggleBtn.addActionListener(e -> toggleDarkMode());
        aboutBtn.addActionListener(e -> showAboutDialog());
        helpBtn.addActionListener(e -> showHelpDialog());
        clearSearchBtn.addActionListener(e -> clearSearchResults());
        exportCsvBtn.addActionListener(e -> exportSearchCsv());
        exportTxtBtn.addActionListener(e -> exportSearchTxt());

        startDupBtn.addActionListener(e -> startDuplicateScan());
        cancelDupBtn.addActionListener(e -> cancelActiveTask());
        deleteSelectedDupBtn.addActionListener(e -> deleteSelectedDuplicates());
        exportDupCsvBtn.addActionListener(e -> exportDuplicatesCsv());

        searchTable.addMouseListener(createTablePopupAdapter(searchTable, true));
        duplicateTable.addMouseListener(createTablePopupAdapter(duplicateTable, false));
    }

    private void toggleDarkMode() {
        applyTheme(darkModeToggleBtn.isSelected(), true);
    }

    private void applyTheme(boolean dark, boolean persist) {
        this.darkMode = dark;
        if (persist) {
            themePreferences.setDarkMode(dark);
        }

        ThemePalette palette = ThemePalette.fromDarkMode(dark);
        darkModeToggleBtn.setSelected(dark);
        darkModeToggleBtn.setText(dark ? "Light Mode" : "Dark Mode");

        getContentPane().setBackground(palette.windowBg());
        applyPaletteRecursive(getContentPane(), palette);
        styleTable(searchTable, palette);
        styleTable(duplicateTable, palette);
        styleLogArea(palette);
        revalidate();
        repaint();
    }

    private void applyPaletteRecursive(Component comp, ThemePalette palette) {
        if (comp instanceof JCheckBox cb) {
            cb.setBackground(palette.panelBg());
            cb.setForeground(palette.textPrimary());
            cb.setOpaque(true);
        } else if (comp instanceof JComboBox<?> combo) {
            styleComboBox(combo, palette);
        } else if (comp instanceof JProgressBar bar) {
            styleProgressBar(bar, palette);
        } else if (comp instanceof JTextField
                || comp instanceof JPasswordField
                || comp instanceof JList<?>) {
            comp.setBackground(palette.inputBg());
            comp.setForeground(palette.textPrimary());
            if (comp instanceof JComponent jComp) {
                jComp.setOpaque(true);
            }
        } else if (comp instanceof JToggleButton toggle) {
            toggle.setUI(new BasicToggleButtonUI());
            toggle.setOpaque(true);
            toggle.setBackground(toggle.isSelected() ? palette.selectionBg() : palette.elevatedBg());
            toggle.setForeground(toggle.isSelected() ? palette.selectionFg() : palette.textPrimary());
            toggle.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(palette.borderColor(), 1),
                    BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        } else if (comp instanceof JButton btn) {
            styleButton(btn, palette);
        } else if (comp instanceof JTabbedPane tabs) {
            tabs.setUI(new BasicTabbedPaneUI());
            tabs.setBackground(palette.panelBg());
            tabs.setForeground(palette.textPrimary());
            tabs.setOpaque(true);
        } else if (comp instanceof JViewport vp) {
            vp.setBackground(palette.panelBg());
            vp.setOpaque(true);
        } else if (comp instanceof JPanel
                || comp instanceof JSplitPane
                || comp instanceof JScrollPane) {
            comp.setBackground(palette.panelBg());
            comp.setForeground(palette.textPrimary());
            if (comp instanceof JComponent jComp) {
                jComp.setOpaque(true);
            }
        } else if (comp instanceof JLabel) {
            comp.setForeground(palette.textPrimary());
        }

        if (comp instanceof JComponent jComp && jComp.getBorder() instanceof TitledBorder titled) {
            titled.setTitleColor(palette.textSecondary());
        }

        if (comp instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyPaletteRecursive(child, palette);
            }
        }
    }

    private void styleButton(JButton btn, ThemePalette palette) {
        Color disabledFg = darkMode ? new Color(148, 163, 184) : new Color(120, 128, 140);
        btn.setUI(new BasicButtonUI() {
            @Override
            protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
                FontMetrics fm = g.getFontMetrics(b.getFont());
                g.setColor(b.isEnabled() ? palette.textPrimary() : disabledFg);
                g.drawString(text, textRect.x, textRect.y + fm.getAscent());
            }
        });
        btn.setOpaque(true);
        btn.setBackground(palette.elevatedBg());
        btn.setForeground(btn.isEnabled() ? palette.textPrimary() : disabledFg);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(palette.borderColor(), 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
    }

    @SuppressWarnings("unchecked")
    private void styleComboBox(JComboBox<?> combo, ThemePalette palette) {
        DefaultListCellRenderer renderer = new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                lbl.setOpaque(true);
                lbl.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                if (isSelected) {
                    lbl.setBackground(palette.selectionBg());
                    lbl.setForeground(palette.selectionFg());
                } else {
                    lbl.setBackground(palette.inputBg());
                    lbl.setForeground(palette.textPrimary());
                }
                return lbl;
            }
        };

        combo.setRenderer(renderer);
        combo.setBackground(palette.inputBg());
        combo.setForeground(palette.textPrimary());
        combo.setBorder(BorderFactory.createLineBorder(palette.borderColor(), 1));
        combo.setOpaque(true);

        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton btn = new BasicArrowButton(
                        SwingConstants.SOUTH,
                        palette.elevatedBg(),
                        palette.borderColor(),
                        palette.textPrimary(),
                        palette.borderColor());
                btn.setBorder(BorderFactory.createLineBorder(palette.borderColor(), 1));
                return btn;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(palette.inputBg());
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            @Override
            public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
                ListCellRenderer<Object> currentRenderer = (ListCellRenderer<Object>) combo.getRenderer();
                Object selected = combo.getSelectedItem();
                Component c = currentRenderer.getListCellRendererComponent(
                        listBox, selected, -1, false, false);
                c.setFont(combo.getFont());
                c.setBackground(palette.inputBg());
                c.setForeground(combo.isEnabled() ? palette.textPrimary() : palette.textSecondary());
                if (c instanceof JComponent jc) {
                    jc.setOpaque(true);
                }
                boolean shouldValidate = c instanceof JPanel;
                currentValuePane.paintComponent(
                        g, c, comboBox, bounds.x, bounds.y, bounds.width, bounds.height, shouldValidate);
            }
        });
    }

    private void styleProgressBar(JProgressBar bar, ThemePalette palette) {
        bar.setUI(new BasicProgressBarUI() {
            @Override
            protected Color getSelectionBackground() {
                return palette.textPrimary();
            }

            @Override
            protected Color getSelectionForeground() {
                return palette.selectionFg();
            }
        });
        bar.setBackground(palette.inputBg());
        bar.setForeground(palette.selectionBg());
        bar.setBorder(BorderFactory.createLineBorder(palette.borderColor(), 1));
        bar.setOpaque(true);
    }

    private void styleTable(JTable table, ThemePalette palette) {
        table.setBackground(palette.tableBg());
        table.setForeground(palette.textPrimary());
        table.setGridColor(palette.tableGrid());
        table.setSelectionBackground(palette.selectionBg());
        table.setSelectionForeground(palette.selectionFg());
        table.setFillsViewportHeight(true);

        if (table.getParent() instanceof JViewport vp) {
            vp.setBackground(palette.tableBg());
            vp.setOpaque(true);
        }

        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setBackground(palette.elevatedBg());
            header.setForeground(palette.textPrimary());
            header.setOpaque(true);
            header.setDefaultRenderer(new DefaultTableCellRenderer() {
                private static final long serialVersionUID = 1L;

                @Override
                public Component getTableCellRendererComponent(
                        JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                            tbl, value, isSelected, hasFocus, row, column);
                    lbl.setBackground(palette.elevatedBg());
                    lbl.setForeground(palette.textPrimary());
                    lbl.setOpaque(true);
                    lbl.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 1, palette.tableGrid()),
                            BorderFactory.createEmptyBorder(4, 6, 4, 6)));
                    return lbl;
                }
            });
        }
    }

    private void styleLogArea(ThemePalette palette) {
        logArea.setBackground(palette.inputBg());
        logArea.setForeground(palette.textPrimary());
        logArea.setCaretColor(palette.textPrimary());
        logArea.setOpaque(true);
        if (logArea.getParent() instanceof JViewport vp) {
            vp.setBackground(palette.inputBg());
            vp.setOpaque(true);
        }
    }

    private void showAboutDialog() {
        ThemePalette palette = ThemePalette.fromDarkMode(darkMode);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JLabel titleLabel = new JLabel(AppConfig.APP_NAME + " v1.0.0");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel descLabel = new JLabel("Multi-root file search and duplicate detector utility.");
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel authorLabel = new JLabel("Author: José González Alonso");
        authorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel githubRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        githubRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel githubPrefixLabel = new JLabel("GitHub: ");
        JLabel githubUrlLabel = new JLabel("<html><a href=\"" + GITHUB_URL + "\">" + GITHUB_URL + "</a></html>");
        githubUrlLabel.setForeground(palette.linkColor());
        githubUrlLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        githubUrlLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openGitHubUrl();
            }
        });

        githubRow.add(githubPrefixLabel);
        githubRow.add(githubUrlLabel);

        panel.add(titleLabel);
        panel.add(new JLabel(" "));
        panel.add(descLabel);
        panel.add(authorLabel);
        panel.add(new JLabel(" "));
        panel.add(githubRow);

        JOptionPane.showMessageDialog(
                this,
                panel,
                "About - " + AppConfig.APP_NAME,
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void openGitHubUrl() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(GITHUB_URL));
                return;
            }
        } catch (Exception ex) {
            logManager.error("Failed opening GitHub URL in browser", ex);
        }
        JOptionPane.showMessageDialog(
                this,
                "GitHub URL: " + GITHUB_URL,
                "GitHub",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showHelpDialog() {
        String helpText = """
                File Searcher - Quick Help

                Search Roots:
                - Select one or more roots to scan, or leave none selected to scan all listed roots.
                - Use Add Folder... / Remove to manage target directories.
                - Missing roots, broken symlinks, or unreadable entries are skipped and logged.

                Filters & Size:
                - Name / Pattern: plain text, wildcards (*, ?), or regex depending on Match mode.
                - Extensions: comma-separated list without dot (e.g. pdf, jpg, log).
                - Min / Max Size: supports B, KB, MB, GB, TB (e.g. 500B, 10MB, 1.5GB).
                - Flags: Case sensitive, Include hidden, Skip symlinks, and Cap results limit output.

                Results & Export:
                - Double-click a row or right-click for Open / Open Containing Folder.
                - Export CSV / Export TXT saves current search results; Clear Results empties the table.

                Duplicate Finder:
                - Compares files by size, partial head/tail hash, or full streaming MD5 / SHA-256.
                - Groups identical files and reports reclaimable space.
                - At least one copy in each duplicate group must remain unselected before deletion.
                - Use Cancel at any time to stop a running scan or duplicate analysis cleanly.
                """;

        JTextArea area = new JTextArea(helpText, 20, 64);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(660, 420));

        JOptionPane.showMessageDialog(
                this,
                scroll,
                "Help - " + AppConfig.APP_NAME,
                JOptionPane.INFORMATION_MESSAGE);
    }

    private MouseAdapter createTablePopupAdapter(JTable table, boolean isSearchTable) {
        return new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    SearchResultItem item = getSelectedItem(table, isSearchTable);
                    if (item != null) {
                        openTarget(item.getPath());
                    }
                }
            }

            private void maybeShowPopup(MouseEvent e) {
                if (!e.isPopupTrigger()) {
                    return;
                }
                Point p = e.getPoint();
                int viewRow = table.rowAtPoint(p);
                if (viewRow >= 0 && !table.isRowSelected(viewRow)) {
                    table.setRowSelectionInterval(viewRow, viewRow);
                }
                SearchResultItem item = getSelectedItem(table, isSearchTable);
                if (item == null) {
                    return;
                }

                JPopupMenu menu = new JPopupMenu();
                JMenuItem openItem = new JMenuItem("Open");
                openItem.addActionListener(ev -> openTarget(item.getPath()));

                JMenuItem openFolderItem = new JMenuItem("Open Containing Folder");
                openFolderItem.addActionListener(ev -> openContainingFolder(item.getPath()));

                menu.add(openItem);
                menu.add(openFolderItem);
                menu.show(table, p.x, p.y);
            }
        };
    }

    private SearchResultItem getSelectedItem(JTable table, boolean isSearchTable) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        List<SearchResultItem> backing = isSearchTable ? currentSearchItems : currentDuplicateRows;
        if (modelRow < 0 || modelRow >= backing.size()) {
            return null;
        }
        return backing.get(modelRow);
    }

    private void loadInitialRoots() {
        Set<String> initial = new LinkedHashSet<>();
        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isBlank()) {
            Path homePath = Path.of(userHome);
            if (Files.isDirectory(homePath)) {
                initial.add(homePath.toAbsolutePath().toString());
            }
        }
        for (File root : File.listRoots()) {
            if (root != null && root.exists()) {
                initial.add(root.getAbsolutePath());
            }
        }
        for (String path : initial) {
            rootsListModel.addElement(path);
        }
        if (!rootsListModel.isEmpty()) {
            rootsList.setSelectedIndex(0);
        }
    }

    private void attachLogListener() {
        for (String existing : logManager.getLogEntriesSnapshot()) {
            logArea.append(existing + System.lineSeparator());
        }
        logManager.addLogListener(line -> {
            logArea.append(line + System.lineSeparator());
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void chooseAndAddRoot() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Search Root Folder");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            String chosen = chooser.getSelectedFile().toPath().toAbsolutePath().toString();
            for (int i = 0; i < rootsListModel.size(); i++) {
                if (rootsListModel.get(i).equals(chosen)) {
                    rootsList.setSelectedIndex(i);
                    return;
                }
            }
            rootsListModel.addElement(chosen);
            rootsList.setSelectedIndex(rootsListModel.size() - 1);
        }
    }

    private List<Path> getSelectedOrAllRoots() {
        List<Path> selected = new ArrayList<>();
        for (String val : rootsList.getSelectedValuesList()) {
            selected.add(Path.of(val));
        }
        if (!selected.isEmpty()) {
            return selected;
        }
        List<Path> all = new ArrayList<>();
        for (int i = 0; i < rootsListModel.size(); i++) {
            all.add(Path.of(rootsListModel.get(i)));
        }
        return all;
    }

    private void startSearch() {
        if (activeWorker != null && !activeWorker.isDone()) {
            JOptionPane.showMessageDialog(this, "A scan is already running.", "Busy", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        List<Path> roots = getSelectedOrAllRoots();
        if (roots.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add at least one search root folder first.", "No Roots", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long minBytes;
        Long maxBytes;
        try {
            minBytes = parseSizeToBytes(minSizeField.getText());
            maxBytes = parseSizeToBytes(maxSizeField.getText());
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid Size Filter", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SearchCriteria criteria;
        try {
            criteria = new SearchCriteria.Builder()
                    .roots(roots)
                    .nameQuery(namePatternField.getText())
                    .matchMode((MatchMode) matchModeCombo.getSelectedItem())
                    .target((SearchTarget) targetCombo.getSelectedItem())
                    .extensionsCsv(extensionsField.getText())
                    .minSizeBytes(minBytes)
                    .maxSizeBytes(maxBytes)
                    .caseSensitive(caseSensitiveCheck.isSelected())
                    .includeHidden(includeHiddenCheck.isSelected())
                    .skipSymbolicLinks(skipSymlinksCheck.isSelected())
                    .build();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid Search Criteria", JOptionPane.ERROR_MESSAGE);
            return;
        }

        clearSearchResults();
        setBusyState(true, true);
        progressBar.setIndeterminate(true);
        progressBar.setString("Scanning...");
        statusLabel.setText("Searching...");

        final boolean enforceLimit = enforceLimitCheck.isSelected();
        final int maxResults = config.getMaxDisplayedResults();
        final AtomicBoolean cancelFlag = activeCancelFlag;

        SwingWorker<FileSystemScannerService.ScanSummary, SearchResultItem> worker =
                new SwingWorker<>() {
                    @Override
                    protected FileSystemScannerService.ScanSummary doInBackground() {
                        return scannerService.scan(
                                criteria,
                                maxResults,
                                enforceLimit,
                                () -> cancelFlag.get() || isCancelled(),
                                this::publish,
                                prog -> SwingUtilities.invokeLater(() -> {
                                    if (!cancelFlag.get() && !isCancelled()) {
                                        statusLabel.setText("Scanned: " + prog.scannedEntries()
                                                + " | Matches: " + prog.matchedEntries()
                                                + (prog.currentPath().isEmpty() ? "" : " | " + prog.currentPath()));
                                    }
                                }));
                    }

                    @Override
                    protected void process(List<SearchResultItem> chunks) {
                        if (cancelFlag.get() || isCancelled()) {
                            return;
                        }
                        for (SearchResultItem item : chunks) {
                            currentSearchItems.add(item);
                            searchTableModel.addRow(new Object[]{
                                item.getName(),
                                item.getTypeLabel(),
                                item.getExtension(),
                                item.getFormattedSize(),
                                item.getFormattedLastModified(),
                                item.getFullPath()
                            });
                        }
                    }

                    @Override
                    protected void done() {
                        if (activeWorker == this) {
                            activeWorker = null;
                        }
                        progressBar.setIndeterminate(false);
                        progressBar.setValue(100);
                        progressBar.setString("Done");
                        setBusyState(false, true);
                        try {
                            FileSystemScannerService.ScanSummary summary = get();
                            String msg = "Matches: " + summary.matchedCount()
                                    + " (scanned " + summary.scannedCount() + " entries)"
                                    + (summary.cancelled() ? " [Cancelled]" : "")
                                    + (summary.limitReached() ? " [Display cap reached]" : "");
                            statusLabel.setText(msg);
                            logManager.info("Search completed: " + msg);
                        } catch (CancellationException ex) {
                            progressBar.setValue(0);
                            progressBar.setString("0%");
                            statusLabel.setText("Search cancelled.");
                            logManager.info("Search cancelled.");
                        } catch (Exception ex) {
                            statusLabel.setText("Search failed.");
                            logManager.error("Search worker failed", ex);
                        }
                    }
                };

        activeWorker = worker;
        worker.execute();
    }

    private void startDuplicateScan() {
        if (activeWorker != null && !activeWorker.isDone()) {
            JOptionPane.showMessageDialog(this, "A scan is already running.", "Busy", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        List<Path> roots = getSelectedOrAllRoots();
        if (roots.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add at least one search root folder first.", "No Roots", JOptionPane.WARNING_MESSAGE);
            return;
        }

        currentDuplicateRows.clear();
        duplicateTableModel.setRowCount(0);
        setBusyState(true, false);
        progressBar.setIndeterminate(false);
        progressBar.setValue(0);
        progressBar.setString("0%");
        statusLabel.setText("Scanning for duplicates...");

        HashAlgorithm algorithm = (HashAlgorithm) hashAlgoCombo.getSelectedItem();
        boolean skipSymlinks = skipSymlinksCheck.isSelected();
        boolean includeHidden = includeHiddenCheck.isSelected();
        final AtomicBoolean cancelFlag = activeCancelFlag;

        SwingWorker<List<DuplicateGroup>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<DuplicateGroup> doInBackground() {
                return duplicateService.findDuplicates(
                        roots,
                        algorithm,
                        skipSymlinks,
                        includeHidden,
                        () -> cancelFlag.get() || isCancelled(),
                        prog -> SwingUtilities.invokeLater(() -> {
                            if (!cancelFlag.get() && !isCancelled()) {
                                int pct = 0;
                                if (prog.totalCandidates() > 0) {
                                    pct = (int) Math.min(100L, (prog.processedFiles() * 100L) / prog.totalCandidates());
                                }
                                progressBar.setValue(pct);
                                progressBar.setString(pct + "%");
                                statusLabel.setText(prog.phase() + " (" + prog.processedFiles()
                                        + (prog.totalCandidates() > 0 ? "/" + prog.totalCandidates() : "") + ")"
                                        + (prog.currentFile().isEmpty() ? "" : " - " + prog.currentFile()));
                            }
                        }));
            }

            @Override
            protected void done() {
                if (activeWorker == this) {
                    activeWorker = null;
                }
                setBusyState(false, false);
                try {
                    List<DuplicateGroup> groups = get();
                    long totalReclaimable = 0L;
                    int totalFiles = 0;
                    for (DuplicateGroup group : groups) {
                        totalReclaimable += group.getReclaimableBytes();
                        for (SearchResultItem file : group.getFiles()) {
                            currentDuplicateRows.add(file);
                            totalFiles++;
                            duplicateTableModel.addRow(new Object[]{
                                group.getGroupKey(),
                                file.getName(),
                                file.getFormattedSize(),
                                group.getFormattedReclaimableSpace(),
                                file.getFormattedLastModified(),
                                file.getFullPath()
                            });
                        }
                    }
                    progressBar.setValue(100);
                    progressBar.setString("100%");
                    String summary = "Groups: " + groups.size()
                            + " | Files: " + totalFiles
                            + " | Reclaimable: " + SearchResultItem.formatBytes(totalReclaimable);
                    statusLabel.setText(summary);
                    logManager.info("Duplicate scan completed: " + summary);
                } catch (CancellationException ex) {
                    progressBar.setValue(0);
                    progressBar.setString("0%");
                    statusLabel.setText("Duplicate scan cancelled.");
                    logManager.info("Duplicate scan cancelled.");
                } catch (Exception ex) {
                    statusLabel.setText("Duplicate scan failed.");
                    logManager.error("Duplicate scan worker failed", ex);
                }
            }
        };

        activeWorker = worker;
        worker.execute();
    }

    private void deleteSelectedDuplicates() {
        int[] selectedViewRows = duplicateTable.getSelectedRows();
        if (selectedViewRows == null || selectedViewRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Select one or more files in the duplicates table first.",
                    "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Set<Integer> selectedModelRows = new LinkedHashSet<>();
        for (int viewRow : selectedViewRows) {
            int modelRow = duplicateTable.convertRowIndexToModel(viewRow);
            if (modelRow >= 0 && modelRow < currentDuplicateRows.size()) {
                selectedModelRows.add(modelRow);
            }
        }

        if (selectedModelRows.isEmpty()) {
            return;
        }

        long estimatedReclaimable = 0L;
        for (int idx : selectedModelRows) {
            estimatedReclaimable += Math.max(0L, currentDuplicateRows.get(idx).getSizeBytes());
        }

        List<String> fullGroupDeletions = findGroupsWithoutRemainingCopy(currentDuplicateRows, selectedModelRows);
        if (!fullGroupDeletions.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Safety check: leave at least one copy unselected in each duplicate group.\n"
                            + "All files selected in group(s): " + String.join(", ", fullGroupDeletions),
                    "Keep At Least One Copy",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Path> targets = new ArrayList<>();
        List<Integer> modelRowsToRemove = new ArrayList<>(selectedModelRows);
        for (int modelRow : modelRowsToRemove) {
            targets.add(currentDuplicateRows.get(modelRow).getPath());
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Permanently delete " + targets.size() + " selected file(s) ("
                        + SearchResultItem.formatBytes(estimatedReclaimable) + ")?\n"
                        + "At least one copy per group will be kept. This cannot be undone.",
                "Confirm Permanent Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        FileActionService.BatchOperationResult result = fileActionService.deletePathsPermanently(targets);
        modelRowsToRemove.sort((a, b) -> Integer.compare(b, a));
        for (int rowIdx : modelRowsToRemove) {
            if (rowIdx >= 0 && rowIdx < currentDuplicateRows.size()) {
                Path p = currentDuplicateRows.get(rowIdx).getPath();
                if (!Files.exists(p, LinkOption.NOFOLLOW_LINKS)) {
                    currentDuplicateRows.remove(rowIdx);
                    duplicateTableModel.removeRow(rowIdx);
                }
            }
        }

        String summary = "Deleted " + result.succeeded() + " of " + targets.size() + " selected path(s).";
        statusLabel.setText(summary);
        if (!result.errors().isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    summary + System.lineSeparator() + String.join(System.lineSeparator(), result.errors()),
                    "Deletion Errors",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    public static List<String> findGroupsWithoutRemainingCopy(
            List<SearchResultItem> allRows,
            Set<Integer> selectedRowIndices) {
        if (allRows == null || allRows.isEmpty() || selectedRowIndices == null || selectedRowIndices.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Integer> totalPerGroup = new LinkedHashMap<>();
        Map<String, Integer> selectedPerGroup = new LinkedHashMap<>();

        for (int i = 0; i < allRows.size(); i++) {
            SearchResultItem item = allRows.get(i);
            String key = groupKeyFor(item);
            totalPerGroup.merge(key, 1, Integer::sum);
            if (selectedRowIndices.contains(i)) {
                selectedPerGroup.merge(key, 1, Integer::sum);
            }
        }

        List<String> blockedGroups = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : selectedPerGroup.entrySet()) {
            int totalInGroup = totalPerGroup.getOrDefault(entry.getKey(), 0);
            if (totalInGroup > 0 && entry.getValue() >= totalInGroup) {
                blockedGroups.add(entry.getKey());
            }
        }
        return blockedGroups;
    }

    private static String groupKeyFor(SearchResultItem item) {
        return item.getDigest() != null ? item.getDigest() : ("SIZE:" + item.getSizeBytes());
    }

    private void exportSearchCsv() {
        if (currentSearchItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No search results to export.", "Empty", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Path target = chooseSaveFile("search-results.csv", "CSV Files (*.csv)", "csv");
        if (target == null) {
            return;
        }
        try {
            reportExportService.exportSearchResultsCsv(target, currentSearchItems);
            statusLabel.setText("Exported CSV to: " + target);
        } catch (IOException ex) {
            logManager.error("Failed exporting search results CSV", ex);
            JOptionPane.showMessageDialog(this, "Error exporting CSV: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportSearchTxt() {
        if (currentSearchItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No search results to export.", "Empty", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Path target = chooseSaveFile("search-results.txt", "Text Files (*.txt)", "txt");
        if (target == null) {
            return;
        }
        try {
            reportExportService.exportSearchResultsTxt(target, currentSearchItems);
            statusLabel.setText("Exported TXT to: " + target);
        } catch (IOException ex) {
            logManager.error("Failed exporting search results TXT", ex);
            JOptionPane.showMessageDialog(this, "Error exporting TXT: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportDuplicatesCsv() {
        if (currentDuplicateRows.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No duplicate results to export.", "Empty", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Path target = chooseSaveFile("duplicates.csv", "CSV Files (*.csv)", "csv");
        if (target == null) {
            return;
        }
        try {
            List<DuplicateGroup> groups = rebuildGroupsFromRows(currentDuplicateRows);
            reportExportService.exportDuplicatesCsv(target, groups);
            statusLabel.setText("Exported duplicates CSV to: " + target);
        } catch (IOException ex) {
            logManager.error("Failed exporting duplicates CSV", ex);
            JOptionPane.showMessageDialog(this, "Error exporting CSV: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<DuplicateGroup> rebuildGroupsFromRows(List<SearchResultItem> rows) {
        Map<String, DuplicateGroup> map = new LinkedHashMap<>();
        for (SearchResultItem item : rows) {
            String key = groupKeyFor(item);
            DuplicateGroup group = map.computeIfAbsent(key, k -> new DuplicateGroup(k, item.getSizeBytes()));
            group.addFile(item);
        }
        return new ArrayList<>(map.values());
    }

    private Path chooseSaveFile(String defaultName, String description, String extension) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(defaultName));
        chooser.setFileFilter(new FileNameExtensionFilter(description, extension));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION || chooser.getSelectedFile() == null) {
            return null;
        }
        Path chosen = chooser.getSelectedFile().toPath();
        String fileName = chosen.getFileName().toString();
        if (!fileName.toLowerCase(Locale.ROOT).endsWith("." + extension.toLowerCase(Locale.ROOT))) {
            chosen = chosen.resolveSibling(fileName + "." + extension);
        }
        return chosen;
    }

    private void clearSearchResults() {
        currentSearchItems.clear();
        searchTableModel.setRowCount(0);
        statusLabel.setText("Ready.");
    }

    private void cancelActiveTask() {
        activeCancelFlag.set(true);
        if (activeWorker != null && !activeWorker.isDone()) {
            activeWorker.cancel(false);
        }
        statusLabel.setText("Cancelling...");
    }

    private void setBusyState(boolean busy, boolean isSearchTab) {
        if (busy) {
            activeCancelFlag.set(false);
        }
        startSearchBtn.setEnabled(!busy);
        startDupBtn.setEnabled(!busy);
        deleteSelectedDupBtn.setEnabled(!busy);
        exportCsvBtn.setEnabled(!busy);
        exportTxtBtn.setEnabled(!busy);
        exportDupCsvBtn.setEnabled(!busy);
        clearSearchBtn.setEnabled(!busy);

        if (isSearchTab) {
            cancelSearchBtn.setEnabled(busy);
            cancelDupBtn.setEnabled(false);
        } else {
            cancelDupBtn.setEnabled(busy);
            cancelSearchBtn.setEnabled(false);
        }

        applyTheme(this.darkMode, false);
    }

    private void openTarget(Path path) {
        try {
            fileActionService.openFile(path);
        } catch (IOException ex) {
            logManager.error("Could not open target: " + path, ex);
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Open Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openContainingFolder(Path path) {
        try {
            fileActionService.openContainingFolder(path);
        } catch (IOException ex) {
            logManager.error("Could not open containing folder for: " + path, ex);
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Folder Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void configureTableColumnWidths(JTable table, int[] widths) {
        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < widths.length && i < columnModel.getColumnCount(); i++) {
            columnModel.getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    private static Long parseSizeToBytes(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim().toUpperCase(Locale.ROOT);
        long multiplier = 1L;
        String numPart = trimmed;

        if (trimmed.endsWith("KB")) {
            multiplier = 1024L;
            numPart = trimmed.substring(0, trimmed.length() - 2).trim();
        } else if (trimmed.endsWith("MB")) {
            multiplier = 1024L * 1024L;
            numPart = trimmed.substring(0, trimmed.length() - 2).trim();
        } else if (trimmed.endsWith("GB")) {
            multiplier = 1024L * 1024L * 1024L;
            numPart = trimmed.substring(0, trimmed.length() - 2).trim();
        } else if (trimmed.endsWith("TB")) {
            multiplier = 1024L * 1024L * 1024L * 1024L;
            numPart = trimmed.substring(0, trimmed.length() - 2).trim();
        } else if (trimmed.endsWith("B")) {
            numPart = trimmed.substring(0, trimmed.length() - 1).trim();
        }

        double parsed;
        try {
            parsed = Double.parseDouble(numPart);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid size value: '" + raw + "' (e.g. 500B, 10KB, 5MB, 1GB)");
        }

        if (Double.isNaN(parsed) || Double.isInfinite(parsed) || parsed < 0) {
            throw new IllegalArgumentException("Size filter cannot be negative: '" + raw + "'");
        }
        return Math.round(parsed * multiplier);
    }
}