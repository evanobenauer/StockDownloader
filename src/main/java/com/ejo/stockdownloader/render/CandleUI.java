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
    public CandleUI(Scene scene, Stock stock, DateTime dateTime, double x, double focusY, double focusPrice, double width, Vector scale) {
        super(scene,Vector.NULL,true,true);
        this.stock = stock;
        this.dateTime = dateTime;

        this.x = x;
        this.focusY = focusY; //The Y position on the screen of the focus price
        this.focusPrice = focusPrice; //The price at which the chart is centered at

        this.width = width;
        this.scale = scale;
    }

    //Current Candle
    public CandleUI(Scene scene, Stock stock, double x, double focusY, double focusPrice, double width, Vector scale) {
        this(scene,stock,null,x,focusY,focusPrice,width,scale);
    }

    @Override
    public void draw() {
        super.draw();
        double wickWidth = getWidth()*getScale().getX()/6;
        double candleHeight = -(getStock().getClose(getDateTime()) - getStock().getOpen(getDateTime()))*getScale().getY();

        //Wicks
        int colorOffset = 100;
        ColorE wickColor = new ColorE(getColor().getRed() - colorOffset, getColor().getGreen() - colorOffset, getColor().getBlue() - colorOffset);
        QuickDraw.drawRect(null,getPos().getAdded((getWidth()*getScale().getX() / 2) - (wickWidth*getScale().getX() / 2),0),new Vector(wickWidth * getScale().getX(), -(getStock().getMax(getDateTime()) - getStock().getOpen(getDateTime()))*getScale().getY()), wickColor);
        QuickDraw.drawRect(null,getPos().getAdded((getWidth()*getScale().getX() / 2) - (wickWidth*getScale().getX() / 2),0),new Vector(wickWidth * getScale().getX(), (getStock().getOpen(getDateTime()) - getStock().getMin(getDateTime()))*getScale().getY()), wickColor);

        //Body
        QuickDraw.drawRect(null, getPos().getAdded(0,candleHeight/2),new Vector(getWidth()*getScale().getX(),1),getColor()); //Base Gray Candle
        QuickDraw.drawRect(null,getPos(),new Vector(getWidth()*getScale().getX(),candleHeight),getColor());

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
        if (isGreen()) return new ColorE(0, 200, 0);
        if (isRed()) return new ColorE(200, 0, 0);
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
