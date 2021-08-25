package ch.kalunight.zoe.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.kalunight.zoe.model.dto.SavedMatch;
import ch.kalunight.zoe.model.dto.SavedMatchPlayer;
import ch.kalunight.zoe.model.dto.SavedSummoner;

public class MatchReceiver {
  
  public final List<SavedMatch> matchs = Collections.synchronizedList(new ArrayList<>());
  
  private SavedSummoner summoner;
  
  private List<Integer> championsWanted;
  
  private List<Integer> queueWanted;
  
  private Timestamp timestampToHit;
  
  private boolean timestampHit;
  
  public MatchReceiver(SavedSummoner summoner, List<Integer> championsWanted, List<Integer> queueWanted,
      Timestamp timestampToHit) {
    this.summoner = summoner;
    this.championsWanted = championsWanted;
    this.queueWanted = queueWanted;
    this.timestampToHit = timestampToHit;
    this.timestampHit = false;
  }
  
  public boolean isGivenMatchWanted(SavedMatch match) {
    boolean wanted = true;
    
    if(championsWanted != null && !championsWanted.isEmpty() && summoner != null) {
      wanted = false;
      for(SavedMatchPlayer participantToCheck : match.getPlayers()) {
        if(participantToCheck.getSummonerId().equals(summoner.getSummonerId()) &&
            championsWanted.contains(participantToCheck.getChampionId())) {
          wanted = true;
          break;
        }
      }
    }
      
      if(queueWanted != null && !queueWanted.isEmpty()) {
        wanted = false;
        for(Integer possibleKey : match.getQueueId().getValues()) {
          if(queueWanted.contains(possibleKey)) {
            wanted = true;
            break;
          }
        }
      }
      
      if(timestampToHit != null) {
        wanted = false;
        final Timestamp matchTimeStamp = new Timestamp(match.getGameCreation());
        if(matchTimeStamp.after(timestampToHit)) {
          wanted = true;
        }else {
          timestampHit = true;
        }
      }
      
      return wanted;
  }
  
  public boolean isTimestampHited() {
    return timestampHit;
  }
  
}
