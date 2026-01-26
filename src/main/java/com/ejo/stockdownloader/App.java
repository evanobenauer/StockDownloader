package com.ejo.stockdownloader;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowui.Window;
import com.ejo.stockdownloader.scenes.TitleScene;
import com.ejo.stockdownloader.util.DownloadStockUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

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