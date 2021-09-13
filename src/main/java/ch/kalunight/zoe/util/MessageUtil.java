package ch.kalunight.zoe.util;

import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.entities.Message;

public class MessageUtil {
  
  private static final String PATTERN_SPLIT_ONE_LINE = "\\r?\\n";
  private static final String PATTERN_SPLIT_TWO_NEW_LINES = "(\\n\\s+\\n|\\n\\n|\\n \\n)";

  private MessageUtil() {
    // hide default public constructor
  }
  
  public static List<String> splitMessageToBeSendable(String messageToSplit){
    
    List<String> listSendableMessage = new ArrayList<>();
    
    if(messageToSplit == null) {
      return listSendableMessage;
    }
    
    if(messageToSplit.length() <= Message.MAX_CONTENT_LENGTH) {
      listSendableMessage.add(messageToSplit);
      return listSendableMessage;
    }
    
    listSendableMessage = splitString(messageToSplit, PATTERN_SPLIT_TWO_NEW_LINES);
    
    List<String> finalMessageList = new ArrayList<>();
    
    for(String messageToCheck : listSendableMessage) {
      if(messageToCheck.isEmpty()) {
        continue;
      }
      
      if(messageToCheck.length() > Message.MAX_CONTENT_LENGTH) {
        finalMessageList.addAll(splitString(messageToCheck, PATTERN_SPLIT_ONE_LINE));
      }else {
        finalMessageList.add(messageToCheck);
      }
    }
    
    return finalMessageList;
  }

  private static List<String> splitString(String messageToSplit, String separator) {
    List<String> sendableMessage = new ArrayList<>();
    String messageToWorkWith = messageToSplit.replace("@everyone", "@\u0435veryone").replace("@here", "@h\u0435re").trim();
    
    String[] messageSplited = messageToWorkWith.split(separator);
    StringBuilder messageAccumulation = new StringBuilder();
    
    for(String partOfMessage : messageSplited) {
      if(messageAccumulation.length() + partOfMessage.length() > Message.MAX_CONTENT_LENGTH) {
        sendableMessage.add(messageAccumulation.toString());
        if(separator.equals(PATTERN_SPLIT_TWO_NEW_LINES)) {
          messageAccumulation = new StringBuilder(partOfMessage + "\n \n");
        } else {
          messageAccumulation = new StringBuilder(partOfMessage + "\n");
        }
        
      }else {
        if(separator.equals(PATTERN_SPLIT_TWO_NEW_LINES)) {
          messageAccumulation.append(partOfMessage + "\n \n");
        } else {
          messageAccumulation.append(partOfMessage + "\n");
        }
      }
    }
    
    if(!messageAccumulation.toString().isEmpty()) {
      sendableMessage.add(messageAccumulation.toString());
    }
    
    return sendableMessage;
  }
}
