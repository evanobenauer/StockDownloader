package com.ejo.stockdownloader;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowui.Window;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.scenes.DownloadScene;
import com.ejo.stockdownloader.scenes.TitleScene;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.FileOutputStream;
import java.io.IOException;

public class Main {

    static Window window = new Window(
            "Stock Downloader",
            new Vector(100,100),
            new Vector(800,600),
            new TitleScene(), true,4,60,60);

    public static void main(String[] args) throws IOException {
        window.run();
        window.close();
    }


    public static void downloadCSV(String stockTicker, String timeSeries, String interval, String outputSize, String slice, String apiKey) throws IOException {
        String folderPath = "stock_history/" + stockTicker + "/" + interval + "/";
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String url = "https://www.alphavantage.co/query?function=" + timeSeries + "&symbol=" + stockTicker + "&interval=" + interval + "&slice=" + slice + "&outputsize=" + outputSize + "&adjusted=true&apikey=" + apiKey + "&datatype=csv" + "&adjusted=false";
        System.out.println(url);
        HttpGet request = new HttpGet(url);

        CloseableHttpResponse response = httpClient.execute(request);

    }

}