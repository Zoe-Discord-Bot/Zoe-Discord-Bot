package ch.kalunight.zoe.model;

import ch.kalunight.zoe.model.dto.ZoePlatform;

public class PlayerRankedResult {

  private long gameId;
  private ZoePlatform platform;
  private String catTitle;
  private String lpResult;
  private String gameStats;
  private Boolean win;

  public PlayerRankedResult(long gameId, ZoePlatform platform, String catTitle, String lpResult, String gameStats,
      Boolean win) {
    this.gameId = gameId;
    this.platform = platform;
    this.catTitle = catTitle;
    this.lpResult = lpResult;
    this.gameStats = gameStats;
    this.win = win;
  }

  public boolean getWin() {
    return win;
  }

  public void setWin(Boolean win) {
    this.win = win;
  }

  public long getGameId() {
    return gameId;
  }

  public void setGameId(long gameId) {
    this.gameId = gameId;
  }

  public ZoePlatform getPlatform() {
    return platform;
  }

  public void setPlatform(ZoePlatform platform) {
    this.platform = platform;
  }

  public String getCatTitle() {
    return catTitle;
  }

  public void setCatTitle(String catTitle) {
    this.catTitle = catTitle;
  }

  public String getLpResult() {
    return lpResult;
  }

  public void setLpResult(String lpResult) {
    this.lpResult = lpResult;
  }

  public String getGameStats() {
    return gameStats;
  }

  public void setGameStats(String gameStats) {
    this.gameStats = gameStats;
  }
  
}
