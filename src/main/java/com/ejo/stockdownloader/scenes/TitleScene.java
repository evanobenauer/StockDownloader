package com.ejo.stockdownloader.scenes;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.misc.Container;
import com.ejo.glowlib.misc.DoOnce;
import com.ejo.glowlib.setting.Setting;
import com.ejo.glowlib.setting.SettingManager;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ElementUI;
import com.ejo.glowui.scene.elements.TextUI;
import com.ejo.glowui.scene.elements.shape.RectangleUI;
import com.ejo.glowui.scene.elements.shape.physics.PhysicsDraggableUI;
import com.ejo.glowui.scene.elements.shape.physics.PhysicsObjectUI;
import com.ejo.glowui.scene.elements.widget.ButtonUI;
import com.ejo.glowui.scene.elements.widget.ModeCycleUI;
import com.ejo.glowui.scene.elements.widget.TextFieldUI;
import com.ejo.glowui.util.QuickDraw;
import com.ejo.stockdownloader.data.Stock;

import java.awt.*;
import java.util.Random;

public class TitleScene extends Scene {

    private final Setting<String> stockTickerContainer = new Setting<>("stockTicker","");

    private final Setting<Stock.TimeFrame> timeFrameContainer = new Setting<>("timeFrame",Stock.TimeFrame.ONE_MINUTE);


    private final ModeCycleUI<Stock.TimeFrame> mode = new ModeCycleUI<>(this,Vector.NULL,new Vector(100,20),ColorE.BLUE,timeFrameContainer,
            Stock.TimeFrame.ONE_SECOND, Stock.TimeFrame.FIVE_SECONDS, Stock.TimeFrame.THIRTY_SECONDS,
            Stock.TimeFrame.ONE_MINUTE, Stock.TimeFrame.FIVE_MINUTES, Stock.TimeFrame.THIRTY_MINUTES,
            Stock.TimeFrame.ONE_HOUR, Stock.TimeFrame.TWO_HOUR, Stock.TimeFrame.FOUR_HOUR, Stock.TimeFrame.ONE_DAY);

    private final TextFieldUI textFieldUI = new TextFieldUI(this,Vector.NULL,new Vector(100,20),ColorE.WHITE,stockTickerContainer,"Stock");

    private final ButtonUI button = new ButtonUI(this,"Download!",Vector.NULL,new Vector(200,60),new ColorE(0,125,200,200),() -> {
        if (!textFieldUI.getContainer().get().equals("")) {
            getWindow().setScene(new DownloadScene(new Stock(stockTickerContainer.get().replace(" ", ""), timeFrameContainer.get())));
            SettingManager.getDefaultManager().saveAll();
        }
    });

    private final TextUI title = new TextUI(this,"Stock Downloader",new Font("Arial Black",Font.BOLD,50),Vector.NULL,ColorE.WHITE);

    public TitleScene() {
        super("Title");
        SettingManager.getDefaultManager().loadAll();
    }

    @Override
    public void draw() {
        DoOnce.default6.run(() -> {
            Random random = new Random();
            for (int i = 0; i < 20; i++) {
                addElements(new PhysicsDraggableUI(new RectangleUI(this,getWindow().getSize().getMultiplied(.5),new Vector(10,10),new ColorE(random.nextInt(),random.nextInt(),random.nextInt(),255)),1,new Vector(random.nextDouble(-5,5),random.nextDouble(-5,5)),Vector.NULL));
            }
            addElements(button,textFieldUI,mode,title);
        });
        QuickDraw.drawRect(this,Vector.NULL,getWindow().getSize(),new ColorE(50,50,50,255));
        QuickDraw.drawRect(this,textFieldUI.getPos(),textFieldUI.getSize(),new ColorE(100,100,100,255));
        QuickDraw.drawRect(this,mode.getPos(),mode.getSize(),new ColorE(100,100,100,255));
        super.draw();
    }

    double step = 0;

    @Override
    public void tick() {
        super.tick();
        for (ElementUI element : getElements()) {
            if (element instanceof PhysicsObjectUI phys) {
                swapVelocity(phys);
            }
        }
        double yOffset = -40;
        title.setPos(getWindow().getSize().getMultiplied(.5d).getAdded(title.getSize().getMultiplied(-.5)).getAdded(0,yOffset));
        title.setPos(title.getPos().getAdded(new Vector(0,Math.sin(step) * 8)));
        step += 0.05;
        if (step >= Math.PI*2) step = 0;

        textFieldUI.setPos(getWindow().getSize().getMultiplied(.5d).getAdded(textFieldUI.getSize().getMultiplied(-.5)).getAdded(-textFieldUI.getSize().getX(),140).getAdded(0,yOffset));
        mode.setPos(getWindow().getSize().getMultiplied(.5d).getAdded(mode.getSize().getMultiplied(-.5)).getAdded(+mode.getSize().getX(),140).getAdded(0,yOffset));

        button.setPos(getWindow().getSize().getMultiplied(.5d).getAdded(button.getSize().getMultiplied(-.5)).getAdded(0,title.getFont().getSize() + 30).getAdded(0,yOffset));
    }

    private void swapVelocity(PhysicsObjectUI phys) {
        int size = 10;
        if (phys.getPos().getX() < 0) {
            phys.setPos(new Vector(0,phys.getPos().getY()));
            phys.setVelocity(new Vector(-phys.getVelocity().getX(),phys.getVelocity().getY()));
        }
        if (phys.getPos().getX() + size > getWindow().getSize().getX()) {
            phys.setPos(new Vector(getWindow().getSize().getX() - size,phys.getPos().getY()));
            phys.setVelocity(new Vector(-phys.getVelocity().getX(),phys.getVelocity().getY()));
        }
        if (phys.getPos().getY() < 0) {
            phys.setPos(new Vector(phys.getPos().getX(),0));
            phys.setVelocity(new Vector(phys.getVelocity().getX(),-phys.getVelocity().getY()));
        }
        if (phys.getPos().getY() + size > getWindow().getSize().getY()) {
            phys.setPos(new Vector(phys.getPos().getX(),getWindow().getSize().getY() - size));
            phys.setVelocity(new Vector(phys.getVelocity().getX(),-phys.getVelocity().getY()));
        }
    }

}
