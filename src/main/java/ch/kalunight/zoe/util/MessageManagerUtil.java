package ch.kalunight.zoe.util;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;

public class MessageManagerUtil {

  private static final Logger logger = LoggerFactory.getLogger(MessageManagerUtil.class);
  
  private MessageManagerUtil() {
    // hide public default constructor
  }
  
  public static void editOrCreateTheseMessages(List<Long> messageListToUpdate, TextChannel channel, String messageToSend) {

    List<Message> messageToEditOrDelete = new ArrayList<>();
    List<Long> messagesNotFound = new ArrayList<>();

    for(Long messageId : messageListToUpdate) {
      try {
        messageToEditOrDelete.add(channel.retrieveMessageById(messageId).complete());
      }catch(ErrorResponseException e) {
        if(!e.getErrorResponse().equals(ErrorResponse.UNKNOWN_MESSAGE)) {
          logger.error("Unexpected error when getting a message", e);
          throw e;
        }else {
          messagesNotFound.add(messageId);
        }
      }
    }

    messageListToUpdate.removeAll(messagesNotFound);

    List<String> messagesToSendCutted = MessageUtil.splitMessageToBeSendable(messageToSend); 

    if(messageToEditOrDelete.size() > messagesToSendCutted.size()) {
      int messageToGet = 0;

      for(Message messageToTreat : messageToEditOrDelete) {
        if(messageToGet < messagesToSendCutted.size()) {
          messageToTreat.editMessage(messagesToSendCutted.get(messageToGet)).complete();
          messageToGet++;
        }else {
          messageListToUpdate.remove(messageToTreat.getIdLong());
          messageToTreat.delete().complete();
        }
      }
    }else if (messageToEditOrDelete.size() == messagesToSendCutted.size()) {
      int messageToGet = 0;
      for(Message messageToTreat : messageToEditOrDelete) {
        messageToTreat.editMessage(messagesToSendCutted.get(messageToGet)).complete();
        messageToGet++;
      }
    }else {
      int messagesAlreadyCreated = messageToEditOrDelete.size();
      int messagesAlreadyTreated = 0;

      for(String messageToEditOrCreate : messagesToSendCutted) {
        if(messagesAlreadyTreated < messagesAlreadyCreated) {
          messageToEditOrDelete.get(messagesAlreadyTreated).editMessage(messageToEditOrCreate).complete();
          messagesAlreadyTreated++;
        }else {
          messageListToUpdate.add(channel.sendMessage(messageToEditOrCreate).complete().getIdLong());
        }
      }
    }
  }
  
}
