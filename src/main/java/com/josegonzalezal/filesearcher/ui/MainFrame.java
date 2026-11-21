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
import java.awt.CardLayout;
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
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
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
import javax.swing.plaf.basic.BasicToggleButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

public final class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String GITHUB_URL = "https://github.com/josegonzalezal/";

    private static final String MODE_SEARCH = "Find Files / Folders";
    private static final String MODE_DUPLICATES = "Find Duplicates";

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

    private final JComboBox<String> modeCombo = new JComboBox<>(new String[]{MODE_SEARCH, MODE_DUPLICATES});

    private final DefaultListModel<String> rootsListModel = new DefaultListModel<>();
    private final JList<String> rootsList = new JList<>(rootsListModel);

    private final JTextField namePatternField = new JTextField(22);
    private final JComboBox<MatchMode> matchModeCombo = new JComboBox<>(MatchMode.values());
    private final JToggleButton literalNameBtn = new JToggleButton("Literal: OFF");
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

    private final JComboBox<HashAlgorithm> hashAlgoCombo = new JComboBox<>(HashAlgorithm.values());
    private final JButton startDupBtn = new JButton("Find Duplicates");
    private final JButton cancelDupBtn = new JButton("Cancel");
    private final JButton selectDupCopiesBtn = new JButton("Select Duplicates");
    private final JButton deleteSelectedDupBtn = new JButton("Delete Selected");

    private final CardLayout runControlsLayout = new CardLayout();
    private final JPanel runControlsPanel = new JPanel(runControlsLayout);

    private final CardLayout tableCardLayout = new CardLayout();
    private final JPanel tableCardPanel = new JPanel(tableCardLayout);

    private final CardLayout actionBarLayout = new CardLayout();
    private final JPanel actionBarPanel = new JPanel(actionBarLayout);

    private final JToggleButton darkModeToggleBtn = new JToggleButton("Dark Mode");
    private final JButton aboutBtn = new JButton("About");
    private final JButton helpBtn = new JButton("Help");
    private final JButton openFileBtn = new JButton("Open File");
    private final JButton openFolderBtn = new JButton("Open Folder");
    private final JButton copyPathBtn = new JButton("Copy Path");
    private final JButton exportCsvBtn = new JButton("Export CSV");
    private final JButton exportTxtBtn = new JButton("Export TXT");
    private final JButton clearResultsBtn = new JButton("Clear Results");

    private final List<SearchResultItem> currentSearchItems = new ArrayList<>();
    private final DefaultTableModel searchTableModel = new DefaultTableModel(SEARCH_COLUMNS, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable searchTable = new JTable(searchTableModel);

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
    private final JButton exportLogBtn = new JButton("Export Log");
    private final JButton clearLogBtn = new JButton("Clear Log");
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
        syncModeUI();
        syncLiteralModeState();
        applyTheme(this.darkMode, false);

        logManager.info("Application initialized (Java " + System.getProperty("java.version") + ").");
    }

    private boolean isDuplicatesMode() {
        return MODE_DUPLICATES.equals(modeCombo.getSelectedItem());
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));

        JPanel workspace = new JPanel(new BorderLayout(8, 8));
        workspace.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.add(buildRootsPanel(), BorderLayout.WEST);
        topPanel.add(buildCriteriaPanel(), BorderLayout.CENTER);

        searchTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        searchTable.setAutoCreateRowSorter(true);
        configureTableColumnWidths(searchTable, new int[]{220, 85, 70, 95, 145, 450});

        duplicateTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        duplicateTable.setAutoCreateRowSorter(true);
        configureTableColumnWidths(duplicateTable, new int[]{240, 220, 95, 100, 145, 420});

        tableCardPanel.add(new JScrollPane(searchTable), MODE_SEARCH);
        tableCardPanel.add(new JScrollPane(duplicateTable), MODE_DUPLICATES);

        JPanel centerPanel = new JPanel(new BorderLayout(6, 6));
        centerPanel.add(tableCardPanel, BorderLayout.CENTER);
        centerPanel.add(buildActionsBar(), BorderLayout.SOUTH);

        workspace.add(topPanel, BorderLayout.NORTH);
        workspace.add(centerPanel, BorderLayout.CENTER);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, workspace, buildBottomPanel());
        mainSplit.setResizeWeight(0.76);
        mainSplit.setOneTouchExpandable(true);

        add(mainSplit, BorderLayout.CENTER);
    }

    private JPanel buildRootsPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Search Roots"));
        panel.setPreferredSize(new Dimension(280, 225));

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

    private JPanel buildCriteriaPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Search & Filter Options"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;

        namePatternField.setToolTipText("Name or pattern (e.g. report, *.pdf, ^IMG_\\d+)");
        literalNameBtn.setToolTipText("Skip patterns and match literal text");
        extensionsField.setToolTipText("Comma-separated extensions without dot, e.g. pdf, jpg, txt");
        minSizeField.setToolTipText("e.g. 500B, 10KB, 5MB, 1GB");
        maxSizeField.setToolTipText("e.g. 50MB, 2GB");

        gbc.gridx = 0; gbc.gridy = 0; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Mode:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.4;
        panel.add(modeCombo, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Target:"), gbc);
        gbc.gridx = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.4;
        panel.add(targetCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Name / Pattern:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(namePatternField, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Match:"), gbc);

        JPanel matchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        matchModeCombo.setPreferredSize(new Dimension(165, 26));
        matchRow.add(matchModeCombo);
        matchRow.add(literalNameBtn);
        gbc.gridx = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(matchRow, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Min Size:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5;
        panel.add(minSizeField, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Max Size:"), gbc);
        gbc.gridx = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5;
        panel.add(maxSizeField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Extensions:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(extensionsField, gbc);

        JPanel flagsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 2));
        flagsPanel.add(caseSensitiveCheck);
        flagsPanel.add(includeHiddenCheck);
        flagsPanel.add(skipSymlinksCheck);
        flagsPanel.add(enforceLimitCheck);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(flagsPanel, gbc);

        JPanel searchRunBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        cancelSearchBtn.setEnabled(false);
        searchRunBar.add(startSearchBtn);
        searchRunBar.add(cancelSearchBtn);

        JPanel dupRunBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        cancelDupBtn.setEnabled(false);
        dupRunBar.add(new JLabel("Algorithm:"));
        dupRunBar.add(hashAlgoCombo);
        dupRunBar.add(startDupBtn);
        dupRunBar.add(cancelDupBtn);
        dupRunBar.add(selectDupCopiesBtn);
        dupRunBar.add(deleteSelectedDupBtn);

        runControlsPanel.add(searchRunBar, MODE_SEARCH);
        runControlsPanel.add(dupRunBar, MODE_DUPLICATES);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(runControlsPanel, gbc);

        return panel;
    }

    private JPanel buildActionsBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        bar.add(darkModeToggleBtn);
        bar.add(aboutBtn);
        bar.add(helpBtn);
        bar.add(openFileBtn);
        bar.add(openFolderBtn);
        bar.add(copyPathBtn);
        bar.add(exportCsvBtn);
        bar.add(exportTxtBtn);
        bar.add(clearResultsBtn);

        actionBarPanel.add(bar, "MAIN");
        return actionBarPanel;
    }

    private JPanel buildBottomPanel() {
        JPanel bottom = new JPanel(new BorderLayout(6, 6));
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

        JPanel statusBar = new JPanel(new BorderLayout(8, 0));
        progressBar.setPreferredSize(new Dimension(220, 18));
        progressBar.setStringPainted(true);
        statusBar.add(statusLabel, BorderLayout.CENTER);
        statusBar.add(progressBar, BorderLayout.EAST);

        JPanel logHeaderBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        logHeaderBar.add(exportLogBtn);
        logHeaderBar.add(clearLogBtn);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);

        JPanel logPanel = new JPanel(new BorderLayout(4, 4));
        logPanel.setBorder(BorderFactory.createTitledBorder("Logs"));
        logPanel.add(logHeaderBar, BorderLayout.NORTH);
        logPanel.add(logScroll, BorderLayout.CENTER);

        bottom.add(statusBar, BorderLayout.NORTH);
        bottom.add(logPanel, BorderLayout.CENTER);
        return bottom;
    }

    private void syncModeUI() {
        String selectedMode = isDuplicatesMode() ? MODE_DUPLICATES : MODE_SEARCH;
        runControlsLayout.show(runControlsPanel, selectedMode);
        tableCardLayout.show(tableCardPanel, selectedMode);
        actionBarLayout.show(actionBarPanel, "MAIN");
        statusLabel.setText("Ready (" + selectedMode + ").");
    }

    private void syncLiteralModeState() {
        boolean literal = literalNameBtn.isSelected();
        literalNameBtn.setText(literal ? "Literal: ON" : "Literal: OFF");
        matchModeCombo.setEnabled(!literal && (activeWorker == null || activeWorker.isDone()));
        namePatternField.setToolTipText(literal
                ? "Literal mode ON: wildcards (*, ?) and regex characters are ignored"
                : "Name or pattern (e.g. report, *.pdf, ^IMG_\\d+)");
        applyTheme(this.darkMode, false);
    }

    private void wireEvents() {
        modeCombo.addActionListener(e -> syncModeUI());
        literalNameBtn.addActionListener(e -> syncLiteralModeState());

        startSearchBtn.addActionListener(e -> startSearch());
        cancelSearchBtn.addActionListener(e -> cancelActiveTask());

        startDupBtn.addActionListener(e -> startDuplicateScan());
        cancelDupBtn.addActionListener(e -> cancelActiveTask());
        selectDupCopiesBtn.addActionListener(e -> selectDuplicateCopiesExceptFirst());
        deleteSelectedDupBtn.addActionListener(e -> deleteSelectedDuplicates());

        darkModeToggleBtn.addActionListener(e -> toggleDarkMode());
        aboutBtn.addActionListener(e -> showAboutDialog());
        helpBtn.addActionListener(e -> showHelpDialog());
        openFileBtn.addActionListener(e -> openSelectedItemFile());
        openFolderBtn.addActionListener(e -> openSelectedItemFolder());
        copyPathBtn.addActionListener(e -> copySelectedPathsToClipboard());
        exportCsvBtn.addActionListener(e -> exportCsvForCurrentMode());
        exportTxtBtn.addActionListener(e -> exportTxtForCurrentMode());
        clearResultsBtn.addActionListener(e -> clearResultsForCurrentMode());

        exportLogBtn.addActionListener(e -> exportLogs());
        clearLogBtn.addActionListener(e -> clearLogScreen());

        searchTable.addMouseListener(createTablePopupAdapter(searchTable, false));
        duplicateTable.addMouseListener(createTablePopupAdapter(duplicateTable, true));
    }

    private JTable activeTable() {
        return isDuplicatesMode() ? duplicateTable : searchTable;
    }

    private List<SearchResultItem> activeBackingList() {
        return isDuplicatesMode() ? currentDuplicateRows : currentSearchItems;
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
            styleToggleButton(toggle, palette);
        } else if (comp instanceof JButton btn) {
            styleButton(btn, palette);
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

    private void styleToggleButton(JToggleButton toggle, ThemePalette palette) {
        Color disabledFg = darkMode ? new Color(148, 163, 184) : new Color(120, 128, 140);
        boolean active = toggle.isSelected();
        Color bg = active ? palette.selectionBg() : palette.elevatedBg();
        Color fg = !toggle.isEnabled() ? disabledFg : (active ? palette.selectionFg() : palette.textPrimary());
        Color borderColor = active ? palette.selectionBg() : palette.borderColor();

        toggle.setUI(new BasicToggleButtonUI() {
            @Override
            protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
                FontMetrics fm = g.getFontMetrics(b.getFont());
                g.setColor(!b.isEnabled() ? disabledFg : (b.isSelected() ? palette.selectionFg() : palette.textPrimary()));
                g.drawString(text, textRect.x, textRect.y + fm.getAscent());
            }
        });
        toggle.setOpaque(true);
        toggle.setBackground(bg);
        toggle.setForeground(fg);
        toggle.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
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

        JLabel titleLabel = new JLabel(AppConfig.APP_NAME + " v1.0.5");
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
        ThemePalette palette = ThemePalette.fromDarkMode(darkMode);

        String helpText = """
                FILE SEARCHER - QUICK REFERENCE

                1. MODES
                   - Find Files / Folders: Search directories by name, pattern, type, size, or extension.
                   - Find Duplicates: Group matching files by hash (SHA-256, SHA-1, MD5) and filter
                     results using the active criteria before inspecting or cleaning up copies.

                2. SEARCH ROOTS
                   - Add Folder... / Remove: Manage the list of target directories to scan.
                   - Root selection: Highlight one or more roots to scan only those, or leave none
                     selected to scan all listed roots.

                3. NAME MATCHING & LITERAL MODE
                   - Literal: OFF: Uses the selected Match mode (Name contains, Exact name, Starts with,
                     Ends with, Wildcard (*, ?), or Regular expression) and trims surrounding spaces.
                   - Literal: ON: Disables pattern matching and searches the exact text entered in
                     Name / Pattern (preserving leading or trailing spaces).
                   - Case sensitive: Applies case sensitivity to name and pattern checks.

                4. TARGET, SIZE, EXTENSIONS & FLAGS
                   - Target: Filter by Files, Folders, or Both (in standard search).
                   - Min Size / Max Size: Supports units like B, KB, MB, GB, and TB (e.g., 500B, 10KB, 5MB).
                   - Extensions: Comma-separated list without leading dots (e.g., pdf, jpg, txt).
                   - Include hidden / Skip symlinks: Control whether hidden entries or symbolic links
                     are traversed or included.
                   - Cap results: Limits displayed rows to the configured display cap for responsiveness.

                5. RESULTS, DUPLICATES & EXPORT
                   - Open / Open Folder / Copy Path: Available via action buttons, double-click, or
                     right-click context menu (supports multi-row path copy).
                   - Select Duplicates: In Duplicate mode, selects extra copies while leaving the first
                     entry of each group unselected.
                   - Delete Selected: Permanently deletes selected duplicate files only after verifying
                     at least one copy in every affected group remains unselected.
                   - Export CSV / Export TXT / Clear Results: Export or clear the active mode's table.

                6. STATUS & LOGS
                   - Progress & status bar: Shows scan phase, counts, cancellation state, or display cap.
                   - Export Log / Clear Log: Save the log buffer to .log/.txt or clear the log view.
                """;

        JTextArea area = new JTextArea(helpText, 26, 74);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setBackground(palette.inputBg());
        area.setForeground(palette.textPrimary());
        area.setCaretColor(palette.textPrimary());
        area.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(740, 500));
        scroll.setBorder(BorderFactory.createLineBorder(palette.borderColor(), 1));
        scroll.getViewport().setBackground(palette.inputBg());

        JOptionPane.showMessageDialog(
                this,
                scroll,
                "Help - " + AppConfig.APP_NAME,
                JOptionPane.INFORMATION_MESSAGE);
    }

    private MouseAdapter createTablePopupAdapter(JTable table, boolean isDuplicatesTable) {
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
                    SearchResultItem item = getSelectedItem(table, isDuplicatesTable);
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
                SearchResultItem item = getSelectedItem(table, isDuplicatesTable);
                if (item == null) {
                    return;
                }

                JPopupMenu menu = new JPopupMenu();
                JMenuItem openItem = new JMenuItem("Open");
                openItem.addActionListener(ev -> openTarget(item.getPath()));

                JMenuItem openFolderItem = new JMenuItem("Open Containing Folder");
                openFolderItem.addActionListener(ev -> openContainingFolder(item.getPath()));

                JMenuItem copyPathItem = new JMenuItem("Copy Path");
                copyPathItem.addActionListener(ev -> copySelectedPathsToClipboard());

                menu.add(openItem);
                menu.add(openFolderItem);
                menu.add(copyPathItem);
                menu.show(table, p.x, p.y);
            }
        };
    }

    private SearchResultItem getSelectedItem(JTable table, boolean isDuplicatesTable) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        List<SearchResultItem> backing = isDuplicatesTable ? currentDuplicateRows : currentSearchItems;
        if (modelRow < 0 || modelRow >= backing.size()) {
            return null;
        }
        return backing.get(modelRow);
    }

    private void openSelectedItemFile() {
        SearchResultItem item = getSelectedItem(activeTable(), isDuplicatesMode());
        if (item == null) {
            JOptionPane.showMessageDialog(this, "Select a row first.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        openTarget(item.getPath());
    }

    private void openSelectedItemFolder() {
        SearchResultItem item = getSelectedItem(activeTable(), isDuplicatesMode());
        if (item == null) {
            JOptionPane.showMessageDialog(this, "Select a row first.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        openContainingFolder(item.getPath());
    }

    private void copySelectedPathsToClipboard() {
        JTable table = activeTable();
        List<SearchResultItem> backing = activeBackingList();
        int[] selectedViewRows = table.getSelectedRows();
        if (selectedViewRows == null || selectedViewRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Select one or more rows first.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        List<String> paths = new ArrayList<>();
        for (int viewRow : selectedViewRows) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            if (modelRow >= 0 && modelRow < backing.size()) {
                paths.add(backing.get(modelRow).getFullPath());
            }
        }
        if (paths.isEmpty()) {
            return;
        }

        String joined = String.join(System.lineSeparator(), paths);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(joined), null);
        statusLabel.setText("Copied " + paths.size() + " path(s) to clipboard.");
    }

    private void selectDuplicateCopiesExceptFirst() {
        if (currentDuplicateRows.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No duplicate results to select.", "Empty", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Set<String> seenGroups = new LinkedHashSet<>();
        Set<Integer> modelRowsToSelect = new LinkedHashSet<>();

        for (int i = 0; i < currentDuplicateRows.size(); i++) {
            String key = groupKeyFor(currentDuplicateRows.get(i));
            if (!seenGroups.contains(key)) {
                seenGroups.add(key);
            } else {
                modelRowsToSelect.add(i);
            }
        }

        ListSelectionModel selModel = duplicateTable.getSelectionModel();
        selModel.setValueIsAdjusting(true);
        selModel.clearSelection();
        for (int modelRow : modelRowsToSelect) {
            int viewRow = duplicateTable.convertRowIndexToView(modelRow);
            if (viewRow >= 0) {
                selModel.addSelectionInterval(viewRow, viewRow);
            }
        }
        selModel.setValueIsAdjusting(false);
        statusLabel.setText("Selected " + modelRowsToSelect.size() + " duplicate copy/copies.");
    }

    private void exportCsvForCurrentMode() {
        if (isDuplicatesMode()) {
            exportDuplicatesCsv();
        } else {
            exportSearchCsv();
        }
    }

    private void exportTxtForCurrentMode() {
        if (isDuplicatesMode()) {
            exportDuplicatesTxt();
        } else {
            exportSearchTxt();
        }
    }

    private void clearResultsForCurrentMode() {
        if (isDuplicatesMode()) {
            currentDuplicateRows.clear();
            duplicateTableModel.setRowCount(0);
        } else {
            currentSearchItems.clear();
            searchTableModel.setRowCount(0);
        }
        statusLabel.setText("Ready.");
    }

    private void exportLogs() {
        String content = logArea.getText();
        if (content == null || content.isBlank()) {
            List<String> snapshot = logManager.getLogEntriesSnapshot();
            content = snapshot.isEmpty() ? "" : String.join(System.lineSeparator(), snapshot) + System.lineSeparator();
        }
        if (content.isBlank()) {
            JOptionPane.showMessageDialog(this, "No log entries to export.", "Empty Log", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("file-searcher.log"));
        chooser.setFileFilter(new FileNameExtensionFilter("Log & Text Files (*.log, *.txt)", "log", "txt"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION || chooser.getSelectedFile() == null) {
            return;
        }

        Path chosen = chooser.getSelectedFile().toPath();
        String fileName = chosen.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!fileName.endsWith(".log") && !fileName.endsWith(".txt")) {
            chosen = chosen.resolveSibling(chosen.getFileName().toString() + ".log");
        }

        try {
            Files.writeString(chosen, content, StandardCharsets.UTF_8);
            statusLabel.setText("Exported log to: " + chosen);
            logManager.info("Log exported to: " + chosen);
        } catch (IOException ex) {
            logManager.error("Failed exporting log", ex);
            JOptionPane.showMessageDialog(this, "Error exporting log: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearLogScreen() {
        logArea.setText("");
        statusLabel.setText("Log screen cleared.");
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

    private SearchCriteria buildCriteriaFromInputs(List<Path> roots) {
        Long minBytes = parseSizeToBytes(minSizeField.getText());
        Long maxBytes = parseSizeToBytes(maxSizeField.getText());
        return new SearchCriteria.Builder()
                .roots(roots)
                .nameQuery(namePatternField.getText())
                .matchMode((MatchMode) matchModeCombo.getSelectedItem())
                .literalName(literalNameBtn.isSelected())
                .target((SearchTarget) targetCombo.getSelectedItem())
                .extensionsCsv(extensionsField.getText())
                .minSizeBytes(minBytes)
                .maxSizeBytes(maxBytes)
                .caseSensitive(caseSensitiveCheck.isSelected())
                .includeHidden(includeHiddenCheck.isSelected())
                .skipSymbolicLinks(skipSymlinksCheck.isSelected())
                .build();
    }

    private Set<String> parseExtensionsInput(String rawCsv) {
        if (rawCsv == null || rawCsv.isBlank()) {
            return Set.of();
        }
        Set<String> result = new HashSet<>();
        for (String part : rawCsv.split(",")) {
            String clean = part.trim().toLowerCase(Locale.ROOT);
            if (clean.startsWith(".")) {
                clean = clean.substring(1);
            }
            if (!clean.isEmpty()) {
                result.add(clean);
            }
        }
        return result;
    }

    private boolean matchesDuplicateFilters(
            SearchResultItem item,
            Long minBytes,
            Long maxBytes,
            Set<String> extensions,
            String nameQuery,
            Pattern compiledPattern,
            MatchMode matchMode,
            boolean caseSensitive,
            boolean literalName) {
        if (minBytes != null && item.getSizeBytes() < minBytes) {
            return false;
        }
        if (maxBytes != null && item.getSizeBytes() > maxBytes) {
            return false;
        }
        if (extensions != null && !extensions.isEmpty()) {
            String ext = item.getExtension() == null ? "" : item.getExtension().toLowerCase(Locale.ROOT);
            if (ext.startsWith(".")) {
                ext = ext.substring(1);
            }
            if (!extensions.contains(ext)) {
                return false;
            }
        }
        if (nameQuery != null && !nameQuery.isEmpty()) {
            String candidate = item.getName() == null ? "" : item.getName();
            if (literalName) {
                String query = caseSensitive ? nameQuery : nameQuery.toLowerCase(Locale.ROOT);
                String targetName = caseSensitive ? candidate : candidate.toLowerCase(Locale.ROOT);
                return targetName.contains(query);
            }
            String modeName = matchMode == null ? "" : matchMode.name().toUpperCase(Locale.ROOT);
            if (compiledPattern != null) {
                return compiledPattern.matcher(candidate).matches();
            }
            String query = caseSensitive ? nameQuery : nameQuery.toLowerCase(Locale.ROOT);
            String targetName = caseSensitive ? candidate : candidate.toLowerCase(Locale.ROOT);
            if (modeName.contains("EXACT") || modeName.contains("EQUALS")) {
                return targetName.equals(query);
            }
            if (modeName.contains("START") || modeName.contains("PREFIX")) {
                return targetName.startsWith(query);
            }
            if (modeName.contains("END") || modeName.contains("SUFFIX")) {
                return targetName.endsWith(query);
            }
            return targetName.contains(query);
        }
        return true;
    }

    private static String wildcardToRegex(String pattern) {
        StringBuilder sb = new StringBuilder("^");
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '*') {
                sb.append(".*");
            } else if (c == '?') {
                sb.append('.');
            } else {
                sb.append(Pattern.quote(String.valueOf(c)));
            }
        }
        sb.append("$");
        return sb.toString();
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

        SearchCriteria criteria;
        try {
            criteria = buildCriteriaFromInputs(roots);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid Search Criteria", JOptionPane.ERROR_MESSAGE);
            return;
        }

        currentSearchItems.clear();
        searchTableModel.setRowCount(0);
        setBusyState(true, false);
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
                        setBusyState(false, false);
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

        final Long minBytes;
        final Long maxBytes;
        final boolean literalName = literalNameBtn.isSelected();
        final boolean caseSensitive = caseSensitiveCheck.isSelected();
        final MatchMode matchMode = (MatchMode) matchModeCombo.getSelectedItem();
        final String rawQuery = namePatternField.getText();
        final String effectiveNameQuery;
        final Pattern compiledPattern;

        try {
            minBytes = parseSizeToBytes(minSizeField.getText());
            maxBytes = parseSizeToBytes(maxSizeField.getText());
            buildCriteriaFromInputs(roots);

            String base = rawQuery == null ? "" : rawQuery;
            effectiveNameQuery = literalName ? base : base.trim();
            String modeName = matchMode == null ? "" : matchMode.name().toUpperCase(Locale.ROOT);
            if (!literalName && !effectiveNameQuery.isEmpty()) {
                int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
                if (modeName.contains("REGEX")) {
                    compiledPattern = Pattern.compile(effectiveNameQuery, flags);
                } else if (modeName.contains("WILDCARD") || modeName.contains("GLOB")) {
                    compiledPattern = Pattern.compile(wildcardToRegex(effectiveNameQuery), flags);
                } else {
                    compiledPattern = null;
                }
            } else {
                compiledPattern = null;
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid Search Criteria", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final Set<String> extensions = parseExtensionsInput(extensionsField.getText());
        final HashAlgorithm algorithm = (HashAlgorithm) hashAlgoCombo.getSelectedItem();
        final boolean skipSymlinks = skipSymlinksCheck.isSelected();
        final boolean includeHidden = includeHiddenCheck.isSelected();
        final boolean enforceLimit = enforceLimitCheck.isSelected();
        final int maxDisplayed = config.getMaxDisplayedResults();
        final AtomicBoolean cancelFlag = activeCancelFlag;

        currentDuplicateRows.clear();
        duplicateTableModel.setRowCount(0);
        setBusyState(true, true);
        progressBar.setIndeterminate(false);
        progressBar.setValue(0);
        progressBar.setString("0%");
        statusLabel.setText("Scanning for duplicates...");

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
                setBusyState(false, true);
                try {
                    List<DuplicateGroup> rawGroups = get();
                    List<DuplicateGroup> filteredGroups = new ArrayList<>();
                    for (DuplicateGroup group : rawGroups) {
                        List<SearchResultItem> matchingFiles = new ArrayList<>();
                        for (SearchResultItem file : group.getFiles()) {
                            if (matchesDuplicateFilters(
                                    file,
                                    minBytes,
                                    maxBytes,
                                    extensions,
                                    effectiveNameQuery,
                                    compiledPattern,
                                    matchMode,
                                    caseSensitive,
                                    literalName)) {
                                matchingFiles.add(file);
                            }
                        }
                        if (matchingFiles.size() >= 2) {
                            long unitSize = matchingFiles.get(0).getSizeBytes();
                            DuplicateGroup rebuilt = new DuplicateGroup(group.getGroupKey(), unitSize);
                            for (SearchResultItem f : matchingFiles) {
                                rebuilt.addFile(f);
                            }
                            filteredGroups.add(rebuilt);
                        }
                    }

                    long totalReclaimable = 0L;
                    int totalFiles = 0;
                    boolean capped = false;
                    for (DuplicateGroup group : filteredGroups) {
                        totalReclaimable += group.getReclaimableBytes();
                        for (SearchResultItem file : group.getFiles()) {
                            if (enforceLimit && totalFiles >= maxDisplayed) {
                                capped = true;
                                break;
                            }
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
                        if (capped) {
                            break;
                        }
                    }
                    progressBar.setValue(100);
                    progressBar.setString("100%");
                    String summary = "Groups: " + filteredGroups.size()
                            + " | Files: " + totalFiles
                            + " | Reclaimable: " + SearchResultItem.formatBytes(totalReclaimable)
                            + (capped ? " [Display cap reached]" : "");
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

    private void exportDuplicatesTxt() {
        if (currentDuplicateRows.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No duplicate results to export.", "Empty", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Path target = chooseSaveFile("duplicates.txt", "Text Files (*.txt)", "txt");
        if (target == null) {
            return;
        }
        try {
            List<DuplicateGroup> groups = rebuildGroupsFromRows(currentDuplicateRows);
            StringBuilder sb = new StringBuilder();
            for (DuplicateGroup group : groups) {
                long unitSize = group.getFiles().isEmpty() ? 0L : group.getFiles().get(0).getSizeBytes();
                sb.append("Group: ").append(group.getGroupKey())
                        .append(" | Size: ").append(SearchResultItem.formatBytes(unitSize))
                        .append(" | Reclaimable: ").append(group.getFormattedReclaimableSpace())
                        .append(System.lineSeparator());
                for (SearchResultItem file : group.getFiles()) {
                    sb.append("  - ").append(file.getFullPath()).append(System.lineSeparator());
                }
                sb.append(System.lineSeparator());
            }
            Files.writeString(target, sb.toString(), StandardCharsets.UTF_8);
            statusLabel.setText("Exported duplicates TXT to: " + target);
        } catch (IOException ex) {
            logManager.error("Failed exporting duplicates TXT", ex);
            JOptionPane.showMessageDialog(this, "Error exporting TXT: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
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

    private void cancelActiveTask() {
        activeCancelFlag.set(true);
        if (activeWorker != null && !activeWorker.isDone()) {
            activeWorker.cancel(false);
        }
        statusLabel.setText("Cancelling...");
    }

    private void setBusyState(boolean busy, boolean isDupScan) {
        if (busy) {
            activeCancelFlag.set(false);
        }
        modeCombo.setEnabled(!busy);
        literalNameBtn.setEnabled(!busy);
        matchModeCombo.setEnabled(!busy && !literalNameBtn.isSelected());
        startSearchBtn.setEnabled(!busy);
        startDupBtn.setEnabled(!busy);
        selectDupCopiesBtn.setEnabled(!busy);
        deleteSelectedDupBtn.setEnabled(!busy);
        openFileBtn.setEnabled(!busy);
        openFolderBtn.setEnabled(!busy);
        copyPathBtn.setEnabled(!busy);
        exportCsvBtn.setEnabled(!busy);
        exportTxtBtn.setEnabled(!busy);
        clearResultsBtn.setEnabled(!busy);
        exportLogBtn.setEnabled(!busy);
        clearLogBtn.setEnabled(!busy);

        cancelSearchBtn.setEnabled(busy && !isDupScan);
        cancelDupBtn.setEnabled(busy && isDupScan);

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