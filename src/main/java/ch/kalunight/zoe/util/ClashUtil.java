package ch.kalunight.zoe.util;

import com.google.common.base.CharMatcher;

import ch.kalunight.zoe.translation.LanguageManager;

public class ClashUtil {

  private ClashUtil() {
    //hide public default class
  }
  
  /**
   * Utility method to parse the "nameKeySecondary" of the clash tournament API.
   * @param language
   * @param nameKeySecondary
   * @return the user friendly
   */
  public static String parseDayId(String language, String nameKeySecondary) {
    String dayNumber = CharMatcher.inRange('0', '9').retainFrom(nameKeySecondary);
    return String.format(LanguageManager.getText(language, "dayNumber"), dayNumber);
  }
  
}
