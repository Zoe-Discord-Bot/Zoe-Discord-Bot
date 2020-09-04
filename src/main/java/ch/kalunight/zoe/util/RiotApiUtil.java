package ch.kalunight.zoe.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.rithms.riot.api.RiotApiException;

public class RiotApiUtil {

  private static final Logger logger = LoggerFactory.getLogger(RiotApiUtil.class);
  
  private RiotApiUtil() {
    //hide default public constructor
  }
  
  public static void handleRiotApi(MessageReceivedEvent event, RiotApiException e, String language) {
    if(e.getErrorCode() == RiotApiException.SERVER_ERROR) {
      event.getChannel().sendMessage(LanguageManager.getText(language, "riotApiSummonerByNameError500")).queue();
    }else if(e.getErrorCode() == RiotApiException.UNAVAILABLE) {
      event.getChannel().sendMessage(LanguageManager.getText(language, "riotApiSummonerByNameError503")).queue();
    }else if(e.getErrorCode() == RiotApiException.RATE_LIMITED) {
      event.getChannel().sendMessage(LanguageManager.getText(language, "riotApiSummonerByNameError429")).queue();
      logger.info("Receive a {} error code : {}", e.getErrorCode(), e.getMessage());
    }else if (e.getErrorCode() == RiotApiException.DATA_NOT_FOUND){
      event.getChannel().sendMessage(LanguageManager.getText(language, "riotApiSummonerByNameError404")).queue();
    }else {
      event.getChannel().sendMessage(String.format(LanguageManager.getText(language, "riotApiSummonerByNameErrorUnexpected"), e.getErrorCode())).queue();
      logger.warn("Unexpected error with a riot api request.", e);
    }
  }
}
