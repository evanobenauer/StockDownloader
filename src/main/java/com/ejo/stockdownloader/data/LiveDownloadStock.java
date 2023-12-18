package com.ejo.stockdownloader.data;

import com.ejo.glowlib.file.FileManager;
import com.ejo.glowlib.misc.DoOnce;
import com.ejo.glowlib.setting.Container;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowlib.time.StopWatch;
import com.ejo.stockdownloader.util.DownloadStockUtil;
import com.ejo.stockdownloader.util.DownloadTimeFrame;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;

/**
 * The stock class is a multi use class. It encompasses both loading historical data and adding new data to said history. The live data is updated
 * by a method whenever it is called
 * Historical data is saved in the folder: stock_data/ticker_timeframe.csv. Place data into this location for it to be
 * saved and loaded with the stock
 */
public class LiveDownloadStock {

    //Stock Information
    private final String ticker;
    private final DownloadTimeFrame timeFrame;
    private final boolean extendedHours;
    private final PriceSource livePriceSource;

    //Historical Data HashMap
    private final HashMap<Long, float[]> dataHash = new HashMap<>();

    //Open Time
    private DateTime openTime;

    //Open-Close Percentage
    private final Container<Double> closePercent;

    //Progress Data
    private final Container<Double> saveProgress = new Container<>(0d);
    protected boolean saving = false;

    //Live Price Variables
    private float price;
    private float open;
    private float min;
    private float max;

    //Live Price Update Variables
    private boolean shouldStartUpdates;
    private final StopWatch updateTimer = new StopWatch();

    //Do Once Definitions
    private final DoOnce doLivePriceUpdate = new DoOnce();
    private final DoOnce doOpen = new DoOnce();
    private final DoOnce doClose = new DoOnce();

    //Default Constructor
    public LiveDownloadStock(String ticker, DownloadTimeFrame timeFrame, boolean extendedHours, PriceSource livePriceSource) {
        this.ticker = ticker;
        this.timeFrame = timeFrame;
        this.extendedHours = extendedHours;
        this.livePriceSource = livePriceSource;

        this.setAllData(-1);
        this.closePercent = new Container<>(0d);
        this.shouldStartUpdates = false;

        this.doLivePriceUpdate.reset();
        this.doOpen.reset();
        this.doClose.reset();
    }


    /**
     * This method updates the live price of the stock as well as the min and max. Depending on the timeframe, the stock will save data to the dataList periodically with this method
     * **METHOD PROCESS: Waits... [Time to close: Updates the close, updates the price, updates the open], Waits...**
     */
    public void updateLiveData(double liveDelayS, boolean includePriceUpdate) {
        //Updates the progress bar of each segmentation
        if (DownloadStockUtil.isPriceActive(isExtendedHours(), DownloadStockUtil.getAdjustedCurrentTime())) updateClosePercent();

        //Check if the stock should update. If not, don't run the method
        if (!shouldUpdate()) return;

        //Close the previous segment
        updateClose();

        //Update live price every provided delay second or update the live price on the start of every open
        if (includePriceUpdate) updateLivePrice(liveDelayS);

        //Open the next segment
        updateOpen();

        //Updates the minimum/maximum values of the stock price over the time frame
        updateMinMax();
    }

    /**
     * Retrieves and sets the live price data gathered for the stock from web scraping.
     */
    public void updateLivePrice() {
        try {
            float livePrice;
            switch (getLivePriceSource()) {
                case MARKETWATCH -> {
                    String url = "https://www.marketwatch.com/investing/fund/" + getTicker();
                    livePrice = DownloadStockUtil.getWebScrapePrice(url, "bg-quote.value", 0);
                }
                case YAHOOFINANCE -> {
                    String url2 = "https://finance.yahoo.com/quote/" + getTicker() + "?p=" + getTicker();
                    livePrice = DownloadStockUtil.getWebScrapePrice(url2, "data-test", "qsp-price", 0);
                }
                default -> livePrice = -1;
            }
            if (livePrice != -1) this.price = livePrice;
        } catch (IOException e) {
            System.out.println("Live Data: Timed Out");
        }
    }


    /**
     * Updates the live price data every timeframe specified in the liveDelay in seconds. The method will also force an update at the beginning of every open to make sure the stock
     * is up-to-date.
     * It is best to include this update in a parallel thread as the price scraping from the internet may cause lag
     *
     * @param liveDelayS
     */
    public void updateLivePrice(double liveDelayS) {
        updateTimer.start();
        if (updateTimer.hasTimePassedS(liveDelayS) || shouldClose()) {
            doLivePriceUpdate.run(() -> {
                updateTimer.restart();
                updateLivePrice();
            });
        }

        //Have live price updates reset if the stock should not close to continue with the liveDelay. This is so the stock will FORCE an update each open. Shown above
        if (!shouldClose()) doLivePriceUpdate.reset();
    }


    /**
     * Sets the stock's open, min, and max to the current price value only when doOpen is set to reset
     */
    private void updateOpen() {
        this.doOpen.run(() -> {
            this.openTime = DownloadStockUtil.getAdjustedCurrentTime();
            setAllData(getPrice());
        });
    }


    /**
     * Updates the splitting of the stock into candles based on the TimeFrame of the stock selected. This method adds an entry to the historical data HashMap and then resets the livedata to the current price
     */
    private void updateClose() {
        if (!shouldClose()) {
            doClose.reset();
            return;
        }
        this.doClose.run(() -> {
            DateTime ct = DownloadStockUtil.getAdjustedCurrentTime();
            //Save Live Data as Historical [Data is stored as (DATETIME,OPEN,CLOSE,MIN,MAX)]
            float[] timeFrameData = {getOpen(), getPrice(), getMin(), getMax()};
            DateTime openTime = new DateTime(ct.getYear(), ct.getMonth(), ct.getDay(), ct.getHour(), ct.getMinute(), ct.getSecond() - getTimeFrame().getSeconds());
            if (getOpenTime() != null) dataHash.put(openTime.getDateTimeID(), timeFrameData);

            //Set stock ready for open
            doOpen.reset();
        });
    }


    /**
     * Updates the minimum/maximum values of the stock over the time frame period. This is reset upon open
     */
    private void updateMinMax() {
        if (getOpenTime() == null) return;
        if (getPrice() < getMin()) this.min = getPrice();
        if (getPrice() > getMax()) this.max = getPrice();
    }


    /**
     * Updates the percentage complete for the current stock candle
     */
    private void updateClosePercent() {
        DateTime ct = DownloadStockUtil.getAdjustedCurrentTime();
        double totalPercent = 0;

        //Second Percent
        double secPercent = (double) ct.getSecond() / getTimeFrame().getSeconds();
        totalPercent += secPercent;

        //Minute Percent
        double minPercent = ct.getMinute() / ((double) getTimeFrame().getSeconds() / 60);
        totalPercent += minPercent;

        //Hour Percent
        double hrPercent = ct.getHour() / ((double) getTimeFrame().getSeconds() / 60 / 60);
        totalPercent += hrPercent;

        totalPercent -= Math.floor(totalPercent);
        getClosePercent().set(totalPercent);
    }


    public boolean saveHistoricalData(String filePath, String fileName) {
        getSaveProgress().set(0d);
        this.saving = true;

        //Load all file data
        HashMap<Long,float[]> dataMap = new HashMap<>();
        try {
            File file = new File(filePath + (fileName.equals("") ? "" : "/") + fileName.replace(".csv", "") + ".csv");

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                long fileSize = Files.lines(file.toPath()).count();
                long currentRow = 0;
                while ((line = reader.readLine()) != null) {
                    String[] row = line.split(",");
                    long key = Long.parseLong(row[0]);
                    String[] rowCut = line.replace(key + ",", "").split(",");

                    float[] floatRowCut = new float[rowCut.length];
                    for (int i = 0; i < rowCut.length; i++) floatRowCut[i] = Float.parseFloat(rowCut[i]);

                    dataMap.put(key, floatRowCut);
                    currentRow += 1;
                    getSaveProgress().set((double) currentRow / fileSize / 2);
                }
            } catch (IOException | SecurityException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Add all class data
        dataMap.putAll(getHistoricalData());

        //Save all data
        FileManager.createFolderPath(filePath); //Creates the folder path if it does not exist
        String outputFile = filePath + (filePath.equals("") ? "" : "/") + fileName.replace(".csv","") + ".csv";
        long fileSize = dataMap.size();
        long currentRow = 0;
        try(FileWriter writer = new FileWriter(outputFile)) {
            for (Long key : dataMap.keySet()) {
                writer.write(key + "," + Arrays.toString(dataMap.get(key)).replace("[","").replace("]","").replace(" ","") + "\n");
                currentRow += 1;
                getSaveProgress().set((double) currentRow / fileSize / 2 + .5);
            }
            this.saving = false;
            return true;
        } catch (IOException | SecurityException e) {
            e.printStackTrace();
        }
        this.saving = false;
        return false;
    }

    public boolean saveHistoricalData() {
        return saveHistoricalData("stock_data", getTicker() + "_" + getTimeFrame().getTag());
    }

        /**
         * Checks if the stock should update live data. This method has the main purpose of stopping the update method if returned false
         *
         * @return
         */
    public boolean shouldUpdate() {
        //Wait until the start of the candle timeframe to allow updates
        if (shouldClose()) this.shouldStartUpdates = true;
        if (!this.shouldStartUpdates) return false;

        //Only allows for data collection during trading hours
        return DownloadStockUtil.isPriceActive(isExtendedHours(), DownloadStockUtil.getAdjustedCurrentTime());
    }

    /**
     * This method will return true if the stock is at a place to go through with a split depending on the current TimeFrame
     *
     * @return
     */
    public boolean shouldClose() {
        DateTime ct = DownloadStockUtil.getAdjustedCurrentTime();
        return switch (getTimeFrame()) {
            case ONE_SECOND -> true;
            case FIVE_SECONDS -> ct.getSecond() % 5 == 0;
            case THIRTY_SECONDS -> ct.getSecond() % 30 == 0;
            case ONE_MINUTE -> ct.getSecond() == 0;
            case FIVE_MINUTES -> ct.getMinute() % 5 == 0 && ct.getSecond() == 0;
            case FIFTEEN_MINUTES -> ct.getMinute() % 15 == 0 && ct.getSecond() == 0;
            case THIRTY_MINUTES -> ct.getMinute() % 30 == 0 && ct.getSecond() == 0;
            case ONE_HOUR -> ct.getHour() == 0 && ct.getMinute() == 0 && ct.getSecond() == 0;
            case TWO_HOUR -> ct.getHour() % 2 == 0 && ct.getMinute() == 0 && ct.getSecond() == 0;
            case FOUR_HOUR -> ct.getHour() % 4 == 0 && ct.getMinute() == 0 && ct.getSecond() == 0;
            case ONE_DAY -> ct.getHour() % 8 == 0 && ct.getMinute() == 0 && ct.getSecond() == 0;
        };
    }

    /**
     * Sets all the data pertaining to the stock to a single value. This includes the price, open, min, and max
     *
     * @param value
     */
    private void setAllData(float value) {
        this.price = value;
        this.open = value;
        this.min = value;
        this.max = value;
    }

    /**
     * Returns the raw data from the historical hashmap.
     * This is in the format of: Open, Close, Min, Max, Volume
     * @param dateTime
     * @return
     */
    public float[] getData(DateTime dateTime) {
        float[] rawData = getHistoricalData().get(dateTime.getDateTimeID());
        if (rawData == null)
            return dateTime.equals(getOpenTime()) ? new float[]{getOpen(),getPrice(),getMin(),getMax()} : new float[]{-1,-1,-1,-1,-1};
        return rawData;
    }

    public float getOpen() {
        return open;
    }

    public float getOpen(DateTime dateTime) {
        return getData(dateTime)[0];
    }

    public float getPrice() {
        return price;
    }

    public float getClose(DateTime dateTime) {
        return getData(dateTime)[1];
    }

    public float getMin() {
        return min;
    }

    public float getMin(DateTime dateTime) {
        return getData(dateTime)[2];
    }

    public float getMax() {
        return max;
    }

    public float getMax(DateTime dateTime) {
        return getData(dateTime)[3];
    }


    public Container<Double> getClosePercent() {
        return closePercent;
    }

    public DateTime getOpenTime() {
        return openTime;
    }

    public Container<Double> getSaveProgress() {
        return saveProgress;
    }

    public boolean isSaving() {
        return saving;
    }

    public PriceSource getLivePriceSource() {
        return livePriceSource;
    }

    public HashMap<Long, float[]> getHistoricalData() {
        return dataHash;
    }

    public boolean isExtendedHours() {
        return extendedHours;
    }

    public DownloadTimeFrame getTimeFrame() {
        return timeFrame;
    }

    public String getTicker() {
        return ticker;
    }


    public enum PriceSource {
        MARKETWATCH("MarketWatch"),
        YAHOOFINANCE("YahooFinance");

        private final String string;

        PriceSource(String string) {
            this.string = string;
        }

        public String getString() {
            return string;
        }

        @Override
        public String toString() {
            return getString();
        }
    }

}
