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

import java.io.*;
import java.util.ArrayList;

//Realtime intraday data from AlphaVantage is premium only
//There is now a 25 daily request limit for free keys
//KEY: "H0JHAOU61I4MESDZ"
//TODO: Maybe implement a key cycle script to make the limit not as daunting
//https://www.alphavantage.co/documentation/
public class AlphaVantageDownloader extends APIDownloader {

    private static final String FUNCTION = "TIME_SERIES_INTRADAY";
    private static final boolean ADJUSTED = false;
    private static final String OUTPUT_SIZE = "full";
    private static final String DATA_TYPE = "csv";

    private final String apiKey;
    private final boolean premium;

    private boolean limitReached;

    public AlphaVantageDownloader(String apiKey, boolean premium, String ticker, TimeFrame timeFrame, boolean extendedHours) {
        super(ticker, timeFrame, extendedHours);
        this.apiKey = apiKey;
        this.premium = premium;
        this.limitReached = false;
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

    public void download(String year, String month) {
        try {
            initDownloadContainers();
            downloadFile(year, month, "stock_data/AlphaVantage/" + getTicker() + "/" + getTimeFrame().getTag() + "/", getDownloadProgress());
            endDownloadContainers(true);
        } catch (IOException e) {
            endDownloadContainers(false);
            e.printStackTrace();
        }
    }

    public void download(int startYear, int startMonth, int endYear, int endMonth) {
        Container<Integer> year = new Container<>(startYear);
        Container<Integer> month = new Container<>(startMonth);

        int yearDiff = endYear - year.get();
        int monthDiff = endMonth - month.get();

        int maxRequestsPerMinute = 5;
        Container<Integer> minuteCount = new Container<>(0);

        boolean isAll = startYear == 2000 && startMonth == 1 && endYear == StockUtil.getAdjustedCurrentTime().getYearInt() && endMonth == StockUtil.getAdjustedCurrentTime().getMonthInt();
        String suffix = isAll ? "ALL" : getMonthString(startMonth) + "-" + startYear + "-" + getMonthString(endMonth) + "-" + endYear;

        String tempPath = "stock_data/AlphaVantage/" + getTicker() + "/" + getTimeFrame().getTag() + "/temp/";
        String mainPath = "stock_data/AlphaVantage/" + getTicker() + "/" + getTimeFrame().getTag();
        String fileName = getTicker() + "_" + getTimeFrame().getTag() + "_" + suffix;

        try {
            initDownloadContainers();
            while (true) {
                downloadFile(String.valueOf(year.get()), getMonthString(month.get()), tempPath, new Container<>(0d));

                //Load the last file, check if error. If so, break and set limit reached
                String lastFileName = getTicker() + "_" + getTimeFrame().getTag() + "_" + year.get() + "-" + getMonthString(month.get());
                ArrayList<String[]> lastFile = CSVManager.getDataFromCSV(tempPath, lastFileName);
                if (lastFile.get(0)[0].contains("{")) { //Requests will max out at 25/day
                    String newSuffix = getMonthString(startMonth) + "-" + startYear + "-" + getMonthString(month.get()) + "-" + year.get();
                    FileManager.deleteFile(tempPath, lastFileName.replace(".csv", "") + ".csv");
                    CSVManager.combineFiles(tempPath, mainPath, fileName.replace(suffix, "") + newSuffix);
                    formatStockCSV(mainPath, fileName.replace(suffix, "") + newSuffix);
                    FileManager.deleteFile(tempPath, "");

                    endDownloadContainers(false);
                    setLimitReached(true);
                    return;
                }

                double yearPercent = (double) (year.get() - startYear) / (yearDiff + 1);
                double monthPercent = ((year.get() == endYear) ? (double) month.get() / monthDiff : (double) month.get() / 12) / (yearDiff + 1);
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
                    if (!isPremium()) Thread.sleep(61 * 1000);
                    minuteCount.set(0);
                }
            }

            CSVManager.combineFiles(tempPath, mainPath, fileName);
            formatStockCSV(mainPath, fileName);
            FileManager.deleteFile(tempPath, "");

            endDownloadContainers(true);
        } catch (Exception e) {
            endDownloadContainers(false);
            e.printStackTrace();
        }
    }

    public void downloadAll() {
        download(2000, 1, StockUtil.getAdjustedCurrentTime().getYearInt(), StockUtil.getAdjustedCurrentTime().getMonthInt());
    }

    public void updateAll() {
        String tempPath = "stock_data/AlphaVantage/" + getTicker() + "/" + getTimeFrame().getTag() + "/temp/";
        String mainPath = "stock_data/AlphaVantage/" + getTicker() + "/" + getTimeFrame().getTag();
        try {
            getDownloadProgress().set(0d);
            FileManager.createFolderPath(tempPath);

            //Copy _ALL.csv file to temp path. Put that code here

            getDownloadProgress().set(.25);
            downloadFile(StockUtil.getAdjustedCurrentTime().getYear(), StockUtil.getAdjustedCurrentTime().getMonth(), tempPath, new Container<>(0d));
            getDownloadProgress().set(.5);
            formatStockCSV(tempPath, getTicker() + "_" + getTimeFrame().getTag() + "_" + StockUtil.getAdjustedCurrentTime().getYear() + "-" + StockUtil.getAdjustedCurrentTime().getMonth() + ".csv");
            getDownloadProgress().set(.6);
            CSVManager.combineFiles(tempPath, mainPath, getTicker() + "_" + getTimeFrame().getTag() + "_" + "UPDATED");
            FileManager.deleteFile(tempPath, "");
            endDownloadContainers(true);
        } catch (Exception e) {
            endDownloadContainers(false);
            e.printStackTrace();
        }
    }

    public static void formatStockCSV(String directory, String name) {
        CSVManager.clearDuplicates(directory, name);
        // Remove Label, Order: ID, Open, Close, Min, Max, Volume
        try {
            String fileDirectory = directory + (directory.equals("") ? "" : "/");
            String fileName = name.replace(".csv", "");
            FileReader reader = new FileReader(fileDirectory + fileName + ".csv");
            BufferedReader br = new BufferedReader(reader);
            FileWriter writer = new FileWriter(fileDirectory + fileName + "_temp" + ".csv");

            String line;
            while ((line = br.readLine()) != null) {
                if (!line.contains("timestamp")) { //Removes all labels
                    String[] lineArray = line.split(",");
                    //Time Format
                    String dateTime = lineArray[0];
                    if (!dateTime.contains("/") && !dateTime.contains("-")) {//If the datetime is not a datetime, skip formatting the line
                        writer.append(line);
                        writer.append("\n");
                        continue;
                    }

                    String[] date;
                    String[] time;
                    String year;
                    String month;
                    String day;
                    String hour;
                    String minute;
                    String second;
                    if (dateTime.contains("/")) {
                        date = dateTime.split(" ")[0].split("/");
                        time = dateTime.split(" ")[1].split(":");
                        year = date[2];
                        month = (Integer.parseInt(date[0]) < 10) ? "0" + date[0] : date[0];
                        day = (Integer.parseInt(date[1]) < 10) ? "0" + date[1] : date[1];
                        hour = (Integer.parseInt(time[0]) < 10) ? "0" + time[0] : time[0];
                        minute = time[1];
                        second = "00";
                    } else {
                        date = dateTime.split(" ")[0].split("-");
                        time = dateTime.split(" ")[1].split(":");
                        year = date[0];
                        month = date[1];
                        day = date[2];
                        hour = time[0];
                        minute = time[1];
                        second = time[2];
                    }
                    long dateTimeID = Long.parseLong(year + month + day + hour + minute + second);
                    lineArray[0] = String.valueOf(dateTimeID);

                    //Open,Close,Min,Max,Volume Format
                    String max = lineArray[2];
                    String close = lineArray[4];
                    lineArray[2] = close;
                    lineArray[4] = max;

                    String newLine = "";
                    for (int i = 0; i < lineArray.length; i++) {
                        newLine = newLine.concat(lineArray[i]);
                        if (i != lineArray.length - 1) newLine = newLine.concat(",");
                    }
                    writer.append(newLine);
                    writer.append("\n");
                }
            }
            writer.flush();
            writer.close();
            reader.close();
            FileManager.deleteFile(directory, fileName + ".csv");
            FileManager.renameFile(directory, fileName + "_temp" + ".csv", fileName + ".csv");
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        return "https://www.alphavantage.co/query?function=" + FUNCTION + "&symbol=" + getTicker() + "&interval=" + getTimeFrame().getTag() + "&adjusted=" + ADJUSTED + "&extended_hours=" + isExtendedHours() + "&month=" + month + "&outputsize=" + OUTPUT_SIZE + "&apikey=" + getApiKey() + "&datatype=" + DATA_TYPE;
    }

    private void setLimitReached(boolean limitReached) {
        this.limitReached = limitReached;
    }

    public boolean isLimitReached() {
        return limitReached;
    }

    public boolean isPremium() {
        return premium;
    }

    public String getApiKey() {
        return apiKey;
    }

}
