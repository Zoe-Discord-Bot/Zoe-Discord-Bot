package ch.kalunight.zoe.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.translation.LanguageManager;
import net.rithms.riot.api.RiotApiException;

public class RiotApiUtil {

  private static final Logger logger = LoggerFactory.getLogger(RiotApiUtil.class);
  
  private RiotApiUtil() {
    //hide default public constructor
  }
  
  public static void handleRiotApi(CommandEvent event, RiotApiException e, String language) {
    if(e.getErrorCode() == RiotApiException.SERVER_ERROR) {
      event.reply(LanguageManager.getText(language, "riotApiSummonerByNameError500"));
    }else if(e.getErrorCode() == RiotApiException.UNAVAILABLE) {
      event.reply(LanguageManager.getText(language, "riotApiSummonerByNameError503"));
    }else if(e.getErrorCode() == RiotApiException.RATE_LIMITED) {
      event.reply(LanguageManager.getText(language, "riotApiSummonerByNameError429"));
      logger.info("Receive a {} error code : {}", e.getErrorCode(), e.getMessage());
    }else if (e.getErrorCode() == RiotApiException.DATA_NOT_FOUND){
      event.reply(LanguageManager.getText(language, "riotApiSummonerByNameError404"));
    }else {
      event.reply(String.format(LanguageManager.getText(language, "riotApiSummonerByNameErrorUnexpected"), e.getErrorCode()));
      logger.warn("Unexpected error in add accountToPlayer command.", e);
    }
  }
}
