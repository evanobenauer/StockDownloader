package com.ejo.stockdownloader.scenes;

import com.ejo.glowlib.math.MathE;
import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.misc.Container;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowlib.time.StopWatch;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ProgressBarUI;
import com.ejo.glowui.scene.elements.SideBarUI;
import com.ejo.glowui.scene.elements.TextUI;
import com.ejo.glowui.scene.elements.widget.ButtonUI;
import com.ejo.glowui.scene.elements.widget.SliderUI;
import com.ejo.glowui.util.QuickDraw;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.util.StockDrawUtil;
import com.ejo.stockdownloader.util.StockUtil;
import com.ejo.stockdownloader.render.CandleUI;

import java.awt.*;
import java.util.ArrayList;

//TODO: Add optional candle rendering mode. One mode is the current one, one mode is the current candle ONLY?
public class LiveDownloadScene extends Scene {

    private final Stock stock;

    private final ProgressBarUI<Double> candlePercent;
    private final SliderUI<Double> scaleSlider;
    private final SliderUI<Integer> secAdjust;
    private final ButtonUI saveButton;
    private final ButtonUI exitButton;
    private final TextUI dateTime;
    private final SideBarUI bottomBar;

    private final Container<Double> scaleY = new Container<>(200d);

    private final StopWatch saveWatch = new StopWatch();

    public LiveDownloadScene(Stock stock) {
        super("Candle Scene");
        this.stock = stock;

        addElements(
                saveButton = new ButtonUI("Force Save", Vector.NULL, new Vector(100, 40), new ColorE(0, 125, 200, 200), stock::saveHistoricalData),
                exitButton = new ButtonUI(Vector.NULL, new Vector(15, 15), new ColorE(200, 0, 0, 255), () -> getWindow().setScene(new TitleScene())),
                bottomBar = new SideBarUI(SideBarUI.Type.BOTTOM, 60,true,new ColorE(25, 25, 25, 255),
                        candlePercent = new ProgressBarUI<>(new Vector(10,22), new Vector(200, 20), ColorE.BLUE, stock.getClosePercent(), 0, 1),
                        scaleSlider = new SliderUI<>("Scale", Vector.NULL, new Vector(200, 22), ColorE.BLUE, scaleY, 0d, 2000d, 10d, SliderUI.Type.FLOAT, true),
                        secAdjust = new SliderUI<>("Seconds", new Vector(145, 8), new Vector(65, 10), ColorE.BLUE, StockUtil.SECOND_ADJUST, -10, 10, 1, SliderUI.Type.INTEGER, true),
                        dateTime = new TextUI(String.valueOf(StockUtil.getAdjustedCurrentTime()),new Font("Arial",Font.PLAIN,14),candlePercent.getPos().getAdded(0,-18),ColorE.WHITE))
        );
    }

    @Override
    public void draw() {
        //Draw Background
        QuickDraw.drawRect( Vector.NULL, getSize(), new ColorE(50, 50, 50, 255));

        //Draw Waiting Text
        if (!stock.shouldUpdate()) QuickDraw.drawTextCentered("Waiting for next candle!", new Font("Arial",Font.PLAIN,20),Vector.NULL,getSize(),ColorE.WHITE);

        //Draw All Candles
        double candleWidth = 14;
        double candleSpace = 4;
        double focusY = getSize().getY() / 2;
        double focusPrice = stock.getPrice();
        Vector candleScale = new Vector(1, scaleY.get());
        drawCandles(this,stock,focusPrice,focusY,candleSpace,candleWidth,candleScale);

        //Draw Price Line and Tag
        if (stock.shouldUpdate()) {
            double boxHeight = 15;

            //Draw Live Price Line
            CandleUI liveCandleColor = new CandleUI(stock, 0, 0,0, 0, Vector.NULL);
            drawPriceLine(stock.getPrice(),getSize().getY() / 2,boxHeight,new ColorE(liveCandleColor.getColor().getRed(), liveCandleColor.getColor().getGreen(), liveCandleColor.getColor().getBlue(), 200));

            //Draw Hover Price Line and Tag
            double yPrice = (focusY - getWindow().getScaledMousePos().getY()) / candleScale.getY() + focusPrice;
            drawPriceLine(yPrice,getWindow().getScaledMousePos().getY(),boxHeight,ColorE.GRAY);
        }

        //Draw Stock Ticker
        QuickDraw.drawText("Downloading: " + stock.getTicker() + "-" + stock.getTimeFrame().getTag(), new Font("Arial", Font.PLAIN, 10), new Vector(16, 1), ColorE.WHITE);

        //Draw FPS-TPS
        QuickDraw.drawFPSTPS(this, new Vector(1, 14), 10, false);

        super.draw();

        //Draw X for Exit Button
        QuickDraw.drawText("X",new Font("Arial",Font.PLAIN,14),exitButton.getPos().getAdded(3,0),ColorE.WHITE);
    }

    @Override
    public void tick() {
        super.tick();

        //Update Segmentation to prevent spaced transition
        if (stock.shouldClose()) stock.updateClose();

        //Update anchored elements
        exitButton.setPos(new Vector(0, 0));
        saveButton.setPos(new Vector(getSize().getX() - saveButton.getSize().getX() - 4, 5));
        scaleSlider.setPos(new Vector(getSize().getX() - scaleSlider.getSize().getX() - 10, 20));

        //Update Current Time Text
        dateTime.setText(String.valueOf(StockUtil.getAdjustedCurrentTime()));

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


    private void drawCandles(Scene scene, Stock stock, double focusPrice, double focusY, double candleSpace, double candleWidth, Vector candleScale) {
        //Define Candle List
        ArrayList<CandleUI> candleList = new ArrayList<>();

        //Create Live Candle
        CandleUI liveCandle = new CandleUI(stock, getSize().getX() - candleWidth - candleSpace, focusY, focusPrice, candleWidth, candleScale);
        candleList.add(liveCandle);

        //Create Historical Candles
        DateTime ct = StockUtil.getAdjustedCurrentTime();
        int candleAmount = (int) (getSize().getX() / 18) + 1;
        for (int i = 1; i < candleAmount; i++) {
            DateTime candleTime = new DateTime(ct.getYearInt(), ct.getMonthInt(), ct.getDayInt(), ct.getHourInt(), ct.getMinuteInt(), stock.getOpenTime().getSecondInt() - stock.getTimeFrame().getSeconds() * i);
            CandleUI historicalCandle = new CandleUI(stock, candleTime, liveCandle.getPos().getX() - (candleSpace + candleWidth) * i, focusY, focusPrice, candleWidth, candleScale);
            candleList.add(historicalCandle);
        }

        //Draw All Candles
        for (CandleUI candle : candleList) {
            if (candle.getStock().getOpen(candle.getDateTime()) != -1) {
                candle.quickDraw();
            }
        }

        //Draw Tooltip
        for (CandleUI candle : candleList) {
            if (candle.getStock().getOpen(candle.getDateTime()) != -1) {
                candle.quickTick(scene); //Update Mouse Over
                if (candle.isMouseOver()) StockDrawUtil.drawCandleTooltip(candle, getWindow().getScaledMousePos());
            }
        }
    }

    private void drawPriceLine(double value, double y, double boxHeight, ColorE color) {
        QuickDraw.drawRect(new Vector(0, y), new Vector(getSize().getX(), .5), color);
        QuickDraw.drawRect(new Vector(0, y - boxHeight / 2), new Vector(36, boxHeight), color);
        QuickDraw.drawText(String.valueOf(MathE.roundDouble(value, 2)), new Font("Arial", Font.PLAIN, 10), new Vector(2, y - boxHeight / 2), ColorE.WHITE);
    }

}
