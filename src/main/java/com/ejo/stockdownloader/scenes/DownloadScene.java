package com.ejo.stockdownloader.scenes;

import com.ejo.glowlib.math.MathE;
import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.misc.Container;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowlib.time.StopWatch;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ProgressBarUI;
import com.ejo.glowui.scene.elements.widget.ButtonUI;
import com.ejo.glowui.scene.elements.widget.SliderUI;
import com.ejo.glowui.util.QuickDraw;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.util.StockDrawUtil;
import com.ejo.stockdownloader.util.StockUtil;
import com.ejo.stockdownloader.render.CandleUI;

import java.awt.*;
import java.util.ArrayList;

public class DownloadScene extends Scene {

    //TODO: Add optional candle rendering mode. One mode is the current one, one mode is the current candle ONLY?

    private final Stock stock;

    private final ProgressBarUI<Double> candlePercent;
    private final SliderUI<Double> scaleSlider;
    private final SliderUI<Integer> secAdjust;
    private final ButtonUI saveButton;
    private final ButtonUI exitButton;

    private final Container<Double> scaleY = new Container<>(200d);

    public DownloadScene(Stock stock) {
        super("Candle Scene");
        this.stock = stock;
        addElements(
                candlePercent = new ProgressBarUI<>(this, Vector.NULL, new Vector(200, 20), stock.getClosePercent(), 0, 1, ColorE.BLUE),
                scaleSlider = new SliderUI<>(this, "Scale", Vector.NULL, new Vector(200, 20), ColorE.BLUE, scaleY, 0d, 2000d, 10d, SliderUI.Type.FLOAT, true),
                secAdjust = new SliderUI<>(this, "Seconds",Vector.NULL, new Vector(65, 10), ColorE.BLUE, StockUtil.SECOND_ADJUST, -10, 10, 1, SliderUI.Type.INTEGER, true),
                saveButton = new ButtonUI(this, "Force Save", Vector.NULL, new Vector(100, 40), new ColorE(0, 125, 200, 200), stock::saveHistoricalData),
                exitButton = new ButtonUI(this, Vector.NULL, new Vector(15, 15), new ColorE(200, 0, 0, 255), () -> getWindow().setScene(new TitleScene()))
        );
    }

    @Override
    public void draw() {
        //Draw Background
        QuickDraw.drawRect(this, Vector.NULL, getWindow().getSize(), new ColorE(50, 50, 50, 255));

        //Define Candle List
        ArrayList<CandleUI> candleList = new ArrayList<>();

        //Define Candle Attributes
        double width = 14;
        double spacing = 4;
        Vector scale = new Vector(1, scaleY.get());

        //Create Live Candle
        CandleUI currentCandle = new CandleUI(this, stock, getWindow().getSize().getX() - width - spacing, getWindow().getSize().getY() / 2, stock.getPrice(), width, scale);
        candleList.add(currentCandle);

        //Create Historical Candles
        DateTime ct = StockUtil.getAdjustedCurrentTime();
        for (int i = 1; i < (int) (getWindow().getSize().getX() / 18) + 1; i++) { //Causes High Power Usage
            DateTime time = new DateTime(ct.getYearInt(), ct.getMonthInt(), ct.getDayInt(), ct.getHourInt(), ct.getMinuteInt(), stock.getStartTime().getSecondInt() - stock.getTimeFrame().getSeconds() * i);
            CandleUI candle = new CandleUI(this, stock, time, currentCandle.getPos().getX() - (spacing + width) * i, getWindow().getSize().getY() / 2, stock.getPrice(), width, scale);
            candleList.add(candle);
        }

        //Draw and Tick All Candles
        for (CandleUI candle : candleList) {
            if (candle.getStock().getOpen(candle.getDateTime()) == -1) continue;
            candle.draw();
            candle.tick(); //For Mouse Over Data
        }

        //Draw Tooltip
        for (CandleUI candle : candleList) {
            if (candle.getStock().getOpen(candle.getDateTime()) == -1) continue;
            if (candle.isMouseOver()) StockDrawUtil.drawCandleTooltip(candle,getWindow().getMousePos());
        }

        //Draw Price Line and Tag
        if (stock.shouldUpdate()) {
            QuickDraw.drawRect(this, new Vector(0, getWindow().getSize().getY() / 2), new Vector(getWindow().getSize().getX(), .5), new ColorE(currentCandle.getColor().getRed(), currentCandle.getColor().getGreen(), currentCandle.getColor().getBlue(), 70));
            double height = 15;
            QuickDraw.drawRect(this, new Vector(0, getWindow().getSize().getY() / 2 - height / 2), new Vector(36, height), new ColorE(currentCandle.getColor().getRed(), currentCandle.getColor().getGreen(), currentCandle.getColor().getBlue(), 200));
            QuickDraw.drawText(this, String.valueOf(MathE.roundDouble(stock.getPrice(), 2)), new Font("Arial", Font.PLAIN, 10), new Vector(2, getWindow().getSize().getY() / 2 - height / 2), ColorE.WHITE);

            //Draw Hover Price Line and Tag
            QuickDraw.drawRect(this, new Vector(0, getWindow().getMousePos().getY()), new Vector(getWindow().getSize().getX(), .5), ColorE.GRAY);
            QuickDraw.drawRect(this, new Vector(0, getWindow().getMousePos().getY() - height / 2), new Vector(36, height), new ColorE(125, 125, 125, 200));
            double yPrice = (currentCandle.getFocusY() - getWindow().getMousePos().getY()) / currentCandle.getScale().getY() + currentCandle.getFocusPrice();
            QuickDraw.drawText(this, String.valueOf(MathE.roundDouble(yPrice, 2)), new Font("Arial", Font.PLAIN, 10), new Vector(2, getWindow().getMousePos().getY() - height / 2), ColorE.WHITE);
        }

        //Draw Waiting Text
        if (!stock.shouldUpdate()) {
            QuickDraw.drawTextCentered(this,"Waiting for next candle!", new Font("Arial",Font.PLAIN,20),Vector.NULL,getWindow().getSize(),ColorE.WHITE);
        }

        //Draw Bottom Bar
        QuickDraw.drawRect(this, new Vector(0, getWindow().getSize().getY() - 60), new Vector(getWindow().getSize().getX(), 60), new ColorE(25, 25, 25, 255));

        super.draw();

        //Draw Stock Ticker
        QuickDraw.drawText(this, "Downloading: " + stock.getTicker() + "-" + stock.getTimeFrame().getTag(), new Font("Arial", Font.PLAIN, 10), new Vector(16, 1), ColorE.WHITE);

        //Draw FPS/TPS
        QuickDraw.drawFPSTPS(this, new Vector(1, 14), 10, false);

        //Draw X for Exit Button
        QuickDraw.drawText(this,"X",new Font("Arial",Font.PLAIN,14),exitButton.getPos().getAdded(3,0),ColorE.WHITE);

        //Draw Current Time
        QuickDraw.drawText(this, String.valueOf(StockUtil.getAdjustedCurrentTime()),new Font("Arial",Font.PLAIN,14),candlePercent.getPos().getAdded(0,-15),ColorE.WHITE);

    }

    private final StopWatch saveWatch = new StopWatch();

    @Override
    public void tick() {
        super.tick();

        //Update Segmentation to prevent spaced transition
        if (stock.shouldOpen()) stock.updateSegmentation();

        //Update anchored elements
        candlePercent.setPos(new Vector(10, getWindow().getSize().getY() - candlePercent.getSize().getY() - 20));
        scaleSlider.setPos(new Vector(getWindow().getSize().getX() - scaleSlider.getSize().getX() - 10, getWindow().getSize().getY() - scaleSlider.getSize().getY() - 20));
        secAdjust.setPos(new Vector(145, getWindow().getSize().getY() - secAdjust.getSize().getY() - 42));
        saveButton.setPos(new Vector(getWindow().getSize().getX() - saveButton.getSize().getX() - 2, 5));
        exitButton.setPos(new Vector(0, 0));

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
