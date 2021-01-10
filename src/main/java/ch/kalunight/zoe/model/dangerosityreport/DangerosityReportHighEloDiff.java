package ch.kalunight.zoe.model.dangerosityreport;

import ch.kalunight.zoe.model.player_data.FullTier;
import ch.kalunight.zoe.translation.LanguageManager;

public class DangerosityReportHighEloDiff extends DangerosityReport {

  private FullTier teamAverage;
  
  private FullTier player;
  
  public DangerosityReportHighEloDiff(FullTier teamAverage, FullTier player) {
    super(DangerosityReportType.HIGH_ELO_DIFF);
    this.teamAverage = teamAverage;
    this.player = player;
  }

  @Override
  protected String getInfoToShow(String lang) {
    return String.format(LanguageManager.getText(lang, "dangerosityReportHighEloDiffInfo"), player.toStringWithoutLp(lang));
  }

  public FullTier getTeamAverage() {
    return teamAverage;
  }

  public FullTier getPlayer() {
    return player;
  }
  
}
