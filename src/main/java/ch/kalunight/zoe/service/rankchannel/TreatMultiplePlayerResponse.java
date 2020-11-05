package ch.kalunight.zoe.service.rankchannel;

import net.dv8tion.jda.api.entities.MessageEmbed;

public class TreatMultiplePlayerResponse {

  private TreatMultiplePlayer treatChoice;
  private MessageEmbed messageEmbed;
  
  public TreatMultiplePlayerResponse(TreatMultiplePlayer treatChoice, MessageEmbed messageEmbed) {
    this.treatChoice = treatChoice;
    this.messageEmbed = messageEmbed;
  }

  public TreatMultiplePlayer getTreatChoice() {
    return treatChoice;
  }

  public void setTreatChoice(TreatMultiplePlayer treatChoice) {
    this.treatChoice = treatChoice;
  }

  public MessageEmbed getMessageEmbed() {
    return messageEmbed;
  }

  public void setMessageEmbed(MessageEmbed messageEmbed) {
    this.messageEmbed = messageEmbed;
  }
}
