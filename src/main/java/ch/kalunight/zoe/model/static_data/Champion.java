package ch.kalunight.zoe.model.static_data;

import java.io.File;
import java.util.List;

import ch.kalunight.zoe.service.analysis.ChampionRole;
import net.dv8tion.jda.api.entities.Emote;

public class Champion {
  private int key;
  private String id;
  private String name;
  private File championLogo;
  private Emote emote;
  private List<ChampionRole> roles;
  private double averageKDA;

  public Champion(final int key, final String id, final String name, File championLogo) {
    this.id = id;
    this.key = key;
    this.name = name;
    this.championLogo = championLogo;
  }

  public String getDisplayName() {
    if(emote != null) {
      return emote.getAsMention();
    }
    return name;
  }
  
  public String getEmoteUsable() {
    if(emote == null) {
      return "";
    }
    return emote.getAsMention();
  }

  public int getKey() {
    return this.key;
  }

  public void setKey(final int key) {
    this.key = key;
  }

  public String getId() {
    return this.id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getName() {
    return this.name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public File getChampionLogo() {
    return championLogo;
  }

  public void setChampionLogo(File championLogo) {
    this.championLogo = championLogo;
  }

  public Emote getEmote() {
    return emote;
  }

  public void setEmote(Emote emote) {
    this.emote = emote;
  }

  public List<ChampionRole> getRoles() {
    return roles;
  }

  public void setRoles(List<ChampionRole> roles) {
    this.roles = roles;
  }

  public double getAverageKDA() {
    return averageKDA;
  }

  public void setAverageKDA(double averageKDA) {
    this.averageKDA = averageKDA;
  }
}
