package ch.kalunight.zoe.model;

import ch.kalunight.zoe.service.analysis.ChampionRole;

public class TeamPositionRated implements Comparable<TeamPositionRated> {

  private ChampionRole championRole;
  
  private double playRatio;

  public TeamPositionRated(ChampionRole role, double numberOfTimePlayed) {
    this.championRole = role;
    this.playRatio = numberOfTimePlayed;
  }
  
  @Override
  public int compareTo(TeamPositionRated objectToTest) {
    if(objectToTest.getRatioOfPlay() > playRatio) {
      return 1;
    }else if(objectToTest.getRatioOfPlay() < playRatio) {
      return -1;
    }
    
    return 0;
  }

  public ChampionRole getChampionRole() {
    return championRole;
  }

  public double getRatioOfPlay() {
    return playRatio;
  }
  
}
