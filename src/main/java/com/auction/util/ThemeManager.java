
package com.auction.util;

import javafx.scene.Scene;

public class ThemeManager {
    private static ThemeManager instance;
    private boolean darkMode = true;

    private ThemeManager() {}

    public static ThemeManager getInstance() {
        if (instance == null) instance = new ThemeManager();
        return instance;
    }

    public boolean isDarkMode() { return darkMode; }

    public void toggleTheme() { darkMode = !darkMode; }

    public String getCurrentThemeName() {
        return darkMode ? "Dark" : "Light";
    }

    public void setCurrentScene(Scene scene) {
        scene.getStylesheets().clear();
        String css = darkMode ? "/styles/dark-theme.css" : "/styles/light-theme.css";
        var resource = getClass().getResource(css);
        if (resource != null) {
            scene.getStylesheets().add(resource.toExternalForm());
        }
    }
}