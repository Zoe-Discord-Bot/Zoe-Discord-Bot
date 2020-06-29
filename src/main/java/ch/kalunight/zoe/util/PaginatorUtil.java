package ch.kalunight.zoe.util;

import java.util.function.BiFunction;
import ch.kalunight.zoe.translation.LanguageManager;

public class PaginatorUtil {

  private PaginatorUtil() {
    //Hide default constructor
  }
  
  public static BiFunction<Integer, Integer, String> getPaginationTranslatedPage(String language){
    return new BiFunction<Integer, Integer, String>() {
      
      @Override
      public String apply(Integer actualPage, Integer maxPage) {
        return String.format(LanguageManager.getText(language, "paginationTranslatedPage"), actualPage, maxPage);
      }
    };
  }
}
