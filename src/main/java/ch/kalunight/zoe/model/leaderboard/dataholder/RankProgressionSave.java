package ch.kalunight.zoe.model.leaderboard.dataholder;

import java.util.List;

public class RankProgressionSave {
  
  private List<PlayerProgression> playersProgression;
  
  public RankProgressionSave(List<PlayerProgression> playersProgression) {
    this.playersProgression = playersProgression;
  }

  public List<PlayerProgression> getPlayersProgression() {
    return playersProgression;
  }
  
}
