package ch.kalunight.zoe.model.dangerosityreport;

import ch.kalunight.zoe.translation.LanguageManager;

public class DangerosityReportPlayedGames extends DangerosityReport {

  private static final int MAX_VALUE_AND_GAMES = 20;
  
  private int nbrGames;
  
  public DangerosityReportPlayedGames(int nbrGames) {
    super(DangerosityReportType.GAMES_PLAYED, DangerosityReportSource.CHAMPION);
    this.nbrGames = nbrGames;
  }

  @Override
  protected String getInfoToShow(String lang) {
    return String.format(LanguageManager.getText(lang, "dangerosityReportPlayRate"), nbrGames);
  }

  @Override
  public int getReportValue() {
    
    if(nbrGames >= MAX_VALUE_AND_GAMES) {
      return MAX_VALUE_AND_GAMES;
    }
    
    return nbrGames;
  }

}
