package ch.kalunight.zoe.model;

import net.dv8tion.jda.api.entities.Message;

public class ComparableMessage implements Comparable<ComparableMessage>{

  private Message message;

  public ComparableMessage(Message message) {
    this.message = message;
  }
  
  /**
   * Older is first in the list
   */
  @Override
  public int compareTo(ComparableMessage o) {
    
    if(message.getTimeCreated().equals(o.getMessage().getTimeCreated())) {
      return 0;
    }
    
    if(message.getTimeCreated().isBefore(o.getMessage().getTimeCreated())) {
      return -1;
    }else {
      return 1;
    }
  }
  
  public Message getMessage() {
    return message;
  }



}
