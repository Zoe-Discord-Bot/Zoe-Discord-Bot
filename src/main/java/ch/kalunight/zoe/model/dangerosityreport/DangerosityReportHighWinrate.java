package ch.kalunight.zoe.model.dangerosityreport;

import ch.kalunight.zoe.translation.LanguageManager;

public class DangerosityReportHighWinrate extends DangerosityReport {

  private static final int HIGH_WINRATE_LOW_VALUE = 10;

  private static final double HIGH_WINRATE_LOW_WINRATE_NEEDED = 55d;

  private static final int HIGH_WINRATE_MEDIUM_VALUE = 20;

  private static final double HIGH_WINRATE_MEDIUM_WINRATE_NEEDED = 60d;

  private static final int HIGH_WINRATE_HIGH_VALUE = 30;

  private static final double HIGH_WINRATE_HIGH_WINRATE_NEEDED = 70d;

  private static final int BAD_WINRATE_LOW_VALUE = -10;

  private static final double BAD_WINRATE_LOW_WINRATE_NEEDED = 45d;

  private static final int BAD_WINRATE_MEDIUM_VALUE = -20;

  private static final double BAD_WINRATE_MEDIUM_WINRATE_NEEDED = 40d;

  private static final int BAD_WINRATE_HIGH_VALUE = -30;

  private static final double BAD_WINRATE_HIGH_WINRATE_NEEDED = 30d;

  private static final int HIGH_WINRATE_MINIMAL_NUMBER_OF_GAME_NEEDED = 15;

  private double winrate;

  private int nbrGames;

  public DangerosityReportHighWinrate(double winrate, int numberOfGames) {
    super(DangerosityReportType.WINRATE, DangerosityReportSource.CHAMPION);
    this.winrate = winrate;
    this.nbrGames = numberOfGames;
  }

  @Override
  protected String getInfoToShow(String lang) {
    if(getReportValue() > BASE_SCORE) {
      return String.format(LanguageManager.getText(lang, "dangerosityReportHighWinrateInfo"), POURCENTAGE_FORMAT.format(winrate) + "%"); 
    }else {
      return String.format(LanguageManager.getText(lang, "dangerosityReportBadWinrateInfo"), POURCENTAGE_FORMAT.format(winrate) + "%"); 
    }
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

      if(winrate > BAD_WINRATE_LOW_WINRATE_NEEDED) {
        return BASE_SCORE;
      }

      if(BAD_WINRATE_LOW_WINRATE_NEEDED <= winrate) {
        return BAD_WINRATE_LOW_VALUE;
      }

      if(BAD_WINRATE_MEDIUM_WINRATE_NEEDED <= winrate) {
        return BAD_WINRATE_MEDIUM_VALUE;
      }

      if(BAD_WINRATE_HIGH_WINRATE_NEEDED <= winrate) {
        return BAD_WINRATE_HIGH_VALUE;
      }
    }

    return BASE_SCORE;
  }
}
