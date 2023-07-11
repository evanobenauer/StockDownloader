package com.ejo.stockdownloader.util;

public enum TimeFrame {

    ONE_SECOND("1sec", 1),
    FIVE_SECONDS("5sec", 5),
    THIRTY_SECONDS("30sec", 30),
    ONE_MINUTE("1min", 60),
    FIVE_MINUTES("5min", 5 * 60),
    FIFTEEN_MINUTES("15min", 15 * 60),
    THIRTY_MINUTES("30min", 30 * 60),
    ONE_HOUR("60min", 60 * 60),
    TWO_HOUR("2hr", 2 * 60 * 60),
    FOUR_HOUR("4hr", 4 * 60 * 60),
    ONE_DAY("1day", 8 * 60 * 60);

    private final String tag;
    private final int seconds;

    TimeFrame(String tag, int seconds) {
        this.tag = tag;
        this.seconds = seconds;
    }

    public String getTag() {
        return tag;
    }

    public int getSeconds() {
        return seconds;
    }

    @Override
    public String toString() {
        return getTag();
    }
}