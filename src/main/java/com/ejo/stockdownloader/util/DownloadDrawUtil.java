package com.ejo.stockdownloader.util;

import com.ejo.glowlib.math.MathE;
import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.util.render.QuickDraw;
import com.ejo.stockdownloader.data.LiveDownloadStock;
import com.ejo.stockdownloader.render.LiveDownloadCandle;

import java.awt.*;
import java.util.ArrayList;

public class DownloadDrawUtil {


    public static void drawDownloadCandles(Scene scene, LiveDownloadStock stock, DateTime endTime, double focusPrice, double focusY, double separation, double candleWidth, Vector candleScale) {

        //Define Candle List
        ArrayList<LiveDownloadCandle> listCandle = new ArrayList<>();

        DateTime openTime = endTime.equals(DownloadStockUtil.getAdjustedCurrentTime()) ? stock.getOpenTime() : endTime;

        //Create Historical Candles
        try {
            int candleAmount = (int) (scene.getSize().getX() / 18) + 1;
            for (int i = 0; i < candleAmount; i++) {
                double x = scene.getSize().getX() - ((separation + candleWidth) * (i + 1)) * candleScale.getX();
                DateTime candleTime = new DateTime(openTime.getYear(), openTime.getMonth(), openTime.getDay(), openTime.getHour(), openTime.getMinute(), openTime.getSecond() - stock.getTimeFrame().getSeconds() * i);
                LiveDownloadCandle historicalCandle = new LiveDownloadCandle(stock, candleTime, x, focusY, focusPrice, candleWidth * candleScale.getX(), new Vector(1, candleScale.getY()));
                listCandle.add(historicalCandle);
            }
        } catch (NullPointerException e) {
        }

        //Draw Candles
        for (LiveDownloadCandle candle : listCandle) {
            if (candle.getStock().getOpen(candle.getOpenTime()) != -1) candle.draw();
        }

        //Draw Tooltips
        for (LiveDownloadCandle candle : listCandle) {
            if (candle.getStock().getOpen(candle.getOpenTime()) != -1) {
                candle.tick(scene); //Update Mouse Over
                if (candle.isMouseOver()) drawCandleTooltip(candle, scene.getWindow().getScaledMousePos());
            }
        }

    }

    public static void drawCandleTooltip(LiveDownloadCandle candle, Vector mousePos) {
        LiveDownloadStock stock = candle.getStock();
        int textSize = 10;
        double x = mousePos.getX() - 96;
        double y = mousePos.getY() - textSize * 5 - 7;

        //Bound X Left
        if (x < 0) {
            x = 0;
            mousePos = new Vector(96, mousePos.getY());
        }

        //Bound Y Up
        if (y < 0) {
            y = 0;
            mousePos = new Vector(mousePos.getX(), textSize * 5 + 7);
        }

        //Round Data
        double open = MathE.roundDouble(stock.getOpen(candle.getOpenTime()), 2);
        double close = MathE.roundDouble(stock.getClose(candle.getOpenTime()), 2);
        double min = MathE.roundDouble(stock.getMin(candle.getOpenTime()), 2);
        double max = MathE.roundDouble(stock.getMax(candle.getOpenTime()), 2);

        //Draw Background
        QuickDraw.drawRect(new Vector(x - 2, y), new Vector(mousePos.getX() - x + 2, mousePos.getY() - y - 1), new ColorE(0, 125, 200, 200));

        //Draw Data
        QuickDraw.drawText(candle.getOpenTime().toString(), new Font("Arial", Font.PLAIN, textSize), new Vector(x, y), ColorE.WHITE);
        QuickDraw.drawText("Open:" + open, new Font("Arial", Font.PLAIN, textSize), new Vector(x, y + textSize), ColorE.WHITE);
        QuickDraw.drawText("Close:" + close, new Font("Arial", Font.PLAIN, textSize), new Vector(x, y + textSize * 2), ColorE.WHITE);
        QuickDraw.drawText("Min:" + min, new Font("Arial", Font.PLAIN, textSize), new Vector(x, y + textSize * 3), ColorE.WHITE);
        QuickDraw.drawText("Max:" + max, new Font("Arial", Font.PLAIN, textSize), new Vector(x, y + textSize * 4), ColorE.WHITE);

    }
}
