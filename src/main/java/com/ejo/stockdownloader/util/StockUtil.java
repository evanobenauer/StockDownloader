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
        try {
            Document doc = Jsoup.connect(url).userAgent(WEB_USER_AGENT).timeout(5 * 1000).get();
            Elements cssElements = doc.getElementsByAttributeValue(attributeKey, attributeValue);
            String priceString = cssElements.get(priceIndex).text().replace("$", "");
            return priceString.equals("") ? -1 : Float.parseFloat(priceString);
        } catch (IndexOutOfBoundsException e) {
            return -1;
        }
    }

    public static float getWebScrapePrice(String url, String cssQuery, int priceIndex) throws IOException {
        try {
            Document doc = Jsoup.connect(url).userAgent(WEB_USER_AGENT).timeout(5 * 1000).get();
            Elements cssElements = doc.select(cssQuery);
            String priceString = cssElements.get(priceIndex).text().replace("$", "");
            return priceString.equals("") ? -1 : Float.parseFloat(priceString);
        } catch (IndexOutOfBoundsException e) {
            return -1;
        }
    }

    public static boolean isTradingHours(DateTime currentTime) {
        return !currentTime.isWeekend() && currentTime.getHour() < 16 && currentTime.getHour() >= 9 && (currentTime.getHour() != 9 || currentTime.getMinute() >= 30);
    }

    public static boolean isPreMarket(DateTime currentTime) {
        return !isTradingHours(currentTime) && !currentTime.isWeekend() && currentTime.getHour() >= 4 && currentTime.getHour() < 10;
    }

    public static boolean isPostMarket(DateTime currentTime) {
        return !isTradingHours(currentTime) && !currentTime.isWeekend() && currentTime.getHour() >= 16 && currentTime.getHour() < 20;
    }

    public static boolean isPriceActive(boolean extendedHours, DateTime currentTime) {
        if (extendedHours) return isTradingHours(currentTime) || isPreMarket(currentTime) || isPostMarket(currentTime);
        return isTradingHours(currentTime);
    }

    public static DateTime getAdjustedCurrentTime() {
        DateTime ct = DateTime.getCurrentDateTime();
        return new DateTime(ct.getYear(), ct.getMonth(), ct.getDay(), ct.getHour(), ct.getMinute(), ct.getSecond() + SECOND_ADJUST.get());
    }

}
