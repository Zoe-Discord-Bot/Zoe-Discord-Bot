package ch.kalunight.zoe.model.dangerosityreport;

import ch.kalunight.zoe.translation.LanguageManager;

public class DangerosityReportOTP extends DangerosityReport {

  private static final int OTP_VALUE = 15;

  private static final int OTP_MINIMUM_RATIO = 70;
  
  private double playRatio;
  
  public DangerosityReportOTP(double playRatio) {
    super(DangerosityReportType.OTP);
    this.playRatio = playRatio;
  }

  @Override
  public String getInfoToShow(String lang) {
    return String.format(LanguageManager.getText(lang, "dangerosityReportOTPInfo"), POURCENTAGE_FORMAT.format(playRatio));
  }
  
  public double getPlayRatio() {
    return playRatio;
  }

  @Override
  public int getReportValue() {
    if(OTP_MINIMUM_RATIO >= playRatio) {
      return OTP_VALUE;
    }
    
    return BASE_SCORE;
  }

}
