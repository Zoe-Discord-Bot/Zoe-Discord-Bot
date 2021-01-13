package ch.kalunight.zoe.model.dangerosityreport;

import ch.kalunight.zoe.translation.LanguageManager;

public class DangerosityReportHighWinrate extends DangerosityReport {

  private static final int HIGH_WINRATE_LOW_VALUE = 10;

  private static final double HIGH_WINRATE_LOW_WINRATE_NEEDED = 55d;
  
  private static final int HIGH_WINRATE_MEDIUM_VALUE = 20;

  private static final double HIGH_WINRATE_MEDIUM_WINRATE_NEEDED = 60d;
  
  private static final int HIGH_WINRATE_HIGH_VALUE = 30;

  private static final double HIGH_WINRATE_HIGH_WINRATE_NEEDED = 70d;
  
  private static final int HIGH_WINRATE_MINIMAL_NUMBER_OF_GAME_NEEDED = 20;
  
  private double winrate;
  
  private int nbrGames;
  
  public DangerosityReportHighWinrate(double winrate, int nbrGames) {
    super(DangerosityReportType.HIGH_WINRATE);
    this.winrate = winrate;
    this.nbrGames = nbrGames;
  }

  @Override
  public String getInfoToShow(String lang) {
    return String.format(LanguageManager.getText(lang, "dangerosityReportHighWinrateInfo"), POURCENTAGE_FORMAT.format(winrate), nbrGames);
  }

  public double getWinrate() {
    return winrate;
  }
  
  public int getNbrGames() {
    return nbrGames;
  }

  @Override
  public int getReportValue() {
    
    if(nbrGames >= HIGH_WINRATE_MINIMAL_NUMBER_OF_GAME_NEEDED) {
      if(HIGH_WINRATE_HIGH_WINRATE_NEEDED <= winrate) {
        return HIGH_WINRATE_HIGH_VALUE;
      }
      
      if(HIGH_WINRATE_MEDIUM_WINRATE_NEEDED <= winrate) {
        return HIGH_WINRATE_MEDIUM_VALUE;
      }
      
      if(HIGH_WINRATE_LOW_WINRATE_NEEDED <= winrate) {
        return HIGH_WINRATE_LOW_VALUE;
      }
    }
    
    return BASE_SCORE;
  }
}
