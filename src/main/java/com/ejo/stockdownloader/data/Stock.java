package com.ejo.stockdownloader.data;

import com.ejo.glowlib.file.CSVManager;
import com.ejo.glowlib.misc.DoOnce;
import com.ejo.glowlib.setting.Container;
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
    private final boolean extendedHours;

    //Historical Data HashMap
    private HashMap<Long,String[]> dataHash;

    //Open Time
    private DateTime openTime;

    //Open-Close Percentage
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
    private final DoOnce doOpen = new DoOnce();
    private final DoOnce doClose = new DoOnce();

    //Default Constructor
    public Stock(String ticker, TimeFrame timeFrame, boolean extendedHours) {
        this.ticker = ticker;
        this.timeFrame = timeFrame;
        this.extendedHours = extendedHours;

        this.dataHash = loadHistoricalData();

        this.setAllData(-1);
        this.closePercent = new Container<>(0d);
        this.openTime = new DateTime(0,0,0);
        this.shouldStartUpdates = false;

        this.doFirstUpdate.reset();
        this.doLivePriceUpdate.reset();
        this.doOpen.reset();
        this.doClose.reset();
    }


    /**
     * This method updates the live price of the stock as well as the min and max. Depending on the timeframe, the stock will save data to the dataList periodically with this method
     * **METHOD PROCESS: Waits... [Time to close: Updates the close, updates the price, updates the open], Waits...**
     */
    public void updateData(double liveDelayS) {
        //Updates the progress bar of each segmentation
        if (StockUtil.isPriceActive(isExtendedHours(),StockUtil.getAdjustedCurrentTime())) updateClosePercent();

        //Check if the stock should update. If not, don't run the method
        if (!shouldUpdate()) return;

        //Set default values to the current price on the first update received
        this.doFirstUpdate.run(this::initLivePriceData);

        //Update the Close of each segment
        updateClose();

        //Update live price every provided delay second or update the live price on the start of every open
        updateLivePriceData(liveDelayS);

        //Update the Open of each segment
        //[If a force update every open is unwanted, remove the force update and add this inside the updateTimer after the updateLivePriceData()]
        updateOpen();
    }


    /**
     * Initiates the live price data from the stock
     */
    private void initLivePriceData() {
        try {
            setLivePrice();
            setAllData(getPrice());
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
     * Updates the live price data every timeframe specified in the liveDelay in seconds. The method will also force an update at the beginning of every open to make sure the stock
     * is up to date
     * @param liveDelayS
     */
    public void updateLivePriceData(double liveDelayS) {
        updateTimer.start();
        if (updateTimer.hasTimePassedS(liveDelayS) || shouldClose()) {
            doLivePriceUpdate.run(() -> {
                updateLivePriceData();
                updateTimer.restart();
            });
        }

        //Have live price updates reset if the stock should not close to continue with the liveDelay. This is so the stock will FORCE an update each open. Shown above
        if (!shouldClose()) doLivePriceUpdate.reset();
    }


    /**
     * Sets the stock's open, min, and max to the current price value only when doOpen is set to reset
     */
    public void updateOpen() {
        this.doOpen.run(() -> {
            this.openTime = StockUtil.getAdjustedCurrentTime();
            setAllData(getPrice());
        });
    }


    /**
     * Updates the splitting of the stock into candles based on the TimeFrame of the stock selected. This method adds an entry to the historical data HashMap and then resets the livedata to the current price
     */
    public void updateClose() {
        if (!shouldClose()) {
            doClose.reset();
            return;
        }
        this.doClose.run(() -> {
            DateTime ct = StockUtil.getAdjustedCurrentTime();
            //Save Live Data as Historical [Data is stored as (DATETIME,OPEN,CLOSE,MIN,MAX)]
            String[] timeFrameData = {String.valueOf(getOpen()), String.valueOf(getPrice()), String.valueOf(getMin()), String.valueOf(getMax())};
            DateTime previousOpen = new DateTime(ct.getYearInt(), ct.getMonthInt(), ct.getDayInt(), ct.getHourInt(), ct.getMinuteInt(), ct.getSecondInt() - getTimeFrame().getSeconds());
            if (getPrice() != -1) dataHash.put(previousOpen.getDateTimeID(), timeFrameData);

            //Set stock ready for open
            setAllData(getPrice());
            doOpen.reset();
        });
    }


    /**
     * Updates the percentage complete for the current stock candle
     */
    public void updateClosePercent() {
        DateTime ct = StockUtil.getAdjustedCurrentTime();
        double totalPercent = 0;

        //Second Percent
        double secPercent = (double) ct.getSecondInt() / getTimeFrame().getSeconds();
        totalPercent += secPercent;

        //Minute Percent
        double minPercent = ct.getMinuteInt() / ((double) getTimeFrame().getSeconds() / 60);
        totalPercent += minPercent;

        //Hour Percent
        double hrPercent = ct.getHourInt() / ((double) getTimeFrame().getSeconds() / 60 / 60);
        totalPercent += hrPercent;

        totalPercent -= Math.floor(totalPercent);
        getClosePercent().set(totalPercent);
    }


    /**
     * Sets all the data pertaining to the stock to a single value. This includes the price, open, min, and max
     * @param value
     */
    public void setAllData(float value) {
        this.price = value;
        this.open = value;
        this.min = value;
        this.max = value;
    }


    /**
     * Sets the live price data from the yahoo finance json data
     * @throws IOException
     * @throws JSONException
     */
    @SuppressWarnings("All")
    private void setLivePrice() throws IOException, JSONException {
        JSONObject liveData = StockUtil.getYahooFinanceJsonData(getTicker(),6);
        DateTime t = StockUtil.getAdjustedCurrentTime();
        if (StockUtil.isTradingHours(t)) this.price = liveData.getJSONObject("regularMarketPrice").getFloat("raw");
        if (StockUtil.isPostMarket(t))this.price = liveData.getJSONObject("postMarketPrice").getFloat("raw");
        if (StockUtil.isPreMarket(t))this.price = liveData.getJSONObject("preMarketPrice").getFloat("raw");
        //this.volume = liveData.getJSONObject("regularMarketVolume").getInt("raw");
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
                if (isExtendedHours()) {
                    convertedMap.put(Long.parseLong(key), rawMap.get(key));
                } else if (StockUtil.isTradingHours(new DateTime(Long.parseLong(key)))) {
                    convertedMap.put(Long.parseLong(key), rawMap.get(key));
                }
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
     * Checks if the stock should update live data. This method has the main purpose of stopping the update method if returned false
     * @return
     */
    public boolean shouldUpdate() {
        //Wait until the start of the candle timeframe to allow updates
        if (shouldClose()) setUpdatesStarted(true);
        if (!shouldStartUpdates()) return false;

        //Only allows for data collection during trading hours
        return StockUtil.isPriceActive(isExtendedHours(),StockUtil.getAdjustedCurrentTime());

        //Finally, if all checks pass,
        //return true;
    }

    @SuppressWarnings("All")
    private void setUpdatesStarted(boolean shouldStart) {
        this.shouldStartUpdates = shouldStart;
    }

    private boolean shouldStartUpdates() {
        return shouldStartUpdates;
    }


    /**
     * This method will return true if the stock is at a place to go through with a split depending on the current TimeFrame
     * @return
     */
    public boolean shouldClose() {
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
                if (dateTime.equals(getOpenTime())) return getOpen();
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
                if (dateTime.equals(getOpenTime())) return getPrice();
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
                if (dateTime.equals(getOpenTime())) return getMin();
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
                if (dateTime.equals(getOpenTime())) return getMax();
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

    public DateTime getOpenTime() {
        return openTime;
    }

    public HashMap<Long, String[]> getHistoricalData() {
        return dataHash;
    }

    public boolean isExtendedHours() {
        return extendedHours;
    }

    public TimeFrame getTimeFrame() {
        return timeFrame;
    }

    public String getTicker() {
        return ticker;
    }

}
