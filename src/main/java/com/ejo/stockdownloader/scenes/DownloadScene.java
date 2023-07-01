package com.ejo.stockdownloader.scenes;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.misc.Container;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowlib.time.StopWatch;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ProgressBarUI;
import com.ejo.glowui.scene.elements.widget.SliderUI;
import com.ejo.glowui.util.QuickDraw;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.render.CandleUI;

import java.awt.*;
import java.util.ArrayList;

public class DownloadScene extends Scene {

    private final Stock stock;

    private final ProgressBarUI<Double> candlePercent;
    private final SliderUI<Double> scaleSlider;

    private final Container<Double> scaleY = new Container<>(200d);

    public DownloadScene(Stock stock) {
        super("Candle Scene");
        this.stock = stock;
        addElements(
                candlePercent = new ProgressBarUI<>(this,Vector.NULL,new Vector(200,20), stock.getClosePercent(),0,1, ColorE.BLUE),
                scaleSlider = new SliderUI<>(this,"Scale",Vector.NULL,new Vector(200,20),ColorE.BLUE,scaleY,0d,2000d,10d, SliderUI.Type.FLOAT,true));
    }

    @Override
    public void draw() {
        //Draw Background
        QuickDraw.drawRect(this,Vector.NULL,getWindow().getSize(),new ColorE(50,50,50,255));

        //Draw FPS/TPS
        QuickDraw.drawFPSTPS(this,new Vector(1,1),15,false);

        //Define Candle List
        ArrayList<CandleUI> candleList = new ArrayList<>();

        //Define Candle Attributes
        double width = 14;
        double spacing = 4;
        Vector scale = new Vector(1,scaleY.get());

        //Create Live Candle
        CandleUI currentCandle = new CandleUI(this, stock,getWindow().getSize().getX() - width - spacing, getWindow().getSize().getY()/2, stock.getPrice(),width,scale);
        candleList.add(currentCandle);

        //Draw Price Line
        QuickDraw.drawRect(this,new Vector(0,getWindow().getSize().getY()/2),new Vector(getWindow().getSize().getX(),.5),new ColorE(currentCandle.getColor().getRed(),currentCandle.getColor().getGreen(),currentCandle.getColor().getBlue(),70));

        //Draw Cross hair Soon
        //QuickDraw.drawRect();

        //Update Segmentation to prevent spaced transition
        if (stock.shouldClose()) stock.updateSegmentation();

        //Create Historical Candles
        DateTime ct = DateTime.getCurrentDateTime();
        for (int i = 1; i < (int)(getWindow().getSize().getX() / 18) + 1; i++) {
            DateTime time = new DateTime(ct.getYearInt(),ct.getMonthInt(),ct.getDayInt(),ct.getHourInt(),ct.getMinuteInt(), stock.getStartTime().getSecondInt() - stock.getTimeFrame().getSeconds() * i);
            CandleUI candle = new CandleUI(this, stock, time, currentCandle.getPos().getX() - (spacing + width)*i, getWindow().getSize().getY()/2, stock.getPrice(), width, scale);
            candleList.add(candle);
        }

        //Draw and Tick All Candles
        for (CandleUI candle : candleList) {
            if (candle.getStock().getOpen() == -1) continue;
            candle.draw();
            candle.tick();
            if (candle.isMouseOver()) {
                int size = 15;
                int x = 25;
                QuickDraw.drawText(this,"Open:" + stock.getOpen(candle.getDateTime()), new Font("Arial", Font.PLAIN, size), new Vector(x, 1), ColorE.WHITE);
                QuickDraw.drawText(this,"Close:" + stock.getClose(candle.getDateTime()), new Font("Arial", Font.PLAIN, size), new Vector(x, 1 + size), ColorE.WHITE);
                QuickDraw.drawText(this,"Min:" + stock.getMin(candle.getDateTime()), new Font("Arial", Font.PLAIN, size), new Vector(x, 1 + size*2), ColorE.WHITE);
                QuickDraw.drawText(this,"Max:" + stock.getMax(candle.getDateTime()), new Font("Arial", Font.PLAIN, size), new Vector(x, 1 + size*3), ColorE.WHITE);
            }
        }

        //Draw Bottom Bar
        QuickDraw.drawRect(this,new Vector(0,getWindow().getSize().getY() - 60),new Vector(getWindow().getSize().getX(),60),new ColorE(25,25,25,255));
        super.draw();
    }

    private final StopWatch saveWatch = new StopWatch();

    @Override
    public void tick() {
        super.tick();

        //Update anchored elements
        candlePercent.setPos(new Vector(10,getWindow().getSize().getY() - candlePercent.getSize().getY() - 20));
        scaleSlider.setPos(new Vector(getWindow().getSize().getX() - scaleSlider.getSize().getX() - 10,getWindow().getSize().getY() - scaleSlider.getSize().getY() - 20));

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
