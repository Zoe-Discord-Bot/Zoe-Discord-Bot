package ch.kalunight.zoe.model.dangerosityreport;

import ch.kalunight.zoe.translation.LanguageManager;

public class DangerosityReportKDA extends DangerosityReport {

  public static final double DEFAULT_AVERAGE_KDA = 2.5;
  
  private static final int HIGH_KDA_LOW_VALUE = 10;

  private static final int HIGH_KDA_LOW_RATIO_NEEDED = 120;

  private static final int HIGH_KDA_MEDIUM_VALUE = 20;

  private static final int HIGH_KDA_MEDIUM_RATIO_NEEDED = 140;

  private static final int HIGH_KDA_HIGH_VALUE = 30;

  private static final int HIGH_KDA_HIGH_RATIO_NEEDED = 160;
  
  private static final int MINIMAL_NUMBER_OF_GAMES_NEEDED = 10;
  
  private double personalAverageKDAForThisChamp;
  
  private double averageKDAForThisChamp;
  
  private int numberOfGames;
  
  public DangerosityReportKDA(double personalAverageKDAForThisChamp, double averageKdaForTheChamp, int numberOfGames) {
    super(DangerosityReportType.KDA, DangerosityReportSource.CHAMPION);
    this.personalAverageKDAForThisChamp = personalAverageKDAForThisChamp;
    this.averageKDAForThisChamp = averageKdaForTheChamp;
    this.numberOfGames = numberOfGames;
  }

  @Override
  public String getInfoToShow(String lang) {
    return String.format(LanguageManager.getText(lang, "dangerosityReportHighKDAInfo"), SMALL_NUMBER_FORMAT.format(personalAverageKDAForThisChamp));
  }

  @Override
  public int getReportValue() {
    
    if(numberOfGames >= MINIMAL_NUMBER_OF_GAMES_NEEDED) {
      if(personalAverageKDAForThisChamp > getPourcentageOfAverageKDAForThisChamp(HIGH_KDA_HIGH_RATIO_NEEDED)) {
        return HIGH_KDA_HIGH_VALUE;
      }
      
      if(personalAverageKDAForThisChamp > getPourcentageOfAverageKDAForThisChamp(HIGH_KDA_MEDIUM_RATIO_NEEDED)) {
        return HIGH_KDA_MEDIUM_VALUE;
      }
      
      if(personalAverageKDAForThisChamp > getPourcentageOfAverageKDAForThisChamp(HIGH_KDA_LOW_RATIO_NEEDED)) {
        return HIGH_KDA_LOW_VALUE;
      }
    }
    
    return BASE_SCORE;
  }
  
  public double getPourcentageOfAverageKDAForThisChamp(int pourcentage) {
    return averageKDAForThisChamp / 100 * pourcentage;
  }
  
}
