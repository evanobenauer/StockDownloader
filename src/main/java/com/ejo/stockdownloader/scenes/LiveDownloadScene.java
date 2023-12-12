package com.ejo.stockdownloader.scenes;

import com.ejo.glowlib.math.MathE;
import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.setting.Container;
import com.ejo.glowlib.time.StopWatch;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ProgressBarUI;
import com.ejo.glowui.scene.elements.SideBarUI;
import com.ejo.glowui.scene.elements.TextUI;
import com.ejo.glowui.scene.elements.widget.ButtonUI;
import com.ejo.glowui.scene.elements.widget.SliderUI;
import com.ejo.glowui.util.Util;
import com.ejo.glowui.util.render.QuickDraw;
import com.ejo.stockdownloader.App;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.util.DownloadDrawUtil;
import com.ejo.stockdownloader.util.StockUtil;
import com.ejo.stockdownloader.render.CandleUI;

import java.awt.*;

public class LiveDownloadScene extends Scene {

    private final Stock stock;

    private final SideBarUI bottomBar;

    private final ProgressBarUI<Double> candlePercent;
    private final TextUI dateTime;
    private final SliderUI<Integer> secAdjust;

    private final SliderUI<Double> scaleSlider;
    private final Container<Double> scaleY;

    private final ButtonUI saveButton;
    private final ButtonUI exitButton;

    private final StopWatch saveWatch = new StopWatch();
    private final StopWatch forceFrameWatch = new StopWatch();

    public LiveDownloadScene(Stock stock) {
        super("Candle Scene");
        this.stock = stock;
        this.scaleY = new Container<>(200d);

        App.getWindow().setEconomic(true);

        addElements(
                saveButton = new ButtonUI("Force Save", Vector.NULL, new Vector(100, 40), new ColorE(0, 125, 200, 200), ButtonUI.MouseButton.LEFT,() -> {
                    Thread thread = new Thread(() -> System.out.println(stock.saveHistoricalData() ? "Saved" : "Could Not Save"));
                    thread.setDaemon(true);
                    thread.start();
                }),
                exitButton = new ButtonUI(Vector.NULL, new Vector(15, 15), new ColorE(200, 0, 0, 255), ButtonUI.MouseButton.LEFT, () -> getWindow().setScene(new TitleScene())),
                bottomBar = new SideBarUI(SideBarUI.Type.BOTTOM, 60,true,new ColorE(25, 25, 25, 255),
                        candlePercent = new ProgressBarUI<>(new Vector(10,22), new Vector(200, 20), ColorE.BLUE, stock.getClosePercent(), 0, 1),
                        scaleSlider = new SliderUI<>("Scale", Vector.NULL, new Vector(200, 22), ColorE.BLUE, scaleY, 1d, 2000d, 10d, SliderUI.Type.FLOAT, true),
                        secAdjust = new SliderUI<>("Seconds", new Vector(145, 8), new Vector(65, 10), ColorE.BLUE, StockUtil.SECOND_ADJUST, -10, 10, 1, SliderUI.Type.INTEGER, true),
                        dateTime = new TextUI(String.valueOf(StockUtil.getAdjustedCurrentTime()),new Font("Arial",Font.PLAIN,14),candlePercent.getPos().getAdded(0,-18),ColorE.WHITE))
        );
    }

    @Override
    public void draw() {
        //Draw Background
        drawBackground(new ColorE(50, 50, 50, 255));

        if (stock.shouldUpdate()) {
            //Draw All Candles
            double candleWidth = 14;
            double candleSpace = 4;
            double focusY = getSize().getY() / 2;
            double focusPrice = stock.getPrice();
            Vector candleScale = new Vector(1, scaleY.get());
            DownloadDrawUtil.drawDownloadCandles(this,stock,StockUtil.getAdjustedCurrentTime(),focusPrice,focusY,candleSpace,candleWidth,candleScale);

            double linePriceBoxHeight = 15;

            //Draw Live Price Line
            CandleUI liveCandleColor = new CandleUI(stock, 0, 0,0, 0, Vector.NULL);
            drawPriceLine(stock.getPrice(),getSize().getY() / 2,linePriceBoxHeight,new ColorE(liveCandleColor.getColor().getRed(), liveCandleColor.getColor().getGreen(), liveCandleColor.getColor().getBlue(), 200));

            //Draw Hover Price Line and Tag
            double yPrice = (focusY - getWindow().getScaledMousePos().getY()) / candleScale.getY() + focusPrice;
            drawPriceLine(yPrice,getWindow().getScaledMousePos().getY(),linePriceBoxHeight,ColorE.GRAY);
        } else {
            //Draw Waiting Text
            QuickDraw.drawTextCentered("Waiting for next candle!", new Font("Arial",Font.PLAIN,20),Vector.NULL,getSize(),ColorE.WHITE);
        }

        //Draw Stock Ticker
        QuickDraw.drawText("Downloading: " + stock.getTicker() + "-" + stock.getTimeFrame().getTag(), new Font("Arial", Font.PLAIN, 10), new Vector(16, 1), ColorE.WHITE);

        super.draw();

        //Draw X for Exit Button
        QuickDraw.drawText("X",new Font("Arial",Font.PLAIN,14),exitButton.getPos().getAdded(3,0),ColorE.WHITE);
    }

    @Override
    public void tick() {
        super.tick();

        //Update anchored elements
        exitButton.setPos(new Vector(0, 0));
        saveButton.setPos(new Vector(getSize().getX() - saveButton.getSize().getX() - 4, 5));
        scaleSlider.setPos(new Vector(getSize().getX() - scaleSlider.getSize().getX() - 10, 20));

        //Update Current Time Text
        dateTime.setText(String.valueOf(StockUtil.getAdjustedCurrentTime()));

        //Update all stock data
        stock.updateLiveData(0.5,true);

        //Save stored data every minute
        saveWatch.start();
        if (saveWatch.hasTimePassedS(60)) {
            System.out.println("Trying to save data...");
            Thread thread = new Thread(() -> System.out.println(stock.saveHistoricalData() ? "Saved" : "Could Not Save"));
            thread.setDaemon(true);
            thread.start();
            saveWatch.restart();
        }

        //Forces the program to run at 2fps even in economic mode
        if (StockUtil.isPriceActive(stock.isExtendedHours(),StockUtil.getAdjustedCurrentTime())) {
            forceFrameWatch.start();
            if (forceFrameWatch.hasTimePassedS(.5)) {
                Util.forceRenderFrame();
                forceFrameWatch.restart();
            }
        } else {
            forceFrameWatch.stop();
        }
    }

    private void drawPriceLine(double value, double y, double boxHeight, ColorE color) {
        QuickDraw.drawRect(new Vector(0, y), new Vector(getSize().getX(), .5), color);
        QuickDraw.drawRect(new Vector(0, y - boxHeight / 2), new Vector(36, boxHeight), color);
        QuickDraw.drawText(String.valueOf(MathE.roundDouble(value, 2)), new Font("Arial", Font.PLAIN, 10), new Vector(2, y - boxHeight / 2), ColorE.WHITE);
    }

}
