package com.ejo.stockdownloader.data;

import com.ejo.glowlib.file.CSVManager;
import com.ejo.glowlib.misc.Container;
import com.ejo.glowlib.misc.DoOnce;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowlib.time.StopWatch;
import com.ejo.stockdownloader.util.StockUtil;
import com.ejo.stockdownloader.util.TimeFrame;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;

/**
 * The stock class is a multi use class. It encompasses both loading historical data and adding new data to said history. The live data is updated
 * by a method whenever it is called
 */
public class Stock {

    //Stock Information
    private final String ticker;
    private final TimeFrame timeFrame;

    //Historical Data HashMap
    private HashMap<Long,String[]> dataHash;

    //Segmentation Start Time
    private DateTime startTime;

    //Segment finished percentage
    private final Container<Double> closePercent;

    //Live Price Variables
    private float price;
    private float open;
    private float min;
    private float max;

    //Live Price Update Variables
    private boolean shouldStartUpdates;
    private final StopWatch updateTimer = new StopWatch();

    //Do Once Definitions
    private final DoOnce doFirstUpdate = new DoOnce();
    private final DoOnce doLivePriceUpdate = new DoOnce();
    private final DoOnce doSegmentation = new DoOnce();

    //Default Constructor
    public Stock(String ticker, TimeFrame timeFrame) {
        //Set Stock Definition
        this.ticker = ticker;
        this.timeFrame = timeFrame;

        //Load Saved Historical Data
        this.dataHash = loadHistoricalData();

        //Set up the container value for the percent done with drawing a candle
        this.closePercent = new Container<>(0d);

        //Set the section start time
        this.startTime = new DateTime(0,0,0);

        //Set starting values for price, open, min, max
        this.price = -1;
        this.open = -1;
        this.min = -1;
        this.max = -1;

        //Doesn't allow updates until set updatable
        this.shouldStartUpdates = false;

        //Prepare first value set
        doFirstUpdate.reset();
        doLivePriceUpdate.reset();
        doSegmentation.reset();
    }


    /**
     * This method updates the live price of the stock as well as the min and max. Depending on the timeframe, the stock will save data to the dataList periodically with this method
     */
    public void updateData(double liveDelayS) {
        //Updates the progress bar of each segmentation
        if (StockUtil.isTradingHours(StockUtil.getAdjustedCurrentTime())) updateSegmentationPercentage();

        //Check if the stock should update. If not, don't run the method
        if (!shouldUpdate()) return;

        //Sets the starting time of the segment
        if (shouldOpen()) this.startTime = StockUtil.getAdjustedCurrentTime();

        //Set default values to the current price on the first update received
        this.doFirstUpdate.run(this::initPriceData);

        //Update live price every .5 seconds, Force an update on the first second of every minute
        updateTimer.start();
        if (updateTimer.hasTimePassedS(liveDelayS) || StockUtil.getAdjustedCurrentTime().getSecondInt() == 0) {
            doLivePriceUpdate.run(() -> {
                updateLivePriceData();
                updateTimer.restart();
            });
        }

        //Have live price updates reset if it is not the first second of every minute
        if (StockUtil.getAdjustedCurrentTime().getSecondInt() != 0) doLivePriceUpdate.reset();

        //Update stock segment splitting
        updateSegmentation();
    }


    /**
     * Initiates the live price data from the stock
     */
    private void initPriceData() {
        try {
            setLivePrice();
            this.open = price;
            this.min = price;
            this.max = price;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            System.out.println("Too Many Requests!");
        }
    }

    /**
     * Retrieves and sets the live price data gathered for the stock. The minimum and maximum are set as well
     * @throws IOException
     */
    public void updateLivePriceData() {
        try {
            setLivePrice();
            if (getPrice() < getMin()) this.min = getPrice();
            if (getPrice() > getMax()) this.max = getPrice();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            System.out.println("Too Many Requests!");
        }
    }

    /**
     * Updates the splitting of the stock into candles based on the TimeFrame of the stock selected. This method adds an entry to the historical data HashMap and then resets the livedata to the current price
     * TODO: The issue with segmentation is of when the next candle opens. For tradingview, the candle opens a tick AFTER the close, causing a difference
     *  MY data opens and closes at the same time, which is different. Tradingview closes at 59 and opens at 0. My data closes at 0 and opens at 0
     *
     */
    public void updateSegmentation() {
        if (shouldOpen()) {
            doSegmentation.run(() -> {
                DateTime ct = StockUtil.getAdjustedCurrentTime();
                //Save Live Data as Historical
                String[] timeFrameData = {String.valueOf(getOpen()),String.valueOf(getPrice()),String.valueOf(getMin()),String.valueOf(getMax())};
                DateTime previousOpen = new DateTime(ct.getYearInt(),ct.getMonthInt(),ct.getDayInt(),ct.getHourInt(),ct.getMinuteInt(),ct.getSecondInt() - getTimeFrame().getSeconds());
                if (getPrice() != -1) dataHash.put(previousOpen.getDateTimeID(),timeFrameData);

                //Reset Live Data for next Candle
                this.startTime = ct;
                this.open = getPrice();
                this.min = getPrice();
                this.max = getPrice();
            });
        } else {
            doSegmentation.reset();
        }
    }

    /**
     * Updates the percentage complete for the current stock candle
     */
    public void updateSegmentationPercentage() {
        //TODO: Fix for all timeframes over 1min
        DateTime ct = StockUtil.getAdjustedCurrentTime();
        double totalPercent = 0;

        //Second Percent
        double sec = ct.getSecondInt();
        if (ct.getSecondInt() % getTimeFrame().getSeconds() == 0) sec *= ((double) getTimeFrame().getSeconds() / ct.getSecondInt());
        double secPercent = sec / getTimeFrame().getSeconds();
        secPercent -= Math.floor(secPercent);
        totalPercent += secPercent;

        //Minute Percent
        //TODO: Add

        getClosePercent().set(totalPercent);
    }


    /**
     * This method loads all historical data saved in the data directory. It converts the key information of the hashmap data into a long to be used in development
     * @return
     */
    private HashMap<Long,String[]> loadHistoricalData() {
        try {
            HashMap<String, String[]> rawMap = CSVManager.getHMDataFromCSV("stock_data", getTicker() + "_" + getTimeFrame().getTag());

            HashMap<Long, String[]> convertedMap = new HashMap<>();
            for (String key : rawMap.keySet()) {
                convertedMap.put(Long.parseLong(key), rawMap.get(key));
            }
            return this.dataHash = convertedMap;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    /**
     * This method saves all historical data from the HashMap as a CSV file using GlowLib
     * @return
     */
    public boolean saveHistoricalData() {
        return CSVManager.saveAsCSV(getHistoricalData(), "stock_data", getTicker() + "_" + getTimeFrame().getTag());
    }


    /**
     * Sets the live price data from the yahoo finance json data
     * @throws IOException
     * @throws JSONException
     */
    private void setLivePrice() throws IOException, JSONException {
        //TODO: Learn to Calculate Volume, then add to CSV
        JSONObject liveData = StockUtil.getYahooFinanceJsonData(getTicker());
        this.price = liveData.getJSONObject("regularMarketPrice").getFloat("raw");
        //this.price = liveData.getJSONObject("postMarketPrice").getFloat("raw");
        //this.volume = liveData.getJSONObject("regularMarketVolume").getInt("raw");
    }

    private void setUpdatesStarted(boolean shouldStart) {
        this.shouldStartUpdates = shouldStart;
    }

    private boolean shouldStartUpdates() {
        return shouldStartUpdates;
    }

    /**
     * Checks if the stock should update live data. This method has the main purpose of stopping the update method if returned false
     * @return
     */
    public boolean shouldUpdate() {
        //Wait until the start of the candle timeframe to allow updates
        if (shouldOpen()) setUpdatesStarted(true);
        if (!shouldStartUpdates()) return false;

        //Only allows for data collection during trading day hours
        if (!StockUtil.isTradingHours(StockUtil.getAdjustedCurrentTime())) return false;

        //Finally, if all checks pass, return true
        return true;
    }

    /**
     * This method will return true if the stock is at a place to go through with a split depending on the current TimeFrame
     * @return
     */
    public boolean shouldOpen() {
        DateTime ct = StockUtil.getAdjustedCurrentTime();
        return switch(getTimeFrame()) {
            case ONE_SECOND -> true;
            case FIVE_SECONDS -> ct.getSecondInt() % 5 == 0;
            case THIRTY_SECONDS -> ct.getSecondInt() % 30 == 0;
            case ONE_MINUTE -> ct.getSecondInt() == 0;
            case FIVE_MINUTES -> ct.getMinuteInt() % 5 == 0 && ct.getSecondInt() == 0;
            case FIFTEEN_MINUTES -> ct.getMinuteInt() % 15 == 0 && ct.getSecondInt() == 0;
            case THIRTY_MINUTES -> ct.getMinuteInt() % 30 == 0 && ct.getSecondInt() == 0;
            case ONE_HOUR -> ct.getHourInt() == 0 && ct.getMinuteInt() == 0 && ct.getSecondInt() == 0;
            case TWO_HOUR -> ct.getHourInt() % 2 == 0 && ct.getMinuteInt() == 0 && ct.getSecondInt() == 0;
            case FOUR_HOUR -> ct.getHourInt() % 4 == 0 && ct.getMinuteInt() == 0 && ct.getSecondInt() == 0;
            case ONE_DAY -> ct.getHourInt() % 8 == 0 && ct.getMinuteInt() == 0 && ct.getSecondInt() == 0;
        };
    }

    public float getPrice() {
        return price;
    }

    public float getOpen() {
        return open;
    }

    public float getOpen(DateTime dateTime) {
        try {
            if (dateTime == null) {
                return getOpen();
            } else {
                return Float.parseFloat(getHistoricalData().get(dateTime.getDateTimeID())[0]);
            }
        } catch (NullPointerException e) {
            return -1;
        }
    }

    public float getClose(DateTime dateTime) {
        try {
            if (dateTime == null) {
                return getPrice();
            } else {
                return Float.parseFloat(getHistoricalData().get(dateTime.getDateTimeID())[1]);
            }
        } catch (NullPointerException e) {
            return -1;
        }
    }

    public float getMin() {
        return min;
    }

    public float getMin(DateTime dateTime) {
        try {
            if (dateTime == null) {
                return getMin();
            } else {
                return Float.parseFloat(getHistoricalData().get(dateTime.getDateTimeID())[2]);
            }
        } catch (NullPointerException e) {
            return -1;
        }
    }

    public float getMax() {
        return max;
    }

    public float getMax(DateTime dateTime) {
        try {
            if (dateTime == null) {
                return getMax();
            } else {
                return Float.parseFloat(getHistoricalData().get(dateTime.getDateTimeID())[3]);
            }
        } catch (NullPointerException e) {
            return -1;
        }
    }


    /**
     * Retrieves the percentage from the open time to the close time. This is mainly used in the progress bar
     * @return
     */
    public Container<Double> getClosePercent() {
        return closePercent;
    }

    public DateTime getStartTime() {
        return startTime;
    }

    public HashMap<Long, String[]> getHistoricalData() {
        return dataHash;
    }

    public TimeFrame getTimeFrame() {
        return timeFrame;
    }

    public String getTicker() {
        return ticker;
    }

}
