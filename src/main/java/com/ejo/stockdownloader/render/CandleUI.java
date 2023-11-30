package com.ejo.stockdownloader.render;


import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ElementUI;
import com.ejo.glowui.util.render.QuickDraw;
import com.ejo.stockdownloader.data.Stock;

public class CandleUI extends ElementUI {

    private float[] data;

    private final Stock stock;
    private final DateTime dateTime;

    private final double x, focusY, focusPrice;

    private final double width;

    private final Vector scale;

    private ColorE colorGreen;
    private ColorE colorRed;
    private ColorE colorNull;

    //Historical Candle
    public CandleUI(Stock stock, DateTime dateTime, double x, double focusY, double focusPrice, double width, Vector scale) {
        super(Vector.NULL, true, true);
        this.stock = stock;
        this.dateTime = dateTime;

        this.x = x;
        this.focusY = focusY; //The Y position on the screen of the focus price
        this.focusPrice = focusPrice; //The price at which the chart is centered at

        this.width = width;
        this.scale = scale;

        this.colorGreen = new ColorE(75, 200, 75);
        this.colorRed = new ColorE(200, 50, 50);
        this.colorNull = ColorE.GRAY;

        updateData();
    }

    //Current Candle. Current candles have a dateTime of NULL
    public CandleUI(Stock stock, double x, double focusY, double focusPrice, double width, Vector scale) {
        this(stock, stock.getOpenTime(), x, focusY, focusPrice, width, scale);
    }

    @Override
    public void drawElement(Scene scene, Vector mousePos) {
        updateData();
        float open = data[0];
        float min = data[2];
        float max = data[3];

        double wickWidth = getBodySize().getX() / 6;

        //Wicks
        int colorOffset = 100;
        Vector wickPos = getPos().getAdded((getBodySize().getX() / 2) - (wickWidth / 2), 0);
        ColorE wickColor = new ColorE(getColor().getRed() - colorOffset, getColor().getGreen() - colorOffset, getColor().getBlue() - colorOffset);
        QuickDraw.drawRect(wickPos, new Vector(wickWidth, -(max - open) * getScale().getY()), wickColor);
        QuickDraw.drawRect(wickPos, new Vector(wickWidth, (open - min) * getScale().getY()), wickColor);

        //Body
        QuickDraw.drawRect(getPos().getAdded(0, getBodySize().getY() / 2), new Vector(getBodySize().getX(), 1), getColor()); //Base Gray Candle
        QuickDraw.drawRect(getPos(), getBodySize(), getColor());
    }

    @Override
    protected void tickElement(Scene scene, Vector mousePos) {
    }

    @Override
    public boolean updateMouseOver(Vector mousePos) {
        boolean mouseOverX = mousePos.getX() >= getPos().getX() && mousePos.getX() <= getPos().getX() + getBodySize().getX();
        boolean mouseOverYDown = mousePos.getY() >= getPos().getY() && mousePos.getY() <= getPos().getY() + getBodySize().getY();
        boolean mouseOverYUp = mousePos.getY() <= getPos().getY() && mousePos.getY() >= getPos().getY() + getBodySize().getY();
        return mouseOver = (mouseOverX && mouseOverYDown) || (mouseOverX && mouseOverYUp);
    }

    //Data is updated like this so that there are not many calls to stock get methods as those are not as efficient
    private void updateData() {
        if (getOpenTime().equals(getStock().getOpenTime())) {
            data = new float[]{getStock().getOpen(), getStock().getPrice(), getStock().getMin(), getStock().getMax()};
        } else {
            data = getStock().getData(getOpenTime());
        }
    }

    public CandleUI setGreen(ColorE color) {
        this.colorGreen = color;
        return this;
    }

    public CandleUI setRed(ColorE color) {
        this.colorRed = color;
        return this;
    }

    public CandleUI setColorNull(ColorE color) {
        this.colorNull = color;
        return this;
    }

    public boolean isGreen() {
        float open = data[0];
        float close = data[1];
        return close > open;
    }

    public boolean isRed() {
        float open = data[0];
        float close = data[1];
        return close < open;
    }

    public ColorE getColor() {
        if (isGreen()) return colorGreen;
        if (isRed()) return colorRed;
        return colorNull;
    }

    @Override
    public Vector getPos() {
        float open = data[0];
        return new Vector(this.x, getFocusY() - (open * getScale().getY()) + getFocusPrice() * getScale().getY());
    }

    public Vector getBodySize() {
        float open = data[0];
        float close = data[1];
        double candleHeight = -(close - open) * getScale().getY();
        return new Vector(getWidth() * getScale().getX(), candleHeight);
    }

    public double getFocusY() {
        return focusY;
    }

    public double getFocusPrice() {
        return focusPrice;
    }

    public double getWidth() {
        return width;
    }

    public Vector getScale() {
        return scale;
    }


    public DateTime getOpenTime() {
        return dateTime;
    }

    public Stock getStock() {
        return stock;
    }

}