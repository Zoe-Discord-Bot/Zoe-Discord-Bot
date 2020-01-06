package ch.kalunight.zoe.model;

public class InfocardPlayerData {

  private static final String DEFAULT_ERROR_STRING = "error";
  
  private String summonerNameData = DEFAULT_ERROR_STRING;
  private String rankData = DEFAULT_ERROR_STRING;
  private String winRateData = DEFAULT_ERROR_STRING;
  private boolean isBlueTeam;
  
  public InfocardPlayerData(boolean isBlueTeam) {
    this.isBlueTeam = isBlueTeam;
  }

  public String getSummonerNameData() {
    return summonerNameData;
  }

  public void setSummonerNameData(String summonerNameData) {
    this.summonerNameData = summonerNameData;
  }

  public String getRankData() {
    return rankData;
  }

  public void setRankData(String rankData) {
    this.rankData = rankData;
  }

  public String getWinRateData() {
    return winRateData;
  }

  public void setWinRateData(String winRateData) {
    this.winRateData = winRateData;
  }

  public boolean isBlueTeam() {
    return isBlueTeam;
  }

  public void setBlueTeam(boolean isBlueTeam) {
    this.isBlueTeam = isBlueTeam;
  }
  
  
  
}
