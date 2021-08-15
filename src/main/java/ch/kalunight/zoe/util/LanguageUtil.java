package ch.kalunight.zoe.util;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import ch.kalunight.zoe.command.LanguageCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.static_data.Mastery;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Message;
import no.stelar7.api.r4j.pojo.lol.championmastery.ChampionMastery;

public class LanguageUtil {

  private LanguageUtil() {
    //Hide default public constructor
  }

  public static Function<Integer, String> getUpdateMessageAfterChangeSelectAction(String language, List<String> choices, Server server) {
    return new Function<Integer, String>() {
      @Override
      public String apply(Integer index) {
        return String.format(LanguageManager.getText(server.getLanguage(),
            "languageCommandStartMessage"), LanguageManager.getText(server.getLanguage(), LanguageCommandRunnable.NATIVE_LANGUAGE_TRANSLATION_ID), "<https://discord.gg/AyAYWGM>")
            + "\n" + String.format(LanguageManager.getText(language, "languageCommandInSelectionMenu"), choices.get(index - 1));
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

  public static String convertMasteryToReadableText(ChampionMastery mastery) {
    StringBuilder masteryString = new StringBuilder();
  
    long points = mastery.getChampionPoints();
    if(points > 1000 && points < 1000000) {
      masteryString.append(points / 1000 + "K");
    } else if(points > 1000000) {
      masteryString.append(points / 1000000 + "M");
    } else {
      masteryString.append(Long.toString(points));
    }
  
    try {
      Mastery masteryLevel = Mastery.getEnum(mastery.getChampionLevel());
      masteryString.append(Ressources.getMasteryEmote().get(masteryLevel).getEmote().getAsMention());
    } catch(NullPointerException | IllegalArgumentException e) {
      masteryString.append("");
    }
  
    return masteryString.toString();
  }

}
