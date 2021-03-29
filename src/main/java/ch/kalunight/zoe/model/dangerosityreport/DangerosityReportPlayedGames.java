package ch.kalunight.zoe.model.dangerosityreport;

import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.JDA;

public class DangerosityReportPlayedGames extends DangerosityReport {

  private static final int MAX_VALUE_AND_GAMES = 20;
  
  private int nbrGames;
  
  public DangerosityReportPlayedGames(int nbrGames) {
    super(DangerosityReportType.GAMES_PLAYED, DangerosityReportSource.CHAMPION);
    this.nbrGames = nbrGames;
  }

  @Override
  protected String getInfoToShow(String lang, JDA jda) {
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
