package ch.kalunight.zoe.model.clash;

public class ChampionDangerosityReport {

  private int championId;
  private int dangerosityScore;
  private DangerosityReport report;
  
  public ChampionDangerosityReport(int championId, int dangerosityScore, DangerosityReport report) {
    this.championId = championId;
    this.dangerosityScore = dangerosityScore;
    this.report = report;
  }

  public int getChampionId() {
    return championId;
  }

  public int getDangerosityScore() {
    return dangerosityScore;
  }

  public DangerosityReport getReport() {
    return report;
  }
  
}
