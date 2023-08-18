package com.ejo.stockdownloader.data.api;

import com.ejo.glowlib.file.FileManager;
import com.ejo.stockdownloader.util.TimeFrame;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.FileOutputStream;
import java.io.IOException;

//Realtime intraday data from AlphaVantage is premium only
//KEY: "H0JHAOU61I4MESDZ"
//https://www.alphavantage.co/documentation/
public class AlphaVantageDownloader extends APIDownloader {

    private final String FUNCTION = "TIME_SERIES_INTRADAY";
    private final boolean ADJUSTED = false;
    private final String OUTPUT_SIZE = "full";
    private final String DATA_TYPE = "csv";

    private final String apiKey;

    public AlphaVantageDownloader(String apiKey, String ticker, TimeFrame timeFrame, boolean extendedHours) {
        super(ticker,timeFrame,extendedHours);
        this.apiKey = apiKey;
    }

    public void download(String year, String month) {
        Thread thread = new Thread(() -> {
            try {
                initDownloadContainers();
                CloseableHttpClient httpClient = HttpClients.createDefault();
                HttpGet httpGet = new HttpGet(getURL(year + "-" + month));
                HttpResponse response = httpClient.execute(httpGet);

                String filePath = "stock_data/AlphaVantage/" + getTicker() + "/" + getTimeFrame().getTag() + "/";
                FileManager.createFolderPath(filePath);

                String fileName = getTicker() + "_" + getTimeFrame().getTag() + "_" + year + "-" + month + ".csv";

                FileOutputStream fos = new FileOutputStream(filePath + fileName);
                response.getEntity().writeTo(fos);
                fos.close();
                endDownloadContainers(true);
            } catch (IOException e) {
                endDownloadContainers(false);
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

}
