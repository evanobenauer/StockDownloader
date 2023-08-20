package com.ejo.stockdownloader.data.api;

import com.ejo.glowlib.file.CSVManager;
import com.ejo.glowlib.file.FileManager;
import com.ejo.glowlib.setting.Container;
import com.ejo.stockdownloader.util.StockUtil;
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
                downloadFile(year,month,"stock_data/AlphaVantage/" + getTicker() + "/" + getTimeFrame().getTag() + "/",getDownloadProgress());
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

    public void downloadGroup(int startYear, int startMonth, int endYear, int endMonth) {
        Container<Integer> year = new Container<>(startYear);
        Container<Integer> month = new Container<>(startMonth);

        int yearDiff = endYear - year.get();
        int monthDiff = endMonth - month.get();

        Container<Integer> minuteCount = new Container<>(0);
        int maxRequestsPerMinute = 5;

        boolean isAll = startYear == 2000 && startMonth == 1 && endYear == StockUtil.getAdjustedCurrentTime().getYearInt() && endMonth == StockUtil.getAdjustedCurrentTime().getMonthInt();
        String suffix = isAll ? "ALL" : getMonthString(startMonth) + "-" + startYear + "-" + getMonthString(endMonth) + "-" + endYear;

        String tempPath = "stock_data/AlphaVantage/" + getTicker() + "/" + getTimeFrame().getTag() + "/temp/";
        String mainPath = "stock_data/AlphaVantage/" + getTicker() + "/" + getTimeFrame().getTag();
        String fileName = getTicker() + "_" + getTimeFrame().getTag() + "_" + suffix;

        Thread thread = new Thread(() -> {
            try {
                initDownloadContainers();
                while (true) {
                    downloadFile(String.valueOf(year.get()), getMonthString(month.get()), tempPath, new Container<>(0d));

                    double yearPercent = (double) (year.get() - 2000) / (yearDiff + 1);
                    double monthPercent = ((year.get() == endYear) ? (double) month.get() / monthDiff : (double) month.get() / 12) / yearDiff;
                    setDownloadProgress(yearPercent + monthPercent);

                    if (year.get() == endYear && month.get() == endMonth) break;

                    if (month.get() != 12) {
                        month.set(month.get() + 1);
                    } else {
                        month.set(1);
                        year.set(year.get() + 1);
                    }

                    minuteCount.set(minuteCount.get() + 1);
                    if (minuteCount.get() == maxRequestsPerMinute) {
                        Thread.sleep(61 * 1000);
                        minuteCount.set(0);
                    }
                }

                CSVManager.combineFiles(tempPath,mainPath,fileName);
                CSVManager.clearDuplicates(mainPath,fileName);
                FileManager.deleteFile(tempPath,"");

                endDownloadContainers(true);
            } catch (Exception e) {
                endDownloadContainers(false);
                e.printStackTrace();
            }
        });
        thread.setName("AlphaVantage Download Thread");
        thread.setDaemon(true);
        thread.start();
    }

    public void downloadAll() {
        downloadGroup(2000, 1, StockUtil.getAdjustedCurrentTime().getYearInt(), StockUtil.getAdjustedCurrentTime().getMonthInt());
    }

    //TODO: Have this download the last month, then combine to the All CSV
    public void updateAll() {

    }

    private void downloadFile(String year, String month, String filePath, Container<Double> downloadProgress) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(getURL(year + "-" + month));
        downloadProgress.set(.25);
        HttpResponse response = httpClient.execute(httpGet);
        downloadProgress.set(.5);
        FileManager.createFolderPath(filePath);

        String fileName = getTicker() + "_" + getTimeFrame().getTag() + "_" + year + "-" + month + ".csv";

        FileOutputStream fos = new FileOutputStream(filePath + fileName);
        downloadProgress.set(.75);
        response.getEntity().writeTo(fos);
        fos.close();
    }

    private String getMonthString(int month) {
        return switch (month) {
            case 1 -> "01";
            case 2 -> "02";
            case 3 -> "03";
            case 4 -> "04";
            case 5 -> "05";
            case 6 -> "06";
            case 7 -> "07";
            case 8 -> "08";
            case 9 -> "09";
            case 10 -> "10";
            case 11 -> "11";
            case 12 -> "12";
            default -> throw new IllegalStateException("Unexpected value: " + month);
        };
    }


    private String getURL(String month) {
        return "https://www.alphavantage.co/query?function=" + FUNCTION + "&symbol=" + getTicker() + "&interval="+getTimeFrame().getTag()+"&adjusted=" + ADJUSTED + "&extended_hours=" + isExtendedHours() + "&month=" + month + "&outputsize=" + OUTPUT_SIZE + "&apikey=" + getApiKey() + "&datatype=" + DATA_TYPE;
    }


    public String getApiKey() {
        return apiKey;
    }

}
