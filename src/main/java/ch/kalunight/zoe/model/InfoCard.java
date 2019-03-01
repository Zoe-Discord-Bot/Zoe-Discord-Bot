package ch.kalunight.zoe.model;

import java.util.List;
import org.joda.time.DateTime;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;

public class InfoCard {

  private List<Player> players;
  private MessageEmbed card;
  private Message title;
  private Message message;
  private DateTime creationTime;

  public InfoCard(List<Player> players, MessageEmbed card) {
    this.players = players;
    this.card = card;
    this.creationTime = DateTime.now();
  }

  public List<Player> getPlayers() {
    return players;
  }

  public void setPlayers(List<Player> players) {
    this.players = players;
  }

  public MessageEmbed getCard() {
    return card;
  }

  public void setCard(MessageEmbed card) {
    this.card = card;
  }

  public Message getMessage() {
    return message;
  }

  public void setMessage(Message message) {
    this.message = message;
  }

  public DateTime getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(DateTime creationTime) {
    this.creationTime = creationTime;
  }

  public Message getTitle() {
    return title;
  }

  public void setTitle(Message title) {
    this.title = title;
  }
}
