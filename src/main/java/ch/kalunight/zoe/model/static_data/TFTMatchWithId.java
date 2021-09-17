package ch.kalunight.zoe.model.static_data;

import no.stelar7.api.r4j.pojo.tft.TFTMatch;

public class TFTMatchWithId {

  private String matchId;
  private TFTMatch match;
  
  public TFTMatchWithId(String matchId, TFTMatch match) {
    this.matchId = matchId;
    this.match = match;
  }

  public String getMatchId() {
    return matchId;
  }

  public void setMatchId(String matchId) {
    this.matchId = matchId;
  }

  public TFTMatch getMatch() {
    return match;
  }

  public void setMatch(TFTMatch match) {
    this.match = match;
  }
  
}