package ch.kalunight.zoe.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.lol.match.v5.MatchParticipant;
import no.stelar7.api.r4j.pojo.lol.summoner.Summoner;

public class MatchReceiver {
  
  public final List<LOLMatch> matchs = Collections.synchronizedList(new ArrayList<>());
  
  private Summoner summoner;
  
  private List<Integer> championsWanted;
  
  private List<Integer> queueWanted;
  
  private Timestamp timestampToHit;
  
  private boolean timestampHit;
  
  public MatchReceiver(Summoner summoner, List<Integer> championsWanted, List<Integer> queueWanted,
      Timestamp timestampToHit) {
    this.summoner = summoner;
    this.championsWanted = championsWanted;
    this.queueWanted = queueWanted;
    this.timestampToHit = timestampToHit;
    this.timestampHit = false;
  }
  
  public boolean isGivenMatchWanted(LOLMatch match) {
    boolean wanted = true;
    
    if(championsWanted != null && !championsWanted.isEmpty() && summoner != null) {
      wanted = false;
      for(MatchParticipant participantToCheck : match.getParticipants()) {
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
