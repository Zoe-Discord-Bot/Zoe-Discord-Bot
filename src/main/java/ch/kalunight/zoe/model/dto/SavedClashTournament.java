package ch.kalunight.zoe.model.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ch.kalunight.zoe.util.MongodbDateUtil;
import no.stelar7.api.r4j.pojo.lol.clash.ClashTournament;
import no.stelar7.api.r4j.pojo.lol.clash.ClashTournamentPhase;

public class SavedClashTournament {

  public static final String SERVER_FIELD_NAME = "server";
  public static final String TOURNAMENT_ID_FIELD_NAME = "tournamentId";
  
  private ZoePlatform server;
  private int tournamentId;
  private int themeId;
  private String nameKey;
  private String nameKeySecondary;
  private List<SavedClashTournamentPhase> schedule;
  
  private Date retrieveDate;
  
  public SavedClashTournament() {}
  
  public SavedClashTournament(ClashTournament tournament, ZoePlatform server) {
    this.server = server;
    tournamentId = tournament.getId();
    themeId = tournament.getThemeId();
    nameKey = tournament.getNameKey(); 
    nameKeySecondary = tournament.getNameKeySecondary();
    
    schedule = new ArrayList<>();
    
    for(ClashTournamentPhase scheduleToAdd : tournament.getSchedule()) {
      schedule.add(new SavedClashTournamentPhase(scheduleToAdd));
    }
    
    this.retrieveDate = MongodbDateUtil.toDate(LocalDateTime.now());
  }

  public int getTournamentId() {
    return tournamentId;
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

  public ZoePlatform getServer() {
    return server;
  }

  public void setServer(ZoePlatform server) {
    this.server = server;
  }

  public void setTournamentId(int id) {
    this.tournamentId = id;
  }

  public void setThemeId(int themeId) {
    this.themeId = themeId;
  }

  public void setNameKey(String nameKey) {
    this.nameKey = nameKey;
  }

  public void setNameKeySecondary(String nameKeySecondary) {
    this.nameKeySecondary = nameKeySecondary;
  }

  public void setSchedule(List<SavedClashTournamentPhase> schedule) {
    this.schedule = schedule;
  }

  public Date getRetrieveDate() {
    return retrieveDate;
  }

  public void setRetrieveDate(Date retrieveDate) {
    this.retrieveDate = retrieveDate;
  }
  
}
