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
import java.util.List;

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

    public final String PATH_MAIN = "stock_data/AlphaVantage/" + getTicker() + "/" + getTimeFrame().getTag();
    public final String PATH_TEMP = "stock_data/AlphaVantage/" + getTicker() + "/" + getTimeFrame().getTag() + "/temp/";
    public final String FILE_PREFIX = getTicker() + "_" + getTimeFrame().getTag();

    public AlphaVantageDownloader(String apiKey, boolean premium, String ticker, TimeFrame timeFrame, boolean extendedHours) {
        super(ticker, timeFrame, extendedHours);
        this.apiKey = apiKey;
        this.premium = premium;
        this.limitReached = false;
    }

    private void downloadFile(String year, String month, String filePath, Container<Double> downloadProgress) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(getURL(year,month));
        downloadProgress.set(.25);
        HttpResponse response = httpClient.execute(httpGet);
        downloadProgress.set(.5);
        FileManager.createFolderPath(filePath);

        String fileName = FILE_PREFIX + "_" + year + "-" + month + ".csv";

        FileOutputStream fos = new FileOutputStream(filePath + fileName);
        downloadProgress.set(.75);
        response.getEntity().writeTo(fos);
        fos.close();
    }

    public void download(String year, String month) {
        try {
            initDownloadContainers();
            downloadFile(year, month, PATH_MAIN, getDownloadProgress());
            formatStockCSV(PATH_MAIN, FILE_PREFIX + "_" + year + "-" + month + ".csv");
            endDownloadContainers(true);
        } catch (IOException e) {
            endDownloadContainers(false);
            e.printStackTrace();
        }
    }

    public void download(int startYear, int startMonth, int endYear, int endMonth) {
        int yearRange = endYear - startYear;
        int monthRange = endMonth - startMonth;

        int year = startYear;
        int month = startMonth;

        int maxRequestsPerMinute = 5;
        int minuteCount = 0;

        boolean isAll = startYear == 2000 && startMonth == 1 && endYear == StockUtil.getAdjustedCurrentTime().getYearInt() && endMonth == StockUtil.getAdjustedCurrentTime().getMonthInt();
        String suffix = isAll ? "ALL" : getMonthString(startMonth) + "-" + startYear + "-" + getMonthString(endMonth) + "-" + endYear;

        String fileName = FILE_PREFIX + "_" + suffix;

        try {
            initDownloadContainers();
            while (true) {
                downloadFile(String.valueOf(year), getMonthString(month), PATH_TEMP, new Container<>(0d));
                //Load the last file, check if error. If so, break and set limit reached
                String lastFileName = FILE_PREFIX + "_" + year + "-" + getMonthString(month);
                ArrayList<String[]> lastFile = CSVManager.getDataFromCSV(PATH_TEMP, lastFileName);
                if (lastFile.get(0)[0].contains("{")) { //Requests will max out at 25/day
                    String newSuffix = getMonthString(startMonth) + "-" + startYear + "-" + getMonthString(month) + "-" + year;

                    FileManager.deleteFile(PATH_TEMP, lastFileName + ".csv");
                    CSVManager.combineFiles(PATH_TEMP, PATH_MAIN, fileName.replace(suffix, newSuffix));
                    formatStockCSV(PATH_MAIN, fileName.replace(suffix, newSuffix));
                    FileManager.deleteFile(PATH_TEMP, "");

                    endDownloadContainers(false);
                    setLimitReached(true);
                    return;
                }

                double yearPercent = (double) (year - startYear) / (yearRange + 1);
                double monthPercent = ((year == endYear) ? (double) month / monthRange : (double) month / 12) / (yearRange + 1);
                setDownloadProgress(yearPercent + monthPercent);

                if (year == endYear && month == endMonth) break;

                if (month == 12) {
                    month = 1;
                    year += 1;
                } else {
                    month += 1;
                }

                minuteCount += 1;
                if (minuteCount == maxRequestsPerMinute) {
                    if (!isPremium()) Thread.sleep(61 * 1000);
                    minuteCount = 0;
                }
            }

            CSVManager.combineFiles(PATH_TEMP, PATH_MAIN, fileName);
            formatStockCSV(PATH_MAIN, fileName);
            FileManager.deleteFile(PATH_TEMP, "");

            endDownloadContainers(true);
        } catch (Exception e) {
            endDownloadContainers(false);
            e.printStackTrace();
        }
    }

    public void downloadAll() {
        download(2000, 1, StockUtil.getAdjustedCurrentTime().getYearInt(), StockUtil.getAdjustedCurrentTime().getMonthInt());
    }

    /**
     * Updates ONLY the last month of data and adds it to the ALL file located in the stock_data folder
     */
    public void updateAll() {
        try {
            getDownloadProgress().set(0d);
            FileManager.createFolderPath(PATH_TEMP);

            //TODO: Copy _ALL.csv file to temp path. Put that code here
            getDownloadProgress().set(.25);

            downloadFile(StockUtil.getAdjustedCurrentTime().getYear(), StockUtil.getAdjustedCurrentTime().getMonth(), PATH_TEMP, new Container<>(0d));
            getDownloadProgress().set(.5);

            formatStockCSV(PATH_TEMP, FILE_PREFIX + "_" + StockUtil.getAdjustedCurrentTime().getYear() + "-" + StockUtil.getAdjustedCurrentTime().getMonth());
            getDownloadProgress().set(.6);

            CSVManager.combineFiles(PATH_TEMP, PATH_MAIN, FILE_PREFIX + "_" + "UPDATED");
            CSVManager.clearDuplicates(PATH_MAIN, FILE_PREFIX + "_" + "UPDATED");
            FileManager.deleteFile(PATH_TEMP, "");

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

    public void combineToLiveFile() {
        String liveFilePath = "stock_data/";
        CSVManager.combineFiles(PATH_MAIN,liveFilePath,FILE_PREFIX + "_AV");
        formatStockCSV(liveFilePath,FILE_PREFIX + "_AV");

        try {
            FileManager.createFolderPath(liveFilePath);
            List<String> files = CSVManager.getCSVFilesInDirectory(liveFilePath);
            FileWriter writer = new FileWriter(liveFilePath + FILE_PREFIX + "_temp.csv");

            for (String file : files) {
                if (file.contains(FILE_PREFIX)) {

                    FileReader fileReader = new FileReader(liveFilePath + "/" + file);
                    BufferedReader reader = new BufferedReader(fileReader);
                    String line;
                    while ((line = reader.readLine()) != null) {
                        writer.append(line);
                        writer.append("\n");
                    }
                    reader.close();
                    fileReader.close();
                    FileManager.deleteFile(liveFilePath, file);
                }
            }
            writer.flush();
            writer.close();
            FileManager.renameFile(liveFilePath, FILE_PREFIX + "_temp.csv", FILE_PREFIX + ".csv");
        } catch (IOException e) {
            System.out.println("Could not combine CSV Files");
            e.printStackTrace();
        }
        CSVManager.clearDuplicates(liveFilePath,FILE_PREFIX + ".csv");
    }

    public static String getMonthString(int month) {
        if (month < 0 || month > 12) throw new IllegalStateException("Unexpected value: " + month);
        return month < 10 ? "0" + month : String.valueOf(month);
    }


    private String getURL(String year, String month) {
        return "https://www.alphavantage.co/query?function=" + FUNCTION + "&symbol=" + getTicker() + "&interval=" + getTimeFrame().getTag() + "&adjusted=" + ADJUSTED + "&extended_hours=" + isExtendedHours() + "&month=" + year + "-" + month + "&outputsize=" + OUTPUT_SIZE + "&apikey=" + getApiKey() + "&datatype=" + DATA_TYPE;
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
