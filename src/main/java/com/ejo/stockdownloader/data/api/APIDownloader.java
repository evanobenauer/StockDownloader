package com.ejo.stockdownloader.data.api;

import com.ejo.glowlib.setting.Container;
import com.ejo.stockdownloader.util.DownloadTimeFrame;

public abstract class APIDownloader {

    private final String ticker;
    private final DownloadTimeFrame timeFrame;
    private final boolean extendedHours;

    //downloadProgress is on a scale of 0 to 1
    private final Container<Double> downloadProgress = new Container<>(0d);

    private final Container<Boolean> downloadActive = new Container<>(false);
    private final Container<Boolean> downloadFinished = new Container<>(false);
    private final Container<Boolean> downloadSuccess = new Container<>(false);

    public APIDownloader(String ticker, DownloadTimeFrame timeFrame, boolean extendedHours) {
        this.ticker = ticker;
        this.timeFrame = timeFrame;
        this.extendedHours = extendedHours;
    }


    protected void initDownloadContainers() {
        isDownloadFinished().set(false);
        isDownloadActive().set(true);
        isDownloadSuccessful().set(false);
        getDownloadProgress().set(0d);
    }

    protected void setDownloadProgress(double progress) {
        getDownloadProgress().set(progress);
    }

    protected void endDownloadContainers(boolean success) {
        getDownloadProgress().set(1d);
        isDownloadActive().set(false);
        isDownloadFinished().set(true);
        isDownloadSuccessful().set(success);
    }


    public Container<Boolean> isDownloadActive() {
        return downloadActive;
    }

    public Container<Boolean> isDownloadFinished() {
        return downloadFinished;
    }

    public Container<Boolean> isDownloadSuccessful() {
        return downloadSuccess;
    }

    public Container<Double> getDownloadProgress() {
        return downloadProgress;
    }


    public boolean isExtendedHours() {
        return extendedHours;
    }

    public DownloadTimeFrame getTimeFrame() {
        return timeFrame;
    }

    public String getTicker() {
        return ticker;
    }

}
