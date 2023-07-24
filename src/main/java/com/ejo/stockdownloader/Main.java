package com.ejo.stockdownloader;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowui.Window;
import com.ejo.stockdownloader.scenes.TitleScene;
import com.ejo.stockdownloader.util.StockUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class Main {

    public static Window window = new Window(
            "Stock Downloader",
            new Vector(100,100),
            new Vector(800,600),
            new TitleScene(), true,4,60,60);

    public static void main(String[] args) {
        window.run();
        window.close();
    }

}