package com.jorge.thesis.util;

import java.util.concurrent.TimeUnit;

public abstract class TimeUtils {

    public static Long convertTimeTo(Long timeAmount, TimeUnit srcUnit, TimeUnit targetUnit) {
        Long ret;

        if (srcUnit == targetUnit)
            ret = timeAmount;
        else
            switch (targetUnit) {
                case NANOSECONDS:
                    ret = srcUnit.toNanos(timeAmount);
                    break;
                case MICROSECONDS:
                    ret = srcUnit.toMicros(timeAmount);
                    break;
                case MILLISECONDS:
                    ret = srcUnit.toMillis(timeAmount);
                    break;
                case SECONDS:
                    ret = srcUnit.toSeconds(timeAmount);
                    break;
                case MINUTES:
                    ret = srcUnit.toMinutes(timeAmount);
                    break;
                case HOURS:
                    ret = srcUnit.toHours(timeAmount);
                    break;
                case DAYS:
                    ret = srcUnit.toDays(timeAmount);
                    break;
                default:
                    throw new IllegalArgumentException("Time unit " + targetUnit + " not supported by converter.");
            }

        return ret;
    }
}
