package ch.kalunight.zoe.model.leaderboard;

public abstract class LeaderboardExtraDataHandler {

  private Objective objective;
  private long guildId;
  private long channelId;
  private long messageId;
  
  public LeaderboardExtraDataHandler(Objective objective, long guildId, long channelId, long messageId) {
    this.objective = objective;
    this.guildId = guildId;
    this.channelId = channelId;
    this.messageId = messageId;
  }
  
  public void handleEndCreation() {
    
  }
  
  protected abstract void handleExtraDataNeeded();
}
