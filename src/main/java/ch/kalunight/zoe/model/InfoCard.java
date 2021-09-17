package ch.kalunight.zoe.model;

import java.util.List;
import org.joda.time.DateTime;
import ch.kalunight.zoe.model.dto.DTO;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import no.stelar7.api.r4j.pojo.lol.spectator.SpectatorGameInfo;

public class InfoCard {

  private List<DTO.Player> players;
  private MessageEmbed card;
  private Message title;
  private Message message;
  private DateTime creationTime;
  private SpectatorGameInfo currentGameInfo;

  public InfoCard(List<DTO.Player> players, MessageEmbed card, SpectatorGameInfo currentGameInfo) {
    this.players = players;
    this.card = card;
    this.currentGameInfo = currentGameInfo;
    this.creationTime = DateTime.now();
  }

  public List<DTO.Player> getPlayers() {
    return players;
  }

  public void setPlayers(List<DTO.Player> players) {
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

  public SpectatorGameInfo getCurrentGameInfo() {
    return currentGameInfo;
  }

  public void setCurrentGameInfo(SpectatorGameInfo currentGameInfo) {
    this.currentGameInfo = currentGameInfo;
  }
}
