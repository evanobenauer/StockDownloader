package com.ejo.stockdownloader.render;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.time.DateTime;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ElementUI;
import com.ejo.glowui.util.QuickDraw;
import com.ejo.stockdownloader.data.Stock;

public class CandleUI extends ElementUI {

    private final Stock stock;
    private final DateTime dateTime;

    private final double x, focusY, focusPrice;

    private final double width;

    private final Vector scale;

    //Historical Candle
    public CandleUI(Stock stock, DateTime dateTime, double x, double focusY, double focusPrice, double width, Vector scale) {
        super(Vector.NULL,true,true);
        this.stock = stock;
        this.dateTime = dateTime;

        this.x = x;
        this.focusY = focusY; //The Y position on the screen of the focus price
        this.focusPrice = focusPrice; //The price at which the chart is centered at

        this.width = width;
        this.scale = scale;
    }

    //Current Candle. Current candles have a dateTime of NULL
    public CandleUI(Stock stock, double x, double focusY, double focusPrice, double width, Vector scale) {
        this(stock,null,x,focusY,focusPrice,width,scale);
    }

    @Override
    public void draw(Scene scene, Vector mousePos) {
        super.draw(scene, mousePos);
        double wickWidth = getBodySize().getX()/6 * getScale().getX();

        //Wicks
        int colorOffset = 100;
        ColorE wickColor = new ColorE(getColor().getRed() - colorOffset, getColor().getGreen() - colorOffset, getColor().getBlue() - colorOffset);
        QuickDraw.drawRect(getPos().getAdded((getBodySize().getX() / 2) - (wickWidth / 2),0),new Vector(wickWidth, -(getStock().getMax(getDateTime()) - getStock().getOpen(getDateTime()))*getScale().getY()), wickColor);
        QuickDraw.drawRect(getPos().getAdded((getBodySize().getX() / 2) - (wickWidth / 2),0),new Vector(wickWidth, (getStock().getOpen(getDateTime()) - getStock().getMin(getDateTime()))*getScale().getY()), wickColor);

        //Body
        QuickDraw.drawRect(getPos().getAdded(0,getBodySize().getY()/2),new Vector(getBodySize().getX(),1),getColor()); //Base Gray Candle
        QuickDraw.drawRect(getPos(),getBodySize(),getColor());

    }

    @Override
    public boolean updateMouseOver(Vector mousePos) {
        boolean mouseOverX = mousePos.getX() >= getPos().getX() && mousePos.getX() <= getPos().getX() + getBodySize().getX();
        boolean mouseOverYDown = mousePos.getY() >= getPos().getY() && mousePos.getY() <= getPos().getY() + getBodySize().getY();
        boolean mouseOverYUp = mousePos.getY() <= getPos().getY() && mousePos.getY() >= getPos().getY() + getBodySize().getY();
        return mouseOver = (mouseOverX && mouseOverYDown) || (mouseOverX && mouseOverYUp);
    }


    public boolean isGreen() {
        return getStock().getClose(getDateTime()) > getStock().getOpen(getDateTime());
    }

    public boolean isRed() {
        return getStock().getClose(getDateTime()) < getStock().getOpen(getDateTime());
    }

    public ColorE getColor() {
        if (isGreen()) return new ColorE(75, 200, 75);
        if (isRed()) return new ColorE(200, 50, 50);
        return ColorE.GRAY;
    }

    @Override
    public Vector getPos() {
        return new Vector(this.x, getFocusY() -(getStock().getOpen(getDateTime()) * getScale().getY()) + getFocusPrice()*getScale().getY());
    }

    public Vector getBodySize() {
        double candleHeight = -(getStock().getClose(getDateTime()) - getStock().getOpen(getDateTime()))*getScale().getY();
        return new Vector(getWidth()*getScale().getX(),candleHeight);
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


    public DateTime getDateTime() {
        return dateTime;
    }

    public Stock getStock() {
        return stock;
    }

}
