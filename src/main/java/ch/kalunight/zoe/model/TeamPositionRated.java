package ch.kalunight.zoe.model;

import ch.kalunight.zoe.service.analysis.ChampionRole;

public class TeamPositionRated implements Comparable<TeamPositionRated> {

  private ChampionRole championRole;
  
  private int numberOfTimePlayed;

  public TeamPositionRated(ChampionRole role, int numberOfTimePlayed) {
    this.championRole = role;
    this.numberOfTimePlayed = numberOfTimePlayed;
  }
  
  @Override
  public int compareTo(TeamPositionRated objectToTest) {
    if(objectToTest.getNumberOfTimePlayed() > numberOfTimePlayed) {
      return 1;
    }else if(objectToTest.getNumberOfTimePlayed() < numberOfTimePlayed) {
      return -1;
    }
    
    return 0;
  }

  public ChampionRole getChampionRole() {
    return championRole;
  }

  public int getNumberOfTimePlayed() {
    return numberOfTimePlayed;
  }
  
}
