package ch.kalunight.zoe.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class MongodbDateUtil {

  private MongodbDateUtil() {
    // hide default public constructor
  }
  
  public static Date toDate(LocalDateTime ldt) {
    return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
  }
}
