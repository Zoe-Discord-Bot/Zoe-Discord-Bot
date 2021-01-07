package ch.kalunight.zoe.util;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Message;

public class LanguageUtil {

  private LanguageUtil() {
    //Hide default public constructor
  }

  public static Function<Integer, String> getUpdateMessageAfterChangeSelectAction(String language, List<String> choices) {
    return new Function<Integer, String>() {
      @Override
      public String apply(Integer index) {
        return String.format(LanguageManager.getText(language, "languageCommandInSelectionMenu"), choices.get(index - 1));
      }
    };
  }

  public static Consumer<Message> getCancelActionSelection(){
    return new Consumer<Message>() {
      @Override
      public void accept(Message message) {
        message.clearReactions().queue();
      }};
  }

}
