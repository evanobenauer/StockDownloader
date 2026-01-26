package com.ejo.stockdownloader.scenes;

import com.ejo.glowlib.math.Vector;
import com.ejo.glowlib.misc.ColorE;
import com.ejo.glowlib.misc.DoOnce;
import com.ejo.glowlib.setting.Container;
import com.ejo.glowlib.setting.Setting;
import com.ejo.glowlib.setting.SettingManager;
import com.ejo.glowui.scene.Scene;
import com.ejo.glowui.scene.elements.*;
import com.ejo.glowui.scene.elements.shape.RectangleUI;
import com.ejo.glowui.scene.elements.widget.ButtonUI;
import com.ejo.glowui.scene.elements.widget.ModeCycleUI;
import com.ejo.glowui.scene.elements.widget.TextFieldUI;
import com.ejo.glowui.scene.elements.widget.ToggleUI;
import com.ejo.glowui.util.render.Fonts;
import com.ejo.glowui.util.render.QuickDraw;
import com.ejo.stockdownloader.App;
import com.ejo.stockdownloader.data.LiveDownloadStock;
import com.ejo.stockdownloader.data.api.APIDownloader;
import com.ejo.stockdownloader.data.api.AlphaVantageDownloader;
import com.ejo.stockdownloader.util.DownloadTimeFrame;
import com.ejo.uiphysics.elements.PhysicsDraggableUI;
import com.ejo.uiphysics.elements.PhysicsObjectUI;
//import com.ejo.uiphysics.elements.PhysicsDraggableUI;
//import com.ejo.uiphysics.elements.PhysicsObjectUI;

import java.awt.*;
import java.util.Random;

public class TitleScene extends Scene {

    private APIDownloader apiDownloader;

    //Main Data Settings
    private final Setting<String> stockTicker = new Setting<>("stockTicker", "");
    private final Setting<DownloadTimeFrame> timeFrame = new Setting<>("timeFrame", DownloadTimeFrame.ONE_MINUTE);
    private final Setting<String> downloadMode = new Setting<>("downloadMode", "Live Data");

    //Live Data Settings
    private final Setting<Boolean> liveExtendedHours = new Setting<>("extendedHoursLive", false);
    private final Setting<LiveDownloadStock.PriceSource> livePriceSource = new Setting<>("priceSourceLive", LiveDownloadStock.PriceSource.YAHOOFINANCE);

    //Api Settings
    private final Setting<String> api = new Setting<>("api", "AlphaVantage");

    //Alpha Vantage Settings
    private final Setting<String> alphaVantageKey = new Setting<>("apiKey", "H0JHAOU61I4MESDZ");
    private final Setting<Boolean> alphaVantagePremium = new Setting<>("premium", false);
    private final Setting<Boolean> alphaVantageExtendedHours = new Setting<>("extendedHoursAPI", false);
    private final Setting<String> alphaVantageTime = new Setting<>("monthMode", "All");
    private final Setting<String> alphaVantageYear = new Setting<>("year", "2000");
    private final Setting<String> alphaVantageMonth = new Setting<>("month", "01");
    private final Setting<String> alphaVantageYearStart = new Setting<>("yearStart", "2000");
    private final Setting<String> alphaVantageMonthStart = new Setting<>("monthStart", "01");
    private final Setting<String> alphaVantageYearEnd = new Setting<>("yearEnd", "2001");
    private final Setting<String> alphaVantageMonthEnd = new Setting<>("monthEnd", "01");


    //Center Elements
    private final TextUI title = new TextUI("Stock Downloader", new Font("Arial Black", Font.BOLD, 50), Vector.NULL, ColorE.WHITE);

    private final TextFieldUI stockTickerField = new TextFieldUI(Vector.NULL, new Vector(100, 20), ColorE.WHITE, stockTicker, "Stock", false);
    private final ModeCycleUI<DownloadTimeFrame> timeFrameMode = new ModeCycleUI<>(Vector.NULL, new Vector(100, 20), ColorE.BLUE, timeFrame, DownloadTimeFrame.ONE_SECOND, DownloadTimeFrame.FIVE_SECONDS, DownloadTimeFrame.THIRTY_SECONDS, DownloadTimeFrame.ONE_MINUTE, DownloadTimeFrame.FIVE_MINUTES, DownloadTimeFrame.THIRTY_MINUTES, DownloadTimeFrame.ONE_HOUR, DownloadTimeFrame.TWO_HOUR, DownloadTimeFrame.FOUR_HOUR, DownloadTimeFrame.ONE_DAY);

    private final ProgressBarUI<Double> progressBarApiDownload = new ProgressBarUI<>(Vector.NULL, new Vector(200, 20), ColorE.BLUE, new Container<>(0d), 0, 1);
    private final TextUI warningText = new TextUI("", Fonts.getDefaultFont(20), Vector.NULL, ColorE.WHITE);

    private final ButtonUI downloadButton = new ButtonUI("Download!", Vector.NULL, new Vector(200, 60), new ColorE(0, 125, 200, 200), ButtonUI.MouseButton.LEFT, () -> {
        System.out.println(SettingManager.getDefaultManager().saveAll() ? "Saved" : "Could Not Save");
        if (stockTickerField.getContainer().get().equals("")) return;

        switch (downloadMode.get()) {
            case "Live Data" -> getWindow().setScene(new LiveDownloadScene(new LiveDownloadStock(stockTicker.get().replace(" ", ""), timeFrame.get(), liveExtendedHours.get(), livePriceSource.get())));

            case "API" -> {
                updateApiDownloader();
                warningText.setText("");
                progressBarApiDownload.setContainer(apiDownloader.getDownloadProgress());
                progressBarApiDownload.setRendered(true);

                if (apiDownloader.isDownloadActive().get()) {
                    warningText.setText("Download Already In Progress!").setColor(ColorE.RED);
                } else {
                    switch (api.get()) {
                        case "AlphaVantage" -> runAlphaVantageDownload();
                        case "OtherAPI" -> System.out.println("Currently Unimplemented");
                    }
                }

            }

        }
    });

    private final TooltipUI tooltipDownloadButton = new TooltipUI(downloadButton, new Vector(264, 70), ColorE.BLUE.alpha(200),
            new TextUI("AlphaVantage API has a daily limit of\\n25 downloads per day. Be mindful that\\nwithout a premium key, this limit may\\nimpact downloads", Fonts.getDefaultFont(15), new Vector(4, 2), ColorE.WHITE));



    //Sidebar Elements
    private final int yInc = 25;
    private final ModeCycleUI<String> modeDownloadMode = new ModeCycleUI<>(new Vector(15, 55), new Vector(110, 20), ColorE.BLUE, downloadMode, "Live Data", "API");

    private final ToggleUI toggleLiveExtendedHours = new ToggleUI("Extended Hours", new Vector(15, modeDownloadMode.getPos().getY() + 50), new Vector(110, 20), new ColorE(0, 200, 255, 255), liveExtendedHours);
    private final ModeCycleUI<LiveDownloadStock.PriceSource> modeLivePriceSource = new ModeCycleUI<>("Source", new Vector(15, toggleLiveExtendedHours.getPos().getY() + yInc), new Vector(110, 20), ColorE.BLUE, livePriceSource, LiveDownloadStock.PriceSource.MARKETWATCH, LiveDownloadStock.PriceSource.YAHOOFINANCE);

    private final ModeCycleUI<String> modeApi = new ModeCycleUI<>(new Vector(15, modeDownloadMode.getPos().getY() + 50), new Vector(110, 20), ColorE.BLUE, api, "AlphaVantage");

    private final TextFieldUI fieldAlphaVantageKey = new TextFieldUI(new Vector(15, modeApi.getPos().getY() + 60), new Vector(110, 20), ColorE.WHITE, alphaVantageKey, "API Key", false);
    private final ToggleUI toggleAlphaVantagePremium = new ToggleUI("Is Key Premium", new Vector(15, fieldAlphaVantageKey.getPos().getY() + yInc), new Vector(110, 20), new ColorE(0, 200, 255, 255), alphaVantagePremium);
    private final ToggleUI toggleAlphaVantageExtendedHours = new ToggleUI("Extended Hours", new Vector(15, toggleAlphaVantagePremium.getPos().getY() + yInc), new Vector(110, 20), new ColorE(0, 200, 255, 255), alphaVantageExtendedHours);
    private final ModeCycleUI<String> modeAlphaVantageTime = new ModeCycleUI<>("Time", new Vector(15, toggleAlphaVantageExtendedHours.getPos().getY() + yInc), new Vector(110, 20), ColorE.BLUE, alphaVantageTime, "Month", "All", "Range");
    private final TextFieldUI fieldAlphaVantageYear = new TextFieldUI(new Vector(88, modeAlphaVantageTime.getPos().getY() + yInc), new Vector(37, 20), ColorE.WHITE, alphaVantageYear, "", true, 4);
    private final TextFieldUI fieldAlphaVantageMonth = new TextFieldUI(new Vector(56, modeAlphaVantageTime.getPos().getY() + yInc), new Vector(22, 20), ColorE.WHITE, alphaVantageMonth, "", true, 2);
    private final TextFieldUI fieldAlphaVantageYearStart = new TextFieldUI(new Vector(88, modeAlphaVantageTime.getPos().getY() + yInc), new Vector(37, 20), ColorE.WHITE, alphaVantageYearStart, "", true, 4);
    private final TextFieldUI fieldAlphaVantageMonthStart = new TextFieldUI(new Vector(56, modeAlphaVantageTime.getPos().getY() + yInc), new Vector(22, 20), ColorE.WHITE, alphaVantageMonthStart, "", true, 2);
    private final TextFieldUI fieldAlphaVantageYearEnd = new TextFieldUI(new Vector(88, fieldAlphaVantageYearStart.getPos().getY() + yInc), new Vector(37, 20), ColorE.WHITE, alphaVantageYearEnd, "", true, 4);
    private final TextFieldUI fieldAlphaVantageMonthEnd = new TextFieldUI(new Vector(56, fieldAlphaVantageMonthStart.getPos().getY() + yInc), new Vector(22, 20), ColorE.WHITE, alphaVantageMonthEnd, "", true, 2);

    private final ButtonUI buttonApiCombineLive = new ButtonUI("Combine To Live", new Vector(15, 0), new Vector(110, 20), new ColorE(0, 200, 255, 150), ButtonUI.MouseButton.ALL, () -> {
        updateApiDownloader();

        if (apiDownloader.isDownloadActive().get()) {
            warningText.setText("Download In Progress. Wait Until Finished!").setColor(ColorE.RED);
        } else {
            switch (api.get()) {
                case "AlphaVantage" -> runAlphaVantageCombineLive();
                case "OtherAPI" -> System.out.println("Currently Unimplemented");
            }
        }
    });

    private final TooltipUI tooltipButtonApiCombineLive = new TooltipUI(buttonApiCombineLive, new Vector(-100, 70), ColorE.BLUE.alpha(200), new RectangleUI(Vector.NULL, new Vector(34, 70), ColorE.BLUE.alpha(200)),
            new TextUI("Combines all API\\ndata and live data\\ninto one main .csv\\nfile in /stock_data/", Fonts.getDefaultFont(15), new Vector(-100 + 4, 2), ColorE.WHITE));

    private final SideBarUI sideBarSettings = new SideBarUI("Settings", SideBarUI.Type.RIGHT, 140, false, new ColorE(0, 125, 200, 200), modeDownloadMode,toggleLiveExtendedHours,modeApi,fieldAlphaVantageKey,toggleAlphaVantagePremium,toggleAlphaVantageExtendedHours,modeAlphaVantageTime,fieldAlphaVantageYear,fieldAlphaVantageMonth,fieldAlphaVantageYearStart,fieldAlphaVantageMonthStart,fieldAlphaVantageYearEnd,fieldAlphaVantageMonthEnd,modeLivePriceSource,buttonApiCombineLive, tooltipButtonApiCombineLive);



    //DoOnce Instantiation
    private final DoOnce doInit = new DoOnce();

    public TitleScene() {
        super("Title");
        doInit.reset();
        SettingManager.getDefaultManager().loadAll();
    }

    private void initScene() {
        doInit.run(() -> {
            App.getWindow().setEconomic(false);

            //Add Bouncing Squares
            Random random = new Random();
            for (int i = 0; i < 20; i++) {
                int speed = 30;
                Vector speedVec = new Vector(random.nextInt(-speed,speed),random.nextInt(-speed,speed));
                addElements(new PhysicsDraggableUI(new RectangleUI(getSize().getMultiplied(.5), new Vector(10, 10), new ColorE(random.nextInt(0,255), random.nextInt(0,255), random.nextInt(0,255), 255)), 1, speedVec, Vector.NULL));
            }

            //Set Progress Bar Off By Default
            progressBarApiDownload.setRendered(false);

            //Add Widgets
            addElements(downloadButton, stockTickerField, timeFrameMode, title, sideBarSettings, progressBarApiDownload, warningText, tooltipDownloadButton);
        });
    }

    @Override
    public void draw() {
        initScene();

        drawBackground(new ColorE(50, 50, 50, 255));

        //Draw Widget Backgrounds
        QuickDraw.drawRect(stockTickerField.getPos(), stockTickerField.getSize(), new ColorE(100, 100, 100, 255));
        QuickDraw.drawRect(timeFrameMode.getPos(), timeFrameMode.getSize(), new ColorE(100, 100, 100, 255));
        if (progressBarApiDownload.shouldRender()) QuickDraw.drawRect(progressBarApiDownload.getPos(), progressBarApiDownload.getSize(), new ColorE(100, 100, 100, 255));

        updateWarningText();

        updateElementPositions();
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

    private void updateElementPositions() {
        double yOffset = -40;

        //Set Floating Title
        title.setPos(getSize().getMultiplied(.5d).getAdded(title.getSize().getMultiplied(-.5)).getAdded(0, yOffset));
        title.setPos(title.getPos().getAdded(new Vector(0, Math.sin(step) * 8)));
        step += 0.05;
        if (step >= Math.PI * 2) step = 0;

        //Set Widget Positions
        stockTickerField.setPos(getSize().getMultiplied(.5d).getAdded(stockTickerField.getSize().getMultiplied(-.5)).getAdded(-stockTickerField.getSize().getX(), 140).getAdded(0, yOffset));
        timeFrameMode.setPos(getSize().getMultiplied(.5d).getAdded(timeFrameMode.getSize().getMultiplied(-.5)).getAdded(+timeFrameMode.getSize().getX(), 140).getAdded(0, yOffset));
        downloadButton.setPos(getSize().getMultiplied(.5d).getAdded(downloadButton.getSize().getMultiplied(-.5)).getAdded(0, title.getFont().getSize() + 30).getAdded(0, yOffset));
        progressBarApiDownload.setPos(downloadButton.getPos().getAdded(0, 120));
        warningText.setPos(getSize().getMultiplied(.5).getAdded(warningText.getSize().getMultiplied(-.5)).getAdded(0, 170));
        buttonApiCombineLive.setPos(new Vector(buttonApiCombineLive.getPos().getX(), getSize().getY() - buttonApiCombineLive.getSize().getY() - 15));
    }

    private void updateWarningText() {
        if (apiDownloader != null && !apiDownloader.isDownloadActive().get() && apiDownloader.isDownloadFinished().get()) {
            if (apiDownloader.isDownloadSuccessful().get()) {
                warningText.setText("Download Finished Successfully!").setColor(ColorE.GREEN);
            } else {
                if (apiDownloader instanceof AlphaVantageDownloader avd) {
                    warningText.setText(avd.isLimitReached() ? "Error! Daily Download Limit Reached!\\n Successful Downloads Saved!" : "Download Failed!").setColor(ColorE.RED);
                } else {
                    warningText.setText("Download Failed!").setColor(ColorE.RED);
                }
            }
        }
    }

    private void updateApiDownloader() {
        switch (api.get()) {
            case "AlphaVantage" -> apiDownloader = new AlphaVantageDownloader(alphaVantageKey.getKey(), alphaVantagePremium.get(), stockTicker.get(), timeFrame.get(), alphaVantageExtendedHours.get());
            case "OtherAPI" -> System.out.println("Not Implemented"); //There are no other APIs currently
            default -> apiDownloader = null;
        }
    }


    private void drawSidebarData() {
        QuickDraw.drawTextCentered("Download Mode:", new Font("Arial", Font.PLAIN, 16), sideBarSettings.getPos().getAdded(0, 45), new Vector(sideBarSettings.getWidth(), 0), ColorE.WHITE);
        switch (downloadMode.get()) {

            case "Live Data" -> {
                //Live Data Settings
                toggleLiveExtendedHours.setEnabled(true);
                modeLivePriceSource.setEnabled(true);

                //API Settings
                modeApi.setEnabled(false);
                buttonApiCombineLive.setEnabled(false);

                //Tooltips
                tooltipDownloadButton.setEnabled(false);
                tooltipButtonApiCombineLive.setEnabled(false);

                //Alpha Vantage Settings
                fieldAlphaVantageKey.setEnabled(false);
                toggleAlphaVantagePremium.setEnabled(false);
                toggleAlphaVantageExtendedHours.setEnabled(false);
                modeAlphaVantageTime.setEnabled(false);
                fieldAlphaVantageMonth.setEnabled(false);
                fieldAlphaVantageYear.setEnabled(false);
                fieldAlphaVantageMonthStart.setEnabled(false);
                fieldAlphaVantageYearStart.setEnabled(false);
                fieldAlphaVantageMonthEnd.setEnabled(false);
                fieldAlphaVantageYearEnd.setEnabled(false);
            }

            case "API" -> {
                QuickDraw.drawTextCentered("API:", new Font("Arial", Font.PLAIN, 16), sideBarSettings.getPos().getAdded(0, 95), new Vector(sideBarSettings.getWidth(), 0), ColorE.WHITE);

                //Live Data Settings
                toggleLiveExtendedHours.setEnabled(false);
                modeLivePriceSource.setEnabled(false);

                //API Settings
                modeApi.setEnabled(true);
                buttonApiCombineLive.setEnabled(true);

                //Tooltips
                tooltipDownloadButton.setEnabled(true);
                tooltipButtonApiCombineLive.setEnabled(true);

                //Alpha Vantage Settings
                boolean isAlphaVantage = api.get().equals("AlphaVantage");
                fieldAlphaVantageKey.setEnabled(isAlphaVantage);
                toggleAlphaVantagePremium.setEnabled(isAlphaVantage);
                toggleAlphaVantageExtendedHours.setEnabled(isAlphaVantage);
                modeAlphaVantageTime.setEnabled(isAlphaVantage);

                boolean isMonthMode = alphaVantageTime.get().equals("Month");
                fieldAlphaVantageMonth.setEnabled(isMonthMode && isAlphaVantage);
                fieldAlphaVantageYear.setEnabled(isMonthMode && isAlphaVantage);

                boolean isRangeMode = alphaVantageTime.get().equals("Range");
                fieldAlphaVantageMonthStart.setEnabled(isRangeMode && isAlphaVantage);
                fieldAlphaVantageYearStart.setEnabled(isRangeMode && isAlphaVantage);
                fieldAlphaVantageMonthEnd.setEnabled(isRangeMode && isAlphaVantage);
                fieldAlphaVantageYearEnd.setEnabled(isRangeMode && isAlphaVantage);

                switch (api.get()) {
                    case "AlphaVantage" -> {
                        QuickDraw.drawTextCentered("Alpha Vantage Settings:", new Font("Arial", Font.PLAIN, 12), sideBarSettings.getPos().getAdded(0, 150), new Vector(sideBarSettings.getWidth(), 0), ColorE.WHITE);

                        switch (alphaVantageTime.get()) {
                            case "Month" -> {
                                QuickDraw.drawText("Month:", new Font("Arial", Font.PLAIN, 14), sideBarSettings.getPos().getAdded(15, fieldAlphaVantageMonth.getPos().getY() + 1), ColorE.WHITE);
                                QuickDraw.drawText("/", new Font("Arial", Font.PLAIN, 16), sideBarSettings.getPos().getAdded(fieldAlphaVantageMonth.getPos()).getAdded(new Vector(fieldAlphaVantageMonth.getSize().getX() + 3, 0)), ColorE.WHITE);
                            }

                            case "Range" -> {
                                QuickDraw.drawText("Start:", new Font("Arial", Font.PLAIN, 14), sideBarSettings.getPos().getAdded(15, fieldAlphaVantageMonthStart.getPos().getY() + 1), ColorE.WHITE);
                                QuickDraw.drawText("End:", new Font("Arial", Font.PLAIN, 14), sideBarSettings.getPos().getAdded(15, fieldAlphaVantageMonthEnd.getPos().getY() + 1), ColorE.WHITE);
                                QuickDraw.drawText("/", new Font("Arial", Font.PLAIN, 16), sideBarSettings.getPos().getAdded(fieldAlphaVantageMonthStart.getPos()).getAdded(new Vector(fieldAlphaVantageMonth.getSize().getX() + 3, 0)), ColorE.WHITE);
                                QuickDraw.drawText("/", new Font("Arial", Font.PLAIN, 16), sideBarSettings.getPos().getAdded(fieldAlphaVantageMonthEnd.getPos()).getAdded(new Vector(fieldAlphaVantageMonth.getSize().getX() + 3, 0)), ColorE.WHITE);
                            }

                        }

                    }

                    case "OtherAPI" -> System.out.println("Not Yet Implemented");
                }

            }
            default -> {
                for (ElementUI element : sideBarSettings.getElementList()) element.setEnabled(false);
            }
        }
    }


    private void runAlphaVantageDownload() {
        AlphaVantageDownloader downloader = (AlphaVantageDownloader) apiDownloader;

        //Download data based on month mode
        Thread thread = new Thread(() -> {
            switch (alphaVantageTime.get()) {

                case "Month" -> {
                    if (alphaVantageYear.get().length() == 4 && alphaVantageMonth.get().length() == 2) {
                        downloader.download(Integer.parseInt(alphaVantageYear.get()), Integer.parseInt(alphaVantageMonth.get()));
                    } else {
                        warningText.setText("Invalid Time! - Make sure time is in the form: MM / YYYY").setColor(ColorE.RED);
                    }
                }

                case "Range" -> {
                    if (alphaVantageYearStart.get().length() == 4 && alphaVantageMonthStart.get().length() == 2 && alphaVantageYearEnd.get().length() == 4 && alphaVantageMonthEnd.get().length() == 2) {
                        downloader.download(Integer.parseInt(alphaVantageYearStart.get()), Integer.parseInt(alphaVantageMonthStart.get()), Integer.parseInt(alphaVantageYearEnd.get()), Integer.parseInt(alphaVantageMonthEnd.get()));
                    } else {
                        warningText.setText("Invalid Time! - Make sure time is in the form: MM / YYYY").setColor(ColorE.RED);
                    }
                }

                case "All" -> downloader.downloadAll();

                default -> System.out.println("Nothing left here lol");
            }
        });
        thread.setName("AlphaVantage Download Thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void runAlphaVantageCombineLive() {
        AlphaVantageDownloader downloader = (AlphaVantageDownloader) apiDownloader;

        if (downloader.combineToLiveFile()) {
            warningText.setText("Data Combined to: " + "stock_data/" + downloader.getTicker() + "_" + downloader.getTimeFrame().getTag() + ".csv").setColor(ColorE.GREEN);
        } else {
            warningText.setText("Error combining data").setColor(ColorE.RED);
        }
    }


    private void doPhysicsBounce(PhysicsObjectUI phys) {
        int size = 10;
        if (phys.getPos().getX() < 0) {
            phys.setPos(new Vector(0, phys.getPos().getY()));
            phys.setVelocity(new Vector(-phys.getVelocity().getX(), phys.getVelocity().getY()));
        }
        if (phys.getPos().getX() + size > getSize().getX()) {
            phys.setPos(new Vector(getSize().getX() - size, phys.getPos().getY()));
            phys.setVelocity(new Vector(-phys.getVelocity().getX(), phys.getVelocity().getY()));
        }
        if (phys.getPos().getY() < 0) {
            phys.setPos(new Vector(phys.getPos().getX(), 0));
            phys.setVelocity(new Vector(phys.getVelocity().getX(), -phys.getVelocity().getY()));
        }
        if (phys.getPos().getY() + size > getSize().getY()) {
            phys.setPos(new Vector(phys.getPos().getX(), getSize().getY() - size));
            phys.setVelocity(new Vector(phys.getVelocity().getX(), -phys.getVelocity().getY()));
        }
    }
}
