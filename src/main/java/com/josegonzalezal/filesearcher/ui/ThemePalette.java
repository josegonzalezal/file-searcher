package com.josegonzalezal.filesearcher.ui;

import java.awt.Color;

public record ThemePalette(
        boolean dark,
        Color windowBg,
        Color panelBg,
        Color elevatedBg,
        Color inputBg,
        Color textPrimary,
        Color textSecondary,
        Color borderColor,
        Color tableBg,
        Color tableAltBg,
        Color tableGrid,
        Color selectionBg,
        Color selectionFg,
        Color linkColor
) {

    public static ThemePalette light() {
        return new ThemePalette(
                false,
                new Color(245, 246, 248),
                new Color(245, 246, 248),
                new Color(255, 255, 255),
                new Color(255, 255, 255),
                new Color(28, 30, 34),
                new Color(90, 96, 106),
                new Color(205, 210, 218),
                new Color(255, 255, 255),
                new Color(248, 250, 252),
                new Color(228, 232, 238),
                new Color(37, 99, 235),
                Color.WHITE,
                new Color(0, 102, 204)
        );
    }

    public static ThemePalette forDark() {
        return new ThemePalette(
                true,
                new Color(24, 26, 31),
                new Color(30, 33, 39),
                new Color(38, 42, 50),
                new Color(22, 24, 29),
                new Color(230, 233, 239),
                new Color(156, 163, 175),
                new Color(62, 68, 79),
                new Color(25, 28, 33),
                new Color(32, 36, 43),
                new Color(45, 50, 59),
                new Color(59, 130, 246),
                Color.WHITE,
                new Color(96, 165, 250)
        );
    }

    public static ThemePalette fromDarkMode(boolean darkMode) {
        return darkMode ? forDark() : light();
    }
}