package com.ejo.stockdownloader.scenes;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.misc.Container;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowlib.time.StopWatch;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ProgressBarUI;
import com.ejo.glowui.scene.elements.TextUI;
import com.ejo.glowui.scene.elements.widget.ButtonUI;
import com.ejo.glowui.scene.elements.widget.SliderUI;
import com.ejo.glowui.util.QuickDraw;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.util.StockUtil;
import com.ejo.stockdownloader.render.CandleUI;

import java.awt.*;
import java.util.ArrayList;

public class DownloadScene extends Scene {

    private final Stock stock;

    private final ProgressBarUI<Double> candlePercent;
    private final SliderUI<Double> scaleSlider;
    private final ButtonUI saveButton;
    private final ButtonUI exitButton;

    private final Container<Double> scaleY = new Container<>(200d);

    public DownloadScene(Stock stock) {
        super("Candle Scene");
        this.stock = stock;
        addElements(
                candlePercent = new ProgressBarUI<>(this, Vector.NULL, new Vector(200, 20), stock.getClosePercent(), 0, 1, ColorE.BLUE),
                scaleSlider = new SliderUI<>(this, "Scale", Vector.NULL, new Vector(200, 20), ColorE.BLUE, scaleY, 0d, 2000d, 10d, SliderUI.Type.FLOAT, true),
                saveButton = new ButtonUI(this, "Force Save", Vector.NULL, new Vector(100, 40), new ColorE(0, 125, 200, 200), stock::saveHistoricalData),
                exitButton = new ButtonUI(this, Vector.NULL, new Vector(15, 15), new ColorE(200, 0, 0, 255), () -> getWindow().setScene(new TitleScene()))
        );
    }

    @Override
    public void draw() {
        //Draw Background
        QuickDraw.drawRect(this, Vector.NULL, getWindow().getSize(), new ColorE(50, 50, 50, 255));

        //Draw Stock Ticker
        QuickDraw.drawText(this, "Downloading: " + stock.getTicker() + "-" + stock.getTimeFrame().getTag(), new Font("Arial", Font.PLAIN, 10), new Vector(1, 1), ColorE.WHITE);

        //Draw FPS/TPS
        QuickDraw.drawFPSTPS(this, new Vector(1, 11), 10, false);

        //Define Candle List
        ArrayList<CandleUI> candleList = new ArrayList<>();

        //Define Candle Attributes
        double width = 14;
        double spacing = 4;
        Vector scale = new Vector(1, scaleY.get());

        //Create Live Candle
        CandleUI currentCandle = new CandleUI(this, stock, getWindow().getSize().getX() - width - spacing, getWindow().getSize().getY() / 2, stock.getPrice(), width, scale);
        candleList.add(currentCandle);

        //Draw Price Line
        QuickDraw.drawRect(this, new Vector(0, getWindow().getSize().getY() / 2), new Vector(getWindow().getSize().getX(), .5), new ColorE(currentCandle.getColor().getRed(), currentCandle.getColor().getGreen(), currentCandle.getColor().getBlue(), 70));

        //Draw Cross hair Soon
        //QuickDraw.drawRect();

        //Update Segmentation to prevent spaced transition
        if (stock.shouldOpen()) stock.updateSegmentation();

        //Create Historical Candles
        DateTime ct = StockUtil.getAdjustedCurrentTime();
        for (int i = 1; i < (int) (getWindow().getSize().getX() / 18) + 1; i++) {
            DateTime time = new DateTime(ct.getYearInt(), ct.getMonthInt(), ct.getDayInt(), ct.getHourInt(), ct.getMinuteInt(), stock.getStartTime().getSecondInt() - stock.getTimeFrame().getSeconds() * i);
            CandleUI candle = new CandleUI(this, stock, time, currentCandle.getPos().getX() - (spacing + width) * i, getWindow().getSize().getY() / 2, stock.getPrice(), width, scale);
            candleList.add(candle);
        }

        //Draw and Tick All Candles
        for (CandleUI candle : candleList) {
            if (candle.getStock().getOpen() == -1) continue;
            candle.draw();
            candle.tick(); //For Mouse Over Data
            if (candle.isMouseOver()) {
                int size = 10;
                int x = 2;
                int y = (int) (getWindow().getSize().getY() - 105);
                QuickDraw.drawText(this, "Open:" + stock.getOpen(candle.getDateTime()), new Font("Arial", Font.PLAIN, size), new Vector(x, y), ColorE.WHITE);
                QuickDraw.drawText(this, "Close:" + stock.getClose(candle.getDateTime()), new Font("Arial", Font.PLAIN, size), new Vector(x, y + size), ColorE.WHITE);
                QuickDraw.drawText(this, "Min:" + stock.getMin(candle.getDateTime()), new Font("Arial", Font.PLAIN, size), new Vector(x, y + size * 2), ColorE.WHITE);
                QuickDraw.drawText(this, "Max:" + stock.getMax(candle.getDateTime()), new Font("Arial", Font.PLAIN, size), new Vector(x, y + size * 3), ColorE.WHITE);
            }
        }

        //Draw Waiting Text
        if (!stock.shouldUpdate()) {
            new TextUI(this, "Waiting for next candle!", "Arial", 20, Vector.NULL, ColorE.WHITE).drawCentered(getWindow().getSize());
        }

        //Draw Bottom Bar
        QuickDraw.drawRect(this, new Vector(0, getWindow().getSize().getY() - 60), new Vector(getWindow().getSize().getX(), 60), new ColorE(25, 25, 25, 255));

        super.draw();

        //Draw X for Exit Button
        QuickDraw.drawText(this,"X",new Font("Arial",Font.PLAIN,14),exitButton.getPos().getAdded(3,0),ColorE.WHITE);

        //Draw Current Time
        QuickDraw.drawText(this, String.valueOf(StockUtil.getAdjustedCurrentTime()),new Font("Arial",Font.PLAIN,14),candlePercent.getPos().getAdded(0,-15),ColorE.WHITE);

    }

    private final StopWatch saveWatch = new StopWatch();

    @Override
    public void tick() {
        super.tick();

        //Update anchored elements
        candlePercent.setPos(new Vector(10, getWindow().getSize().getY() - candlePercent.getSize().getY() - 20));
        scaleSlider.setPos(new Vector(getWindow().getSize().getX() - scaleSlider.getSize().getX() - 10, getWindow().getSize().getY() - scaleSlider.getSize().getY() - 20));
        saveButton.setPos(new Vector(getWindow().getSize().getX() - saveButton.getSize().getX() - 20, 10));
        exitButton.setPos(new Vector(getWindow().getSize().getX(), 0).getAdded(-exitButton.getSize().getX(), 0));

        //Update all stock data
        stock.updateData(0.5);

        //Save stored data every minute
        saveWatch.start();
        if (saveWatch.hasTimePassedS(60)) {
            if (stock.saveHistoricalData()) {
                System.out.println("Saved");
            } else {
                System.out.println("Could not save");
            }
            saveWatch.restart();
        }
    }
}
