package com.ejo.stockdownloader.util;

import com.ejo.glowlib.setting.Container;
import com.ejo.glowlib.time.DateTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

public class DownloadStockUtil {

    private final static String WEB_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    public static float getWebScrapePrice(String url, String attributeKey, String attributeValue, int priceIndex) throws IOException {
        try {
            Document doc = Jsoup.connect(url).userAgent(WEB_USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.5")
                    .timeout(5 * 1000).get();
            Elements cssElements = doc.getElementsByAttributeValue(attributeKey, attributeValue);
            String priceString = cssElements.get(priceIndex).text().replace("$", "");
            return priceString.equals("") ? -1 : Float.parseFloat(priceString);
        } catch (IndexOutOfBoundsException e) {
            return -1;
        }
    }

    public static float getWebScrapePrice(String url, String cssQuery, int priceIndex) throws IOException {
        try {
            Document doc = Jsoup.connect(url).userAgent(WEB_USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.5")
                    .timeout(5 * 1000).get();
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

}
