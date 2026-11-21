package com.josegonzalezal.filesearcher;

import com.josegonzalezal.filesearcher.config.AppConfig;
import com.josegonzalezal.filesearcher.config.LogManager;
import com.josegonzalezal.filesearcher.ui.MainFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class MainApp {

    private MainApp() {
    }

    public static void main(String[] args) {
        AppConfig config = new AppConfig();
        LogManager logManager = new LogManager(config);

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                logManager.warn("Could not apply system LookAndFeel: " + ex.getMessage());
            }

            MainFrame frame = new MainFrame(config, logManager);
            frame.setVisible(true);
        });
    }
}