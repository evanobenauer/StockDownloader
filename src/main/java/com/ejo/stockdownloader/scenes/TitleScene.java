package com.ejo.stockdownloader.scenes;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.misc.DoOnce;
import com.ejo.glowlib.setting.Setting;
import com.ejo.glowlib.setting.SettingManager;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ElementUI;
import com.ejo.glowui.scene.elements.SideBarUI;
import com.ejo.glowui.scene.elements.TextUI;
import com.ejo.glowui.scene.elements.shape.RectangleUI;
import com.ejo.glowui.scene.elements.shape.physics.PhysicsDraggableUI;
import com.ejo.glowui.scene.elements.shape.physics.PhysicsObjectUI;
import com.ejo.glowui.scene.elements.widget.ButtonUI;
import com.ejo.glowui.scene.elements.widget.ModeCycleUI;
import com.ejo.glowui.scene.elements.widget.TextFieldUI;
import com.ejo.glowui.scene.elements.widget.ToggleUI;
import com.ejo.glowui.util.QuickDraw;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.data.api.AlphaVantageDownloader;
import com.ejo.stockdownloader.util.TimeFrame;

import java.awt.*;
import java.util.Random;

public class TitleScene extends Scene {

    //Main Data Settings
    private final Setting<String> stockTickerContainer = new Setting<>("stockTicker","");
    private final Setting<TimeFrame> timeFrameContainer = new Setting<>("timeFrame",TimeFrame.ONE_MINUTE);
    private final Setting<String> downloadModeContainer = new Setting<>("downloadMode","Live Data");

    //Api Settings
    private final Setting<String> apiContainer = new Setting<>("api","AlphaVantage");

    //Live Data Settings
    private final Setting<Boolean> extendedHoursContainerLive = new Setting<>("extendedHoursLive",false);

    //Alpha Vantage Settings
    private final Setting<String> apiKeyContainer = new Setting<>("apiKey","H0JHAOU61I4MESDZ");
    private final Setting<Boolean> extendedHoursContainerAPI = new Setting<>("extendedHoursAPI",false);
    private final Setting<String> timeContainer = new Setting<>("monthMode","All");
    private final Setting<String> yearContainer = new Setting<>("year","2000");
    private final Setting<String> monthContainer = new Setting<>("month","01");


    //Sidebar Elements
    private final ModeCycleUI<String> downloadMode = new ModeCycleUI<>(new Vector(15,25),new Vector(110,20),ColorE.BLUE, downloadModeContainer,"Live Data", "API");

    private final ToggleUI extendedHoursToggleLive = new ToggleUI("Extended Hours",new Vector(15,100),new Vector(110,20),new ColorE(0,200,255,255), extendedHoursContainerLive);

    private final ModeCycleUI<String> apiSelectorMode = new ModeCycleUI<>(new Vector(15,105),new Vector(110,20),ColorE.BLUE, apiContainer,"AlphaVantage");
    private final TextFieldUI apiKeyField = new TextFieldUI(new Vector(15,165),new Vector(110,20),ColorE.WHITE,apiKeyContainer,"API Key",false);
    private final ToggleUI extendedHoursToggleAPI = new ToggleUI("Extended Hours",new Vector(15,190),new Vector(110,20),new ColorE(0,200,255,255), extendedHoursContainerAPI);
    private final ModeCycleUI<String> dateMode = new ModeCycleUI<>("Time",new Vector(15,215),new Vector(110,20),ColorE.BLUE, timeContainer,"Month","All");
    private final TextFieldUI yearField = new TextFieldUI(new Vector(88,240),new Vector(37,20),ColorE.WHITE,yearContainer,"",true,4);
    private final TextFieldUI monthField = new TextFieldUI(new Vector(56,240),new Vector(22,20),ColorE.WHITE,monthContainer,"",true,2);

    private final SideBarUI settingsSideBar = new SideBarUI("Settings",SideBarUI.Type.RIGHT,140,false,new ColorE(0,125,200,200), downloadMode, extendedHoursToggleLive,apiSelectorMode, apiKeyField, extendedHoursToggleAPI, dateMode,yearField,monthField);


    //Center Elements
    private final TextUI title = new TextUI("Stock Downloader",new Font("Arial Black",Font.BOLD,50),Vector.NULL,ColorE.WHITE);

    private final TextFieldUI stockTickerField = new TextFieldUI(Vector.NULL,new Vector(100,20),ColorE.WHITE,stockTickerContainer,"Stock",false);
    private final ModeCycleUI<TimeFrame> timeFrameMode = new ModeCycleUI<>(Vector.NULL,new Vector(100,20),ColorE.BLUE,timeFrameContainer,
            TimeFrame.ONE_SECOND, TimeFrame.FIVE_SECONDS, TimeFrame.THIRTY_SECONDS,
            TimeFrame.ONE_MINUTE, TimeFrame.FIVE_MINUTES, TimeFrame.THIRTY_MINUTES,
            TimeFrame.ONE_HOUR, TimeFrame.TWO_HOUR, TimeFrame.FOUR_HOUR, TimeFrame.ONE_DAY
    );

    private final ButtonUI downloadButton = new ButtonUI("Download!",Vector.NULL,new Vector(200,60),new ColorE(0,125,200,200), ButtonUI.MouseButton.LEFT,() -> {
        if (SettingManager.getDefaultManager().saveAll()) {
            System.out.println("Saved");
        } else {
            System.out.println("Could Not Save");
        }

        if (stockTickerField.getContainer().get().equals("")) return;

        if (downloadModeContainer.get().equals("Live Data")) {
            getWindow().setScene(new LiveDownloadScene(new Stock(stockTickerContainer.get().replace(" ", ""), timeFrameContainer.get(),true)));
        } else if (downloadModeContainer.get().equals("API")) {
            if (apiContainer.get().equals("AlphaVantage")) {
                AlphaVantageDownloader downloader = new AlphaVantageDownloader(apiKeyContainer.getKey(), stockTickerContainer.get(), timeFrameContainer.get(), extendedHoursContainerAPI.get());
                if (timeContainer.get().equals("Month")) {
                    downloader.download(yearContainer.get(), monthContainer.get());
                } else {
                    downloader.downloadAll();
                }
            }

        }
    });


    //DoOnce Instantiation
    private final DoOnce doInit = new DoOnce();

    
    public TitleScene() {
        super("Title");
        doInit.reset();
        SettingManager.getDefaultManager().loadAll();
    }


    @Override
    public void draw() {
        initScene();
        updateWidgetPositions();

        //Draw Background
        drawBackground(new ColorE(50,50,50,255));

        //Draw Widget Backgrounds
        QuickDraw.drawRect(stockTickerField.getPos(), stockTickerField.getSize(),new ColorE(100,100,100,255));
        QuickDraw.drawRect(timeFrameMode.getPos(), timeFrameMode.getSize(),new ColorE(100,100,100,255));

        super.draw();

        drawSidebarData();
    }


    @Override
    public void tick() {
        getWindow().setEconomic(false);
        super.tick();

        //Bounce Physics Squares
        for (ElementUI element : getElements()) {
            if (element instanceof PhysicsObjectUI phys) doPhysicsBounce(phys);
        }
    }

    private void initScene() {
        doInit.run(() -> {
            Random random = new Random();
            //Add Bouncing Squares
            for (int i = 0; i < 20; i++) {
                addElements(new PhysicsDraggableUI(new RectangleUI(getSize().getMultiplied(.5),new Vector(10,10),new ColorE(random.nextInt(),random.nextInt(),random.nextInt(),255)),1,new Vector(random.nextDouble(-5,5),random.nextDouble(-5,5)),Vector.NULL));
            }

            //Add Widgets
            addElements(downloadButton, stockTickerField, timeFrameMode,title, settingsSideBar);
        });
    }

    private double step = 0;
    private void updateWidgetPositions() {
        double yOffset = -40;
        //Set Floating Title
        title.setPos(getSize().getMultiplied(.5d).getAdded(title.getSize().getMultiplied(-.5)).getAdded(0,yOffset));
        title.setPos(title.getPos().getAdded(new Vector(0,Math.sin(step) * 8)));
        step += 0.05;
        if (step >= Math.PI*2) step = 0;

        //Set Widget Positions
        stockTickerField.setPos(getSize().getMultiplied(.5d).getAdded(stockTickerField.getSize().getMultiplied(-.5)).getAdded(-stockTickerField.getSize().getX(),140).getAdded(0,yOffset));
        timeFrameMode.setPos(getSize().getMultiplied(.5d).getAdded(timeFrameMode.getSize().getMultiplied(-.5)).getAdded(+timeFrameMode.getSize().getX(),140).getAdded(0,yOffset));
        downloadButton.setPos(getSize().getMultiplied(.5d).getAdded(downloadButton.getSize().getMultiplied(-.5)).getAdded(0,title.getFont().getSize() + 30).getAdded(0,yOffset));
    }

    private void drawSidebarData() {
        QuickDraw.drawTextCentered("Download Mode:",new Font("Arial",Font.PLAIN,16), settingsSideBar.getBarPos().getAdded(0,45),new Vector(settingsSideBar.getWidth(),0),ColorE.WHITE);
        downloadMode.setPos(new Vector(downloadMode.getPos().getX(),55));

        if (downloadModeContainer.get().equals("Live Data")) {
            extendedHoursToggleLive.disable(false);
            apiSelectorMode.disable(true);
            apiKeyField.disable(true);
            extendedHoursToggleAPI.disable(true);
            dateMode.disable(true);
            monthField.disable(true);
            yearField.disable(true);
        } else if (downloadModeContainer.get().equals("API")) {

            extendedHoursToggleLive.disable(true);
            apiSelectorMode.disable(false);
            apiKeyField.disable(!apiContainer.get().equals("AlphaVantage"));
            extendedHoursToggleAPI.disable(!apiContainer.get().equals("AlphaVantage"));
            dateMode.disable(!apiContainer.get().equals("AlphaVantage"));

            monthField.disable(!timeContainer.get().equals("Month"));
            yearField.disable(!timeContainer.get().equals("Month"));

            if (apiContainer.get().equals("AlphaVantage")) QuickDraw.drawTextCentered("Alpha Vantage Settings:",new Font("Arial",Font.PLAIN,12),settingsSideBar.getBarPos().getAdded(0,150), new Vector(settingsSideBar.getWidth(),0),ColorE.WHITE);

            if (timeContainer.get().equals("Month")) {
                QuickDraw.drawText("Month:",new Font("Arial",Font.PLAIN,13), settingsSideBar.getBarPos().getAdded(15,monthField.getPos().getY() + 2),ColorE.WHITE);
                QuickDraw.drawText("/",new Font("Arial",Font.PLAIN,16), settingsSideBar.getBarPos().getAdded(monthField.getPos()).getAdded(new Vector(monthField.getSize().getX() + 3,0)),ColorE.WHITE);
            }

            QuickDraw.drawTextCentered("API:",new Font("Arial",Font.PLAIN,16), settingsSideBar.getBarPos().getAdded(0,95),new Vector(settingsSideBar.getWidth(),0),ColorE.WHITE);
        } else {
            for (ElementUI element : settingsSideBar.getElementList()) {
                element.disable(true);
            }
        }
    }

    private void doPhysicsBounce(PhysicsObjectUI phys) {
        int size = 10;
        if (phys.getPos().getX() < 0) {
            phys.setPos(new Vector(0,phys.getPos().getY()));
            phys.setVelocity(new Vector(-phys.getVelocity().getX(),phys.getVelocity().getY()));
        }
        if (phys.getPos().getX() + size > getSize().getX()) {
            phys.setPos(new Vector(getSize().getX() - size,phys.getPos().getY()));
            phys.setVelocity(new Vector(-phys.getVelocity().getX(),phys.getVelocity().getY()));
        }
        if (phys.getPos().getY() < 0) {
            phys.setPos(new Vector(phys.getPos().getX(),0));
            phys.setVelocity(new Vector(phys.getVelocity().getX(),-phys.getVelocity().getY()));
        }
        if (phys.getPos().getY() + size > getSize().getY()) {
            phys.setPos(new Vector(phys.getPos().getX(),getSize().getY() - size));
            phys.setVelocity(new Vector(phys.getVelocity().getX(),-phys.getVelocity().getY()));
        }
    }
}
