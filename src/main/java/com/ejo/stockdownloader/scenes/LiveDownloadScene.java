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
import com.ejo.glowui.util.UIUtil;
import com.ejo.glowui.util.render.Fonts;
import com.ejo.glowui.util.render.QuickDraw;
import com.ejo.stockdownloader.App;
import com.ejo.stockdownloader.data.LiveDownloadStock;
import com.ejo.stockdownloader.util.DownloadDrawUtil;
import com.ejo.stockdownloader.util.DownloadStockUtil;
import com.ejo.stockdownloader.render.LiveDownloadCandle;

import java.awt.*;

public class LiveDownloadScene extends Scene {

    private final LiveDownloadStock stock;

    private final SideBarUI barBottomBar;

    private final ProgressBarUI<Double> progressBarCandlePercent;
    private final TextUI textDateTime;
    private final SliderUI<Integer> sliderSecAdjust;

    private final SliderUI<Double> sliderPriceScale;
    private final Container<Double> priceScale;

    private final ButtonUI buttonSave;
    private final ButtonUI buttonExit;

    private final StopWatch stopWatchSave = new StopWatch();
    private final StopWatch stopWatchForceFrame = new StopWatch();

    public LiveDownloadScene(LiveDownloadStock stock) {
        super("Candle Scene");
        this.stock = stock;
        this.priceScale = new Container<>(200d);

        App.getWindow().setEconomic(true);

        addElements(
                buttonSave = new ButtonUI("Force Save", Vector.NULL, new Vector(100, 40), new ColorE(0, 125, 200, 200), ButtonUI.MouseButton.LEFT,() -> {
                    Thread thread = new Thread(() -> System.out.println(stock.saveHistoricalData() ? "Saved" : "Could Not Save"));
                    thread.setDaemon(true);
                    thread.start();
                }),
                buttonExit = new ButtonUI(Vector.NULL, new Vector(15, 15), new ColorE(200, 0, 0, 255), ButtonUI.MouseButton.LEFT, () -> getWindow().setScene(new TitleScene())),
                barBottomBar = new SideBarUI(SideBarUI.Type.BOTTOM, 60,true,new ColorE(25, 25, 25, 255),
                        progressBarCandlePercent = new ProgressBarUI<>(new Vector(10,22), new Vector(200, 20), ColorE.BLUE, stock.getClosePercent(), 0, 1),
                        sliderPriceScale = new SliderUI<>("Scale", Vector.NULL, new Vector(200, 22), ColorE.BLUE, priceScale, 1d, 2000d, 10d, SliderUI.Type.FLOAT, true),
                        sliderSecAdjust = new SliderUI<>("Seconds", new Vector(145, 8), new Vector(65, 10), ColorE.BLUE, DownloadStockUtil.SECOND_ADJUST, -10, 10, 1, SliderUI.Type.INTEGER, true),
                        textDateTime = new TextUI(String.valueOf(DownloadStockUtil.getAdjustedCurrentTime()),new Font("Arial",Font.PLAIN,14), progressBarCandlePercent.getPos().getAdded(0,-18),ColorE.WHITE))
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
            Vector candleScale = new Vector(1, priceScale.get());
            DownloadDrawUtil.drawDownloadCandles(this,stock, DownloadStockUtil.getAdjustedCurrentTime(),focusPrice,focusY,candleSpace,candleWidth,candleScale);

            double linePriceBoxHeight = 15;

            //Draw Live Price Line
            ColorE liveCandleColor = new LiveDownloadCandle(stock, 0, 0,0, 0, Vector.NULL).getColor().alpha(200);
            drawPriceLine(stock.getPrice(),getSize().getY() / 2,linePriceBoxHeight,liveCandleColor);

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
        QuickDraw.drawText("X",new Font("Arial",Font.PLAIN,14), buttonExit.getPos().getAdded(3,-2),ColorE.WHITE);


        if (stock.isSaving()) {
            ProgressBarUI<Double> progressBar = new ProgressBarUI<>(Vector.NULL,new Vector(150,20),ColorE.BLUE,stock.getSaveProgress(),0,1);
            progressBar.setPos(new Vector(2,2));
            QuickDraw.drawRect(progressBar.getPos(),progressBar.getSize(),ColorE.BLACK);
            progressBar.draw();
            QuickDraw.drawText("Saving...", Fonts.getDefaultFont(18),progressBar.getPos().getAdded(4,-3),ColorE.WHITE);
        }
    }

    @Override
    public void tick() {
        super.tick();

        //Update anchored elements
        buttonExit.setPos(new Vector(0, 0));
        buttonSave.setPos(new Vector(getSize().getX() - buttonSave.getSize().getX() - 4, 5));
        sliderPriceScale.setPos(new Vector(getSize().getX() - sliderPriceScale.getSize().getX() - 10, 20));

        //Update Current Time Text
        textDateTime.setText(String.valueOf(DownloadStockUtil.getAdjustedCurrentTime()));

        //Update all stock data (Have this be either .5s or 1s depending on how often the internet can handle)
        stock.updateLiveData(1,true);

        //Save stored data every minute
        stopWatchSave.start();
        if (stopWatchSave.hasTimePassedS(5) && stock.shouldClose()) {
            System.out.println("Trying to save data...");
            Thread thread = new Thread(() -> System.out.println(stock.saveHistoricalData() ? "Saved" : "Could Not Save"));
            thread.setDaemon(true);
            thread.start();
            stopWatchSave.restart();
        }

        //Forces the program to run at 2fps even in economic mode
        if (DownloadStockUtil.isPriceActive(stock.isExtendedHours(), DownloadStockUtil.getAdjustedCurrentTime())) {
            stopWatchForceFrame.start();
            if (stopWatchForceFrame.hasTimePassedS(.5)) {
                UIUtil.forceEconRenderFrame();
                stopWatchForceFrame.restart();
            }
        } else {
            stopWatchForceFrame.stop();
        }
    }

    private void drawPriceLine(double value, double y, double boxHeight, ColorE color) {
        QuickDraw.drawRect(new Vector(0, y), new Vector(getSize().getX(), .5), color);
        QuickDraw.drawRect(new Vector(0, y - boxHeight / 2), new Vector(36, boxHeight), color);
        QuickDraw.drawText(String.valueOf(MathE.roundDouble(value, 2)), new Font("Arial", Font.PLAIN, 10), new Vector(2, y - boxHeight / 2), ColorE.WHITE);
    }

}
