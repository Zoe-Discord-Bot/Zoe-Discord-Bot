package ch.kalunight.zoe.model.dto;

import java.util.ArrayList;
import java.util.List;

import no.stelar7.api.r4j.pojo.lol.clash.ClashTournament;
import no.stelar7.api.r4j.pojo.lol.clash.ClashTournamentPhase;

public class SavedClashTournament {

  private String serverName;
  private int id;
  private int themeId;
  private String nameKey;
  private String nameKeySecondary;
  private List<SavedClashTournamentPhase> schedule;
  
  public SavedClashTournament(ClashTournament tournament, ZoePlatform server) {
    id = tournament.getId();
    themeId = tournament.getThemeId();
    nameKey = tournament.getNameKey(); 
    nameKeySecondary = tournament.getNameKeySecondary();
    
    schedule = new ArrayList<>();
    
    for(ClashTournamentPhase scheduleToAdd : tournament.getSchedule()) {
      schedule.add(new SavedClashTournamentPhase(scheduleToAdd));
    }
    
    this.serverName = server.getDbName();
  }

  public String getServerName() {
    return serverName;
  }
  
  public void setServerName(ZoePlatform server) {
    this.serverName = server.getDbName();
  }

  public int getId() {
    return id;
  }

  public int getThemeId() {
    return themeId;
  }

  public String getNameKey() {
    return nameKey;
  }

  public String getNameKeySecondary() {
    return nameKeySecondary;
  }

  public List<SavedClashTournamentPhase> getSchedule() {
    return schedule;
  }
  
}
