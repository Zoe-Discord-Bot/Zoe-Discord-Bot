package ch.kalunight.zoe.model.dangerosityreport;

import ch.kalunight.zoe.translation.LanguageManager;

public class DangerosityReportOTP extends DangerosityReport {

  private static final int MINIMAL_NUMBER_OF_MATCH = 20;

  private static final int OTP_VALUE = 15;

  private static final int OTP_MINIMUM_RATIO = 70;

  private double playRatio;

  private int numberOfGamesForTheChampion;

  public DangerosityReportOTP(double playRatio, int numberOfGamesForTheChampion) {
    super(DangerosityReportType.OTP, DangerosityReportSource.CHAMPION);
    this.playRatio = playRatio;
    this.numberOfGamesForTheChampion = numberOfGamesForTheChampion;
  }

  @Override
  public String getInfoToShow(String lang) {
    return String.format(LanguageManager.getText(lang, "dangerosityReportOTPInfo"), POURCENTAGE_FORMAT.format(playRatio) + "%");
  }

  public double getPlayRatio() {
    return playRatio;
  }

  @Override
  public int getReportValue() {
    if(MINIMAL_NUMBER_OF_MATCH <= numberOfGamesForTheChampion && OTP_MINIMUM_RATIO <= playRatio) {
      return OTP_VALUE;
    }

    return BASE_SCORE;
  }

}
