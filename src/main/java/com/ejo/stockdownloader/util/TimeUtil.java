package com.ejo.stockdownloader.util;

import com.ejo.glowlib.time.DateTime;

@Deprecated
public class TimeUtil {

    public static long getSecondDiff(DateTime timeFinal, DateTime timeInitial) {
        return (timeFinal.getCalendar().getTimeInMillis() / 1000) - (timeInitial.getCalendar().getTimeInMillis() / 1000);
    }

    public static double getDateTimePercent(DateTime start, DateTime current, DateTime end) {
        return (double) getSecondDiff(current, start) / (getSecondDiff(end,start));
    }

}
