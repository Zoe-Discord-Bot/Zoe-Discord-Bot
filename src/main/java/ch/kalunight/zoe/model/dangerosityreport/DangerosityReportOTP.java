package ch.kalunight.zoe.model.dangerosityreport;

import ch.kalunight.zoe.translation.LanguageManager;

public class DangerosityReportOTP extends DangerosityReport {

  private double playRatio;
  
  public DangerosityReportOTP(double playRatio) {
    super(DangerosityReportType.OTP);
    this.playRatio = playRatio;
  }

  @Override
  protected String getInfoToShow(String lang) {
    return String.format(LanguageManager.getText(lang, "dangerosityReportOTP"), pourcentageFormat.format(playRatio));
  }
  
  public double getPlayRatio() {
    return playRatio;
  }

}
