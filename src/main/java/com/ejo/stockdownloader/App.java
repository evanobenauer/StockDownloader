package com.ejo.stockdownloader;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowui.Window;
import com.ejo.stockdownloader.scenes.TitleScene;

public class App {

    private static final Window window = new Window(
            "Stock Downloader",
            new Vector(100,100),
            new Vector(800,600),
            new TitleScene(), true,4,60,60);

    public static void main(String[] args) {
        window.run();
        window.close();
    }

    public static Window getWindow() {
        return window;
    }
}