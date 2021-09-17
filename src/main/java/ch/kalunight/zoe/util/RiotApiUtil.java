package ch.kalunight.zoe.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import no.stelar7.api.r4j.basic.exceptions.APIHTTPErrorReason;
import no.stelar7.api.r4j.basic.exceptions.APIResponseException;

public class RiotApiUtil {

  private static final Logger logger = LoggerFactory.getLogger(RiotApiUtil.class);

  private RiotApiUtil() {
    //hide default public constructor
  }

  public static void handleRiotApi(Message messageToEdit, APIResponseException e, String language) {
    messageToEdit.editMessage(getTextHandlerRiotApiError(e, language)).queue();
  }
  
  public static void handleRiotApi(MessageReceivedEvent event, APIResponseException e, String language) {
    handleRiotApi(event.getTextChannel(), e, language);
  }
  
  public static void handleRiotApi(TextChannel channel, APIResponseException e, String language) {
    channel.sendMessage(getTextHandlerRiotApiError(e, language)).queue();
  }
  
  public static String getTextHandlerRiotApiError(APIResponseException e, String language) {
    if(e.getReason() == APIHTTPErrorReason.ERROR_500) {
      return LanguageManager.getText(language, "riotApiSummonerByNameError500");
    }else if(e.getReason() == APIHTTPErrorReason.ERROR_429) {
      logger.info("Receive a {} error code : {}", e.getReason().getCode(), e.getMessage());
      return LanguageManager.getText(language, "riotApiSummonerByNameError429");
    }else if (e.getReason() == APIHTTPErrorReason.ERROR_404){
      return LanguageManager.getText(language, "riotApiSummonerByNameError404");
    }else {
      logger.warn("Unexpected error with a riot api request.", e);
      return String.format(LanguageManager.getText(language, "riotApiSummonerByNameErrorUnexpected"), e.getReason().getCode());
    }
  }
  
}
