package ch.kalunight.zoe.model.dto;

public class SavedMatchPlayer {

  private String accountId;
  private int championId;
  
  public SavedMatchPlayer(String accountId, int championId) {
    this.accountId = accountId;
    this.championId = championId;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public int getChampionId() {
    return championId;
  }

  public void setChampionId(int championId) {
    this.championId = championId;
  }
}