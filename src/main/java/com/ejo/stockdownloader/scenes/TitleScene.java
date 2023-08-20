package com.ejo.stockdownloader.scenes;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.misc.DoOnce;
import com.ejo.glowlib.setting.Container;
import com.ejo.glowlib.setting.Setting;
import com.ejo.glowlib.setting.SettingManager;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.ElementUI;
import com.ejo.glowui.scene.elements.ProgressBarUI;
import com.ejo.glowui.scene.elements.SideBarUI;
import com.ejo.glowui.scene.elements.TextUI;
import com.ejo.glowui.scene.elements.shape.RectangleUI;
import com.ejo.glowui.scene.elements.shape.physics.PhysicsDraggableUI;
import com.ejo.glowui.scene.elements.shape.physics.PhysicsObjectUI;
import com.ejo.glowui.scene.elements.widget.ButtonUI;
import com.ejo.glowui.scene.elements.widget.ModeCycleUI;
import com.ejo.glowui.scene.elements.widget.TextFieldUI;
import com.ejo.glowui.scene.elements.widget.ToggleUI;
import com.ejo.glowui.util.Fonts;
import com.ejo.glowui.util.QuickDraw;
import com.ejo.stockdownloader.App;
import com.ejo.stockdownloader.data.Stock;
import com.ejo.stockdownloader.data.api.APIDownloader;
import com.ejo.stockdownloader.data.api.AlphaVantageDownloader;
import com.ejo.stockdownloader.util.StockUtil;
import com.ejo.stockdownloader.util.TimeFrame;

import java.awt.*;
import java.util.Random;

public class TitleScene extends Scene {

    private APIDownloader apiDownloader;

    //Main Data Settings
    private final Setting<String> stockTicker = new Setting<>("stockTicker","");
    private final Setting<TimeFrame> timeFrame = new Setting<>("timeFrame",TimeFrame.ONE_MINUTE);
    private final Setting<String> downloadMode = new Setting<>("downloadMode","Live Data");

    //Api Settings
    private final Setting<String> api = new Setting<>("api","AlphaVantage");

    //Live Data Settings
    private final Setting<Boolean> liveExtendedHours = new Setting<>("extendedHoursLive",false);

    //Alpha Vantage Settings
    private final Setting<String> apiKey = new Setting<>("apiKey","H0JHAOU61I4MESDZ");
    private final Setting<Boolean> apiExtendedHours = new Setting<>("extendedHoursAPI",false);
    private final Setting<String> apiTime = new Setting<>("monthMode","All");
    private final Setting<String> apiYear = new Setting<>("year","2000");
    private final Setting<String> apiMonth = new Setting<>("month","01");


    //Sidebar Elements
    private final ModeCycleUI<String> modeDownloadMode = new ModeCycleUI<>(new Vector(15,25),new Vector(110,20),ColorE.BLUE, downloadMode,"Live Data", "API");

    private final ToggleUI toggleLiveExtendedHours = new ToggleUI("Extended Hours",new Vector(15,100),new Vector(110,20),new ColorE(0,200,255,255), liveExtendedHours);

    private final ModeCycleUI<String> modeApi = new ModeCycleUI<>(new Vector(15,105),new Vector(110,20),ColorE.BLUE, api,"AlphaVantage");
    private final TextFieldUI fieldApiKey = new TextFieldUI(new Vector(15,165),new Vector(110,20),ColorE.WHITE, apiKey,"API Key",false);
    private final ToggleUI toggleApiExtendedHours = new ToggleUI("Extended Hours",new Vector(15,190),new Vector(110,20),new ColorE(0,200,255,255), apiExtendedHours);
    private final ModeCycleUI<String> modeApiTime = new ModeCycleUI<>("Time",new Vector(15,215),new Vector(110,20),ColorE.BLUE, apiTime,"Month","All");
    private final TextFieldUI fieldApiYear = new TextFieldUI(new Vector(88,240),new Vector(37,20),ColorE.WHITE, apiYear,"",true,4);
    private final TextFieldUI fieldApiMonth = new TextFieldUI(new Vector(56,240),new Vector(22,20),ColorE.WHITE, apiMonth,"",true,2);

    private final SideBarUI sideBarSettings = new SideBarUI("Settings",SideBarUI.Type.RIGHT,140,false,new ColorE(0,125,200,200), modeDownloadMode, toggleLiveExtendedHours, modeApi, fieldApiKey, toggleApiExtendedHours, modeApiTime, fieldApiYear, fieldApiMonth);


    //Center Elements
    private final TextUI title = new TextUI("Stock Downloader",new Font("Arial Black",Font.BOLD,50),Vector.NULL,ColorE.WHITE);

    private final TextFieldUI stockTickerField = new TextFieldUI(Vector.NULL,new Vector(100,20),ColorE.WHITE, stockTicker,"Stock",false);

    private final ModeCycleUI<TimeFrame> timeFrameMode = new ModeCycleUI<>(Vector.NULL,new Vector(100,20),ColorE.BLUE, timeFrame,
            TimeFrame.ONE_SECOND, TimeFrame.FIVE_SECONDS, TimeFrame.THIRTY_SECONDS,
            TimeFrame.ONE_MINUTE, TimeFrame.FIVE_MINUTES, TimeFrame.THIRTY_MINUTES,
            TimeFrame.ONE_HOUR, TimeFrame.TWO_HOUR, TimeFrame.FOUR_HOUR, TimeFrame.ONE_DAY
    );
    private final ProgressBarUI<Double> progressBarApiDownload = new ProgressBarUI<>(Vector.NULL,new Vector(200,20),ColorE.BLUE,new Container<>(0d),0,1);

    private final TextUI warningText = new TextUI("", Fonts.getDefaultFont(20),Vector.NULL,ColorE.WHITE);


    private final ButtonUI downloadButton = new ButtonUI("Download!",Vector.NULL,new Vector(200,60),new ColorE(0,125,200,200), ButtonUI.MouseButton.LEFT,() -> {
        System.out.println(SettingManager.getDefaultManager().saveAll() ? "Saved" : "Could Not Save");
        if (stockTickerField.getContainer().get().equals("")) return;

        if (downloadMode.get().equals("Live Data")) {
            getWindow().setScene(new LiveDownloadScene(new Stock(stockTicker.get().replace(" ", ""), timeFrame.get(),liveExtendedHours.get())));

        } else if (downloadMode.get().equals("API")) {
            warningText.setText("");
            progressBarApiDownload.setRendered(true);

            //API SPECIFIC CODE
            if (api.get().equals("AlphaVantage")) runAlphaVantageDownload();

            //SET WARNING TEXT IF A DOWNLOAD IS ACTIVE, BUT BUTTON IS CLICKED AGAIN
            if (apiDownloader.isDownloadActive().get())
                warningText.setText("Download Already In Progress!").setColor(ColorE.RED);

        }
    });

    //DoOnce Instantiation
    private final DoOnce doInit = new DoOnce();

    
    public TitleScene() {
        super("Title");
        doInit.reset();
        SettingManager.getDefaultManager().loadAll();
    }

    private void initScene() {
        doInit.run(() -> {
            //Set Window Not-Economic
            App.getWindow().setEconomic(false);

            Random random = new Random();
            //Add Bouncing Squares
            for (int i = 0; i < 20; i++) {
                addElements(new PhysicsDraggableUI(new RectangleUI(getSize().getMultiplied(.5),new Vector(10,10),new ColorE(random.nextInt(),random.nextInt(),random.nextInt(),255)),1,new Vector(random.nextDouble(-5,5),random.nextDouble(-5,5)),Vector.NULL));
            }

            //Set Progress Bar Off By Default
            progressBarApiDownload.setRendered(false);

            //Add Widgets
            addElements(downloadButton, stockTickerField, timeFrameMode,title, sideBarSettings, progressBarApiDownload, warningText);
        });
    }

    @Override
    public void draw() {
        initScene();

        //Draw Background
        drawBackground(new ColorE(50,50,50,255));

        //Draw Widget Backgrounds
        QuickDraw.drawRect(stockTickerField.getPos(), stockTickerField.getSize(),new ColorE(100,100,100,255));
        QuickDraw.drawRect(timeFrameMode.getPos(), timeFrameMode.getSize(),new ColorE(100,100,100,255));
        if (progressBarApiDownload.shouldRender()) QuickDraw.drawRect(progressBarApiDownload.getPos(), progressBarApiDownload.getSize(),new ColorE(100,100,100,255));

        //Set Warning Text
        if (apiDownloader != null && !apiDownloader.isDownloadActive().get() && apiDownloader.isDownloadFinished().get()) {
            if (apiDownloader.isDownloadSuccessful().get())
                warningText.setText("Download Finished Successfully!").setColor(ColorE.GREEN);
            else
                warningText.setText("Download Failed!").setColor(ColorE.RED);
        }

        updateWidgetPositions();
        super.draw();
        drawSidebarData();
    }

    @Override
    public void tick() {
        super.tick();

        //Bounce Physics Squares
        for (ElementUI element : getElements()) {
            if (element instanceof PhysicsObjectUI phys) doPhysicsBounce(phys);
        }
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
        progressBarApiDownload.setPos(downloadButton.getPos().getAdded(0,120));
        warningText.setPos(getSize().getMultiplied(.5).getAdded(warningText.getSize().getMultiplied(-.5)).getAdded(0,170));
    }

    private void drawSidebarData() {
        QuickDraw.drawTextCentered("Download Mode:",new Font("Arial",Font.PLAIN,16), sideBarSettings.getBarPos().getAdded(0,45),new Vector(sideBarSettings.getWidth(),0),ColorE.WHITE);
        modeDownloadMode.setPos(new Vector(modeDownloadMode.getPos().getX(),55));

        if (downloadMode.get().equals("Live Data")) {
            toggleLiveExtendedHours.setEnabled(true);

            modeApi.setEnabled(false);
            fieldApiKey.setEnabled(false);
            toggleApiExtendedHours.setEnabled(false);
            modeApiTime.setEnabled(false);
            fieldApiMonth.setEnabled(false);
            fieldApiYear.setEnabled(false);
        } else if (downloadMode.get().equals("API")) {
            toggleLiveExtendedHours.setEnabled(false);

            modeApi.setEnabled(true);

            fieldApiKey.setEnabled(api.get().equals("AlphaVantage"));
            toggleApiExtendedHours.setEnabled(api.get().equals("AlphaVantage"));
            modeApiTime.setEnabled(api.get().equals("AlphaVantage"));

            fieldApiMonth.setEnabled(apiTime.get().equals("Month") && api.get().equals("AlphaVantage"));
            fieldApiYear.setEnabled(apiTime.get().equals("Month") && api.get().equals("AlphaVantage"));

            if (api.get().equals("AlphaVantage")) {
                QuickDraw.drawTextCentered("Alpha Vantage Settings:", new Font("Arial", Font.PLAIN, 12), sideBarSettings.getBarPos().getAdded(0, 150), new Vector(sideBarSettings.getWidth(), 0), ColorE.WHITE);

                if (apiTime.get().equals("Month")) {
                    QuickDraw.drawText("Month:", new Font("Arial", Font.PLAIN, 13), sideBarSettings.getBarPos().getAdded(15, fieldApiMonth.getPos().getY() + 2), ColorE.WHITE);
                    QuickDraw.drawText("/", new Font("Arial", Font.PLAIN, 16), sideBarSettings.getBarPos().getAdded(fieldApiMonth.getPos()).getAdded(new Vector(fieldApiMonth.getSize().getX() + 3, 0)), ColorE.WHITE);
                }
            }

            QuickDraw.drawTextCentered("API:",new Font("Arial",Font.PLAIN,16), sideBarSettings.getBarPos().getAdded(0,95),new Vector(sideBarSettings.getWidth(),0),ColorE.WHITE);
        } else {
            for (ElementUI element : sideBarSettings.getElementList()) {
                element.setEnabled(false);
            }
        }
    }

    private void runAlphaVantageDownload() {
        if (apiDownloader == null || !apiDownloader.isDownloadActive().get()) { //Run only if the api downloader has NOT been set OR if the downloader is NOT active

            //Set apiDownloader as the AlphaVantage downloader
            apiDownloader = new AlphaVantageDownloader(apiKey.getKey(), stockTicker.get(), timeFrame.get(), apiExtendedHours.get());
            AlphaVantageDownloader downloader = (AlphaVantageDownloader) apiDownloader;

            //Set Progress Bar Container
            progressBarApiDownload.setContainer(apiDownloader.getDownloadProgress());

            //Download data based on month mode
            if (apiTime.get().equals("Month")) {
                if (apiYear.get().length() == 4 && apiMonth.get().length() == 2) {
                    downloader.download(apiYear.get(), apiMonth.get());
                } else {
                    warningText.setText("Invalid Time! - Make sure time is in the form: MM / YYYY").setColor(ColorE.RED);
                }
            } else if (apiTime.get().equals("All")) {
                downloader.downloadAll();
            } else {
                System.out.println("Lol, Maybe ill add an update mode to only download the last month?");
            }

            //TODO: If too many requests, add an error here

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
