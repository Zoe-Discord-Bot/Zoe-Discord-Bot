package ch.kalunight.zoe.util;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class TimeZoneUtil {

  private TimeZoneUtil() {
    //hide default public constructor
  }

  public static String displayTimeZone(TimeZone tz) {

    long hours = TimeUnit.MILLISECONDS.toHours(tz.getRawOffset());
    long minutes = TimeUnit.MILLISECONDS.toMinutes(tz.getRawOffset()) 
        - TimeUnit.HOURS.toMinutes(hours);
    // avoid -4:-30 issue
    minutes = Math.abs(minutes);

    String result = "";
    if (hours > 0) {
      result = String.format("(UTC+%d:%02d) %s", hours, minutes, tz.getID());
    } else {
      result = String.format("(UTC%d:%02d) %s", hours, minutes, tz.getID());
    }

    return result;

  }
}
