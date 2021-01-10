package ch.kalunight.zoe.model.dangerosityreport;

import ch.kalunight.zoe.translation.LanguageManager;

public class DangerosityReportHighWinrate extends DangerosityReport {

  private double winrate;
  
  public DangerosityReportHighWinrate(double winrate) {
    super(DangerosityReportType.HIGH_WINRATE);
    this.winrate = winrate;
  }

  @Override
  protected String getInfoToShow(String lang) {
    return String.format(LanguageManager.getText(lang, "dangerosityReportHighWinrateInfo"), pourcentageFormat.format(winrate));
  }

  public double getWinrate() {
    return winrate;
  }

}
