package ch.kalunight.zoe.model;

import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.core.entities.Message;

public class ControlPannel {
  
  private List<Message> messagesList;

  public ControlPannel() {
    messagesList = new ArrayList<>();
  }

  public List<Message> getMessagesList() {
    return messagesList;
  }

  public void setMessagesList(List<Message> messagesList) {
    this.messagesList = messagesList;
  }
  
}
