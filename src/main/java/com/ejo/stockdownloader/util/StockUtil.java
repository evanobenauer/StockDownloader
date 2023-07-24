package com.ejo.stockdownloader.util;

import com.ejo.glowlib.misc.Container;
import com.ejo.glowlib.time.DateTime;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class StockUtil {

    public static final Container<Integer> SECOND_ADJUST = new Container<>(-2);

    /**
     * This method downloads live stock data from Yahoo Finance. This is how we get the live data for the stock and proceed with downloading our numbers. "raw" is the raw data, "fmt" is the formatted data
     * @return
     * @throws IOException
     */
    public static JSONObject getYahooFinanceJsonData(String stockTicker) throws IOException, JSONException {
        //This uses the YahooFinance API to get the live stock price
        //Yahoo Finance will sometimes return: "Too Many Requests".
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String version = "v6";
        String url = "https://query1.finance.yahoo.com/" + version + "/finance/quoteSummary/" + stockTicker + "?modules=price";
        HttpGet httpGet = new HttpGet(url);
        HttpResponse response = httpClient.execute(httpGet); //This causes lots of lag

        String jsonString = EntityUtils.toString(response.getEntity());
        JSONObject jsonObject = new JSONObject(jsonString).getJSONObject("quoteSummary");
        String resultJsonString = jsonObject.get("result").toString().replace("[", "").replace("]", "");
        JSONObject priceObject = new JSONObject(resultJsonString).getJSONObject("price");

        return priceObject;
    }

    public static boolean isTradingHours(DateTime currentTime) {
        return !currentTime.isWeekend() && currentTime.getHourInt() < 16 && currentTime.getHourInt() >= 9 && (currentTime.getHourInt() != 9 || currentTime.getMinuteInt() >= 30);
    }

    public static DateTime getAdjustedCurrentTime() {
        DateTime ct = DateTime.getCurrentDateTime();
        return new DateTime(ct.getYearInt(), ct.getMonthInt(), ct.getDayInt(), ct.getHourInt(),ct.getMinuteInt(),ct.getSecondInt() + SECOND_ADJUST.get());
    }

}
