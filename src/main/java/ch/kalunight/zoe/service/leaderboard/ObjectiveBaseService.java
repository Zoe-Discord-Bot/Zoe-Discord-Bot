package ch.kalunight.zoe.service.leaderboard;

public class ObjectiveBaseService implements Runnable {

  private long guildId;

  private long channelId;

  private long leaderboardId;

  public ObjectiveBaseService(long guildId, long channelId, long leaderboardId) {
    this.guildId = guildId;
    this.channelId = channelId;
    this.leaderboardId = leaderboardId;
  }

  @Override
  public void run() {
    

  }

}
