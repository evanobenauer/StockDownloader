package com.ejo.stockdownloader.data;

import com.ejo.glowlib.file.CSVManager;
import com.ejo.glowlib.misc.Container;
import com.ejo.glowlib.misc.DoOnce;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowlib.time.StopWatch;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;

//TODO: Learn to Calculate Volume, then add to CSV
public class Stock {

    private HashMap<Long,String[]> dataHash;

    private final String ticker;
    private final TimeFrame timeFrame;

    private DateTime startTime;

    private final Container<Double> closePercent;

    private float price;
    private float open;
    private float min;
    private float max;

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

        //Set default values to the current price
        try {
            this.price = getLiveJSONData().getJSONObject("regularMarketPrice").getFloat("raw");
            this.open = price;
            this.min = price;
            this.max = price;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            System.out.println("Too Many Requests!");
        }
    }


    private boolean shouldUpdate = false; //Waits until the start of the timeframe to begin
    private final StopWatch updateTimer = new StopWatch();

    /**
     * This method updates the live price of the stock as well as the min and max. Depending on the timeframe, the stock will save data to the dataList periodically with this method
     */
    public void updateData(double liveDelayS) {
        //Only allows for data collection during trading day hours
        DateTime ct = DateTime.getCurrentDateTime();
        if (ct.isWeekend() || ct.getHourInt() >= 16 || ct.getHourInt() < 9 || (ct.getHourInt() == 9 && ct.getMinuteInt() < 30)) return;

        //Updates the progress bar of each segmentation
        updateSegmentationPercentage();

        //Wait until the start of the candle timeframe to save
        if (shouldClose()) {
            shouldUpdate = true;
            this.startTime = DateTime.getCurrentDateTime();
        }
        if (!shouldUpdate) return;

        //Update live price every .5 seconds, Force an update on the first second of every minute
        updateTimer.start();
        if (updateTimer.hasTimePassedS(liveDelayS) || DateTime.getCurrentDateTime().getSecondInt() == 0) {
            DoOnce.default2.run(() -> {
                try {
                    updateLivePriceData();
                    updateTimer.restart();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    System.out.println("Too Many Requests!");
                }
            });
        }
        if (DateTime.getCurrentDateTime().getSecondInt() != 0) DoOnce.default2.reset();

        updateSegmentation();
    }


    /**
     * Retrieves and sets the live price data gathered for the stock. The minimum and maximum are set as well
     * @throws IOException
     */
    public void updateLivePriceData() throws IOException {
        JSONObject liveData = getLiveJSONData();
        this.price = liveData.getJSONObject("regularMarketPrice").getFloat("raw");
        //this.volume = liveData.getJSONObject("regularMarketVolume").getInt("raw");

        if (getPrice() < getMin()) this.min = getPrice();
        if (getPrice() > getMax()) this.max = getPrice();
    }

    /**
     * Updates the percentage complete for the current stock candle
     */
    public void updateSegmentationPercentage() {
        //TODO: Make segmentation percentage consistent across all time frames
        double sec = DateTime.getCurrentDateTime().getSecondInt();
        if (DateTime.getCurrentDateTime().getSecondInt() % getTimeFrame().getSeconds() == 0) sec *= ((double) getTimeFrame().getSeconds() / DateTime.getCurrentDateTime().getSecondInt());
        double percent = sec / getTimeFrame().getSeconds();
        percent -= Math.floor(percent);
        getClosePercent().set(percent);
    }

    /**
     * Updates the splitting of the stock into candles based on the TimeFrame of the stock selected. This method adds an entry to the historical data HashMap and then resets the livedata to the current price
     */
    public void updateSegmentation() {
        if (shouldClose()) {
            DoOnce.default1.run(() -> {
                DateTime ct = DateTime.getCurrentDateTime();
                //Save Live Data as Historical
                String[] timeFrameData = {String.valueOf(getOpen()),String.valueOf(getPrice()),String.valueOf(getMin()),String.valueOf(getMax())};
                DateTime previousOpen = new DateTime(ct.getYearInt(),ct.getMonthInt(),ct.getDayInt(),ct.getHourInt(),ct.getMinuteInt(),ct.getSecondInt() - getTimeFrame().getSeconds());
                dataHash.put(previousOpen.getDateTimeID(),timeFrameData);

                //Reset Live Data for next Candle
                this.startTime = ct;
                this.open = getPrice();
                this.min = getPrice();
                this.max = getPrice();
            });
        } else {
            DoOnce.default1.reset();
        }
    }

    /**
     * This method will return true if the stock is at a place to go through with a split depending on the current TimeFrame
     * @return
     */
    public boolean shouldClose() {
        DateTime currentTime = DateTime.getCurrentDateTime();
        return switch(getTimeFrame()) {
            case ONE_SECOND -> true;
            case FIVE_SECONDS -> currentTime.getSecondInt() % 5 == 0;
            case THIRTY_SECONDS -> currentTime.getSecondInt() % 30 == 0;
            case ONE_MINUTE -> currentTime.getSecondInt() == 0;
            case FIVE_MINUTES -> currentTime.getMinuteInt() % 5 == 0 && currentTime.getSecondInt() == 0;
            case FIFTEEN_MINUTES -> currentTime.getMinuteInt() % 15 == 0 && currentTime.getSecondInt() == 0;
            case THIRTY_MINUTES -> currentTime.getMinuteInt() % 30 == 0 && currentTime.getSecondInt() == 0;
            case ONE_HOUR -> currentTime.getHourInt() == 0 && currentTime.getMinuteInt() == 0 && currentTime.getSecondInt() == 0;
            case TWO_HOUR -> currentTime.getHourInt() % 2 == 0 && currentTime.getMinuteInt() == 0 && currentTime.getSecondInt() == 0;
            case FOUR_HOUR -> currentTime.getHourInt() % 4 == 0 && currentTime.getMinuteInt() == 0 && currentTime.getSecondInt() == 0;
            case ONE_DAY -> currentTime.getHourInt() % 8 == 0 && currentTime.getMinuteInt() == 0 && currentTime.getSecondInt() == 0;
        };
    }

    /**
     * This method downloads live stock data from Yahoo Finance. This is how we get the live data for the stock and proceed with downloading our numbers. "raw" is the raw data, "fmt" is the formatted data
     * @return
     * @throws IOException
     */
    private JSONObject getLiveJSONData() throws IOException {
        //This uses the YahooFinance API to get the live stock price
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String url = "https://query1.finance.yahoo.com/v10/finance/quoteSummary/" + getTicker() + "?modules=price";
        HttpGet httpGet = new HttpGet(url);
        HttpResponse response = httpClient.execute(httpGet); //This causes lots of lag

        //TODO: Find a new API. Yahoo Finance will sometimes return: "Too Many Requests" instead. Find a new way
        String jsonString = EntityUtils.toString(response.getEntity());
        JSONObject jsonObject = new JSONObject(jsonString).getJSONObject("quoteSummary");
        String resultJsonString = jsonObject.get("result").toString().replace("[", "").replace("]", "");
        JSONObject priceObject = new JSONObject(resultJsonString).getJSONObject("price");

        return priceObject;
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


    public enum TimeFrame {
        ONE_SECOND("1sec",1),
        FIVE_SECONDS("5sec",5),
        THIRTY_SECONDS("30sec",30),
        ONE_MINUTE("1min",60),
        FIVE_MINUTES("5min",5 * 60),
        FIFTEEN_MINUTES("15min", 15 * 60),
        THIRTY_MINUTES("30min", 30 * 60),
        ONE_HOUR("1hr", 60 * 60),
        TWO_HOUR("2hr", 2 * 60 * 60),
        FOUR_HOUR("4hr", 4 * 60 * 60),
        ONE_DAY("1day", 8 * 60 * 60);

        private final String tag;
        private final int seconds;

        TimeFrame(String tag, int seconds) {
            this.tag = tag;
            this.seconds = seconds;
        }

        public String getTag() {
            return tag;
        }

        public int getSeconds() {
            return seconds;
        }

        @Override
        public String toString() {
            return getTag();
        }
    }

}
