package com.ejo.stockdownloader.data.api;

import com.ejo.glowlib.file.FileManager;
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

    public AlphaVantageDownloader(String apiKey, String ticker, TimeFrame timeFrame, boolean extendedHours) {
        this.apiKey = apiKey;
        this.ticker = ticker;
        this.timeFrame = timeFrame;
        this.extendedHours = extendedHours;
    }

    public boolean download(String year, String month) {
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
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean downloadAll() {
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
