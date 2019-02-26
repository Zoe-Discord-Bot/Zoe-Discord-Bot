package ch.kalunight.zoe.model;

import net.dv8tion.jda.core.entities.User;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;

public class Player {
  private User discordUser;
  private Summoner summoner;
  private Platform server;
  private boolean mentionnable;

  public Player(User discordUser, Summoner summoner, boolean mentionnable) {
    this.discordUser = discordUser;
    this.summoner = summoner;
    this.mentionnable = mentionnable;
  }

  public User getDiscordUser() {
    return discordUser;
  }

  public void setDiscordUser(User discordUser) {
    this.discordUser = discordUser;
  }

  public Summoner getSummoner() {
    return summoner;
  }

  public void setSummoner(Summoner summoner) {
    this.summoner = summoner;
  }

  public boolean isMentionnable() {
    return mentionnable;
  }

  public void setMentionnable(boolean mentionned) {
    this.mentionnable = mentionned;
  }

  public Platform getServer() {
    return server;
  }

  public void setServer(Platform server) {
    this.server = server;
  }

}
