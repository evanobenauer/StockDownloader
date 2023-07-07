package com.ejo.stockdownloader.util;

import com.ejo.glowlib.math.MathE;
import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowui.util.QuickDraw;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.render.CandleUI;

import java.awt.*;

public class StockDrawUtil {

    public static void drawCandleTooltip(CandleUI candle, Vector mousePos) {
        Stock stock = candle.getStock();
        int size = 10;
        double x = mousePos.getX() - 60;
        double y = mousePos.getY() - size * 4 - 5;

        if (x < 0) {
            x = 0;
            mousePos = new Vector(60,mousePos.getY());
        }

        if (y < 0) {
            y = 0;
            mousePos = new Vector(mousePos.getX(),size * 4 + 5);
        }

        double open = MathE.roundDouble(stock.getOpen(candle.getDateTime()), 2);
        double close = MathE.roundDouble(stock.getClose(candle.getDateTime()), 2);
        double min = MathE.roundDouble(stock.getMin(candle.getDateTime()), 2);
        double max = MathE.roundDouble(stock.getMax(candle.getDateTime()), 2);

        QuickDraw.drawRect(candle.getScene(), new Vector(x - 2, y), new Vector(mousePos.getX() - x + 2, mousePos.getY() - y - 1), new ColorE(0, 125, 200, 200));

        QuickDraw.drawText(candle.getScene(), "Open:" + open, new Font("Arial", Font.PLAIN, size), new Vector(x, y), ColorE.WHITE);
        QuickDraw.drawText(candle.getScene(), "Close:" + close, new Font("Arial", Font.PLAIN, size), new Vector(x, y + size), ColorE.WHITE);
        QuickDraw.drawText(candle.getScene(), "Min:" + min, new Font("Arial", Font.PLAIN, size), new Vector(x, y + size * 2), ColorE.WHITE);
        QuickDraw.drawText(candle.getScene(), "Max:" + max, new Font("Arial", Font.PLAIN, size), new Vector(x, y + size * 3), ColorE.WHITE);

    }
}
