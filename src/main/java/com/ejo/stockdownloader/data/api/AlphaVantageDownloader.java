package com.ejo.stockdownloader.data.api;

import com.ejo.glowlib.file.FileManager;
import com.ejo.glowlib.setting.Container;
import com.ejo.stockdownloader.util.TimeFrame;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.FileOutputStream;
import java.io.IOException;

//Realtime intraday data from AlphaVantage is premium only
//KEY: "H0JHAOU61I4MESDZ"
//https://www.alphavantage.co/documentation/
public class AlphaVantageDownloader {

    private final String FUNCTION = "TIME_SERIES_INTRADAY";
    private final boolean ADJUSTED = false;
    private final String OUTPUT_SIZE = "full";
    private final String DATA_TYPE = "csv";

    private final String apiKey;
    private final String ticker;
    private final TimeFrame timeFrame;
    private final boolean extendedHours;

    private final Container<Double> downloadPercent = new Container<>(0d);

    public AlphaVantageDownloader(String apiKey, String ticker, TimeFrame timeFrame, boolean extendedHours) {
        this.apiKey = apiKey;
        this.ticker = ticker;
        this.timeFrame = timeFrame;
        this.extendedHours = extendedHours;
    }

    public void download(String year, String month) {
        //TODO: Update Download Percent
        //TODO: Add Download Complete Text to Title
        Thread thread = new Thread(() -> {
            try {
                CloseableHttpClient httpClient = HttpClients.createDefault();
                HttpGet httpGet = new HttpGet(getURL(year + "-" + month));
                HttpResponse response = httpClient.execute(httpGet);

                String filePath = "stock_data/AlphaVantage/" + getTicker() + "/" + getTimeFrame().getTag() + "/";
                FileManager.createFolderPath(filePath);

                String fileName = getTicker() + "_" + getTimeFrame().getTag() + "_" + year + "-" + month + ".csv";

                FileOutputStream fos = new FileOutputStream(filePath + fileName);
                response.getEntity().writeTo(fos);
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.setName("AlphaVantage Download Thread");
        thread.setDaemon(false);
        thread.start();
    }

    //TODO: Have this download all months, then combine all to 1 CSV
    public boolean downloadAll() {
        return false;
    }

    //TODO: Have this download the last month, then combine to the All CSV
    public boolean updateAll() {
        return false;
    }


    private String getURL(String month) {
        return "https://www.alphavantage.co/query?function=" + FUNCTION + "&symbol=" + getTicker() + "&interval="+getTimeFrame().getTag()+"&adjusted=" + ADJUSTED + "&extended_hours=" + isExtendedHours() + "&month=" + month + "&outputsize=" + OUTPUT_SIZE + "&apikey=" + getApiKey() + "&datatype=" + DATA_TYPE;
    }


    public String getApiKey() {
        return apiKey;
    }

    public String getTicker() {
        return ticker;
    }

    public TimeFrame getTimeFrame() {
        return timeFrame;
    }

    public boolean isExtendedHours() {
        return extendedHours;
    }

}
