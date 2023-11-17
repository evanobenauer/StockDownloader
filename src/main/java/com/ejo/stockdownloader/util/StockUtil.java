package com.ejo.stockdownloader.util;

import com.ejo.glowlib.setting.Container;
import com.ejo.glowlib.time.DateTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class StockUtil {

    public static final Container<Integer> SECOND_ADJUST = new Container<>(0);

    private static final String WEB_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36";

    //TODO: This is very dependent on internet speed. Maybe put it on its own thread as not to mess up stuff
    public static float getWebScrapePrice(String url, String attributeKey, String attributeValue, int priceIndex) throws IOException {
        Document doc = Jsoup.connect(url).userAgent(WEB_USER_AGENT).timeout(5 * 1000).get();
        Elements cssElements = doc.getElementsByAttributeValue(attributeKey, attributeValue);
        String priceString = cssElements.get(priceIndex).text().replace("$","");
        return priceString.equals("") ? -1 : Float.parseFloat(priceString);
    }

    public static float getWebScrapePrice(String url, String cssQuery, int priceIndex) throws IOException {
        Document doc = Jsoup.connect(url).userAgent(WEB_USER_AGENT).timeout(5 * 1000).get();
        Elements cssElements = doc.select(cssQuery);
        String priceString = cssElements.get(priceIndex).text().replace("$","");
        return priceString.equals("") ? -1 : Float.parseFloat(priceString);
    }

    public static boolean isTradingHours(DateTime currentTime) {
        return !currentTime.isWeekend() && currentTime.getHourInt() < 16 && currentTime.getHourInt() >= 9 && (currentTime.getHourInt() != 9 || currentTime.getMinuteInt() >= 30);
    }

    public static boolean isPreMarket(DateTime currentTime) {
        return !isTradingHours(currentTime) && !currentTime.isWeekend() && currentTime.getHourInt() >= 4 && currentTime.getHourInt() < 10;
    }

    public static boolean isPostMarket(DateTime currentTime) {
        return !isTradingHours(currentTime) && !currentTime.isWeekend() && currentTime.getHourInt() >= 16 && currentTime.getHourInt() < 20;
    }

    public static boolean isPriceActive(boolean extendedHours, DateTime currentTime) {
        if (extendedHours) return isTradingHours(currentTime) || isPreMarket(currentTime) || isPostMarket(currentTime);
        return isTradingHours(currentTime);
    }

    public static DateTime getAdjustedCurrentTime() {
        DateTime ct = DateTime.getCurrentDateTime();
        return new DateTime(ct.getYearInt(), ct.getMonthInt(), ct.getDayInt(), ct.getHourInt(),ct.getMinuteInt(),ct.getSecondInt() + SECOND_ADJUST.get());
    }

}
