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
import java.util.Arrays;
import java.util.HashSet;
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

    public final String PATH_MAIN = "stock_data/AlphaVantage/" + getTicker() + "/" + getTimeFrame().getTag() + "/";
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

        FileOutputStream fos = new FileOutputStream(filePath + "/" + fileName);
        downloadProgress.set(.75);
        response.getEntity().writeTo(fos);
        fos.close();
    }

    public void download(String year, String month) {
        try {
            initDownloadContainers();
            downloadFile(year, month, PATH_MAIN, getDownloadProgress());

            //Load the last file, check if error. If so, break and set limit reached
            ArrayList<String[]> lastFile = CSVManager.getDataFromCSV(PATH_MAIN, FILE_PREFIX + "_" + year + "-" + month + ".csv");
            if (lastFile.get(0)[0].contains("{")) { //Requests will max out at 25/day
                FileManager.deleteFile(PATH_MAIN, FILE_PREFIX + "_" + year + "-" + month + ".csv");
                endDownloadContainers(false);
                setLimitReached(true);
                return;
            }

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

        String suffix = getMonthString(startMonth) + "-" + startYear + "-" + getMonthString(endMonth) + "-" + endYear;

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
        download(2000, 1, StockUtil.getAdjustedCurrentTime().getYear(), StockUtil.getAdjustedCurrentTime().getMonth());
    }

    public static void formatStockCSV(String directory, String name) {
        // Remove Label, Order: ID, Open, Close, Min, Max, Volume

        HashSet<String> idList = new HashSet<>();
        try {
            String fileDirectory = directory + (directory.equals("") ? "" : "/");
            String fileName = name.replace(".csv", "");

            FileReader fileReader = new FileReader(fileDirectory + fileName + ".csv");
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            FileWriter fileWriter = new FileWriter(fileDirectory + fileName + "_temp" + ".csv");

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains("timestamp")) continue; //Removes all labels
                if (line.contains("{") || line.contains("}") || line.contains("Alpha Vantage")) continue; //Remove AlphaVantage Error Lines

                String[] lineArray = line.split(",");

                //Time Format
                String dateTime = lineArray[0];

                if (!idList.add(dateTime)) continue; //Clear values with repeat dateTimes

                if (!dateTime.contains("/") && !dateTime.contains("-")) {//If the datetime is not a formatted DateTime, skip the line
                    fileWriter.append(line);
                    fileWriter.append("\n");
                    continue;
                }

                lineArray[0] = String.valueOf(getDateTimeIDFromAlphaVantageFormat(dateTime));

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
                fileWriter.append(newLine);
                fileWriter.append("\n");
            }

            fileWriter.flush();

            fileWriter.close();
            fileReader.close();
            FileManager.deleteFile(directory, fileName + ".csv");
            FileManager.renameFile(directory, fileName + "_temp" + ".csv", fileName + ".csv");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static long getDateTimeIDFromAlphaVantageFormat(String dateTime) {
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
        return Long.parseLong(year + month + day + hour + minute + second);
    }

    public boolean combineToLiveFile() {
        String liveFilePath = "stock_data/";
        FileManager.createFolderPath(liveFilePath);
        FileManager.createFolderPath(PATH_MAIN);
        CSVManager.combineFiles(PATH_MAIN,liveFilePath,FILE_PREFIX + "_AV");
        formatStockCSV(liveFilePath,FILE_PREFIX + "_AV");

        HashSet<String> idList = new HashSet<>();
        try {
            FileWriter writer = new FileWriter(liveFilePath + FILE_PREFIX + "_temp.csv");

            ArrayList<String> fileNameList = new ArrayList<>(List.of(FILE_PREFIX + "_AV" + ".csv",FILE_PREFIX + ".csv"));

            //For the live file and alpha vantage file. AlphaVantage comes FIRST, Live comes SECOND. This is to give priority to AV repeats
            for (String file : fileNameList) {
                try {
                    FileReader fileReader = new FileReader(liveFilePath + "/" + file);
                    BufferedReader bufferedReader = new BufferedReader(fileReader);
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        if (!idList.add(line.split(",")[0])) continue; //If the list already contains this dateTimeID, do not add a new one
                        writer.append(line);
                        writer.append("\n");
                    }
                    bufferedReader.close();
                    fileReader.close();
                    FileManager.deleteFile(liveFilePath, file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            writer.flush();
            writer.close();
            FileManager.renameFile(liveFilePath, FILE_PREFIX + "_temp.csv", FILE_PREFIX + ".csv");
            return true;
        } catch (IOException e) {
            System.out.println("Could not combine CSV Files");
            e.printStackTrace();
            return false;
        }
    }


    public static String getMonthString(int month) {
        if (month < 0 || month > 12) throw new IllegalStateException("Unexpected value: Month=" + month);
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
