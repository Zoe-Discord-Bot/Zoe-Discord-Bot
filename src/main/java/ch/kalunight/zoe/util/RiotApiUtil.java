package ch.kalunight.zoe.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.rithms.riot.api.RiotApiException;

public class RiotApiUtil {

  private static final Logger logger = LoggerFactory.getLogger(RiotApiUtil.class);

  private RiotApiUtil() {
    //hide default public constructor
  }

  public static void handleRiotApi(Message messageToEdit, RiotApiException e, String language) {
    messageToEdit.editMessage(getTextHandlerRiotApiError(e, language)).queue();
  }
  
  public static void handleRiotApi(MessageReceivedEvent event, RiotApiException e, String language) {
    handleRiotApi(event.getTextChannel(), e, language);
  }
  
  public static void handleRiotApi(TextChannel channel, RiotApiException e, String language) {
    channel.sendMessage(getTextHandlerRiotApiError(e, language)).queue();
  }
  
  public static String getTextHandlerRiotApiError(RiotApiException e, String language) {
    if(e.getErrorCode() == RiotApiException.SERVER_ERROR) {
      return LanguageManager.getText(language, "riotApiSummonerByNameError500");
    }else if(e.getErrorCode() == RiotApiException.UNAVAILABLE) {
      return LanguageManager.getText(language, "riotApiSummonerByNameError503");
    }else if(e.getErrorCode() == RiotApiException.RATE_LIMITED) {
      logger.info("Receive a {} error code : {}", e.getErrorCode(), e.getMessage());
      return LanguageManager.getText(language, "riotApiSummonerByNameError429");
    }else if (e.getErrorCode() == RiotApiException.DATA_NOT_FOUND){
      return LanguageManager.getText(language, "riotApiSummonerByNameError404");
    }else {
      logger.warn("Unexpected error with a riot api request.", e);
      return String.format(LanguageManager.getText(language, "riotApiSummonerByNameErrorUnexpected"), e.getErrorCode());
    }
  }
  
}
