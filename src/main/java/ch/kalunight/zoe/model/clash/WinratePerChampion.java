package ch.kalunight.zoe.model.clash;

import java.util.List;

import ch.kalunight.zoe.model.dto.SavedMatch;
import ch.kalunight.zoe.model.dto.SavedMatchPlayer;

public class WinratePerChampion {

  private int championId;

  private List<SavedMatch> matchs;

  private Integer nbrWin;

  private Integer nbrLose;

  private Double winrate;

  public WinratePerChampion(int championId, List<SavedMatch> matchs) {
    this.championId = championId;
    this.matchs = matchs;
  }

  public double getWinrate() {
    if(winrate == null) {
      nbrWin = 0;
      nbrLose = 0;

      for(SavedMatch match : matchs) {
        for(SavedMatchPlayer player : match.getPlayers()) {
          if(player.getChampionId() == championId) {
            if(match.isGivenAccountWinner(player.getAccountId())){
              nbrWin++;
            }else {
              nbrLose++;
            }
          }
        }
      }

      if(getNumberOfGame() == 0) {
        winrate = nbrWin.doubleValue() / (nbrWin + nbrLose);
      }else {
        winrate = 0d;
      }
    }
    
    return winrate;
  }
  
  public int getNumberOfWin() {
    if(nbrWin == null) {
      getWinrate();
    }
    
    return nbrWin;
  }
  
  public int getNumberOfLose() {
    if(nbrLose == null) {
      getWinrate();
    }
    
    return nbrLose;
  }

  public int getNumberOfGame() {
    if(nbrWin == null || nbrLose == null) {
      return matchs.size();
    }else {
      return nbrLose + nbrWin;
    }
  }

  public int getChampionId() {
    return championId;
  }

  public List<SavedMatch> getMatchs() {
    return matchs;
  }
  
}
