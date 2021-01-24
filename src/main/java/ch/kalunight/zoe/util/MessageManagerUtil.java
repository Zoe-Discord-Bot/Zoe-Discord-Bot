package ch.kalunight.zoe.util;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.command.CommandEvent;

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

    List<String> messagesToSendCutted = CommandEvent.splitMessage(messageToSend); 

    if(messageToEditOrDelete.size() > messagesToSendCutted.size()) {
      int messagesToTreat = messagesToSendCutted.size();
      int messageToGet = 0;

      for(Message messageToTreat : messageToEditOrDelete) {
        if(messagesToTreat != 0) {
          messageToTreat.editMessage(messagesToSendCutted.get(messageToGet)).queue();
          messageToGet++;
          messagesToTreat++;
        }else {
          messageListToUpdate.remove(messageToTreat.getIdLong());
          messageToTreat.delete().queue();
        }
      }
    }else if (messageToEditOrDelete.size() == messagesToSendCutted.size()) {
      int messageToGet = 0;
      for(Message messageToTreat : messageToEditOrDelete) {
        messageToTreat.editMessage(messagesToSendCutted.get(messageToGet)).queue();
        messageToGet++;
      }
    }else {
      int messagesAlreadyCreated = messageToEditOrDelete.size();
      int messagesAlreadyTreated = 0;

      for(String messageToEditOrCreate : messagesToSendCutted) {
        if(messagesAlreadyTreated < messagesAlreadyCreated) {
          messageToEditOrDelete.get(messagesAlreadyTreated).editMessage(messageToEditOrCreate).queue();
          messagesAlreadyTreated++;
        }else {
          messageListToUpdate.add(channel.sendMessage(messageToEditOrCreate).complete().getIdLong());
        }
      }
    }
  }
  
}
