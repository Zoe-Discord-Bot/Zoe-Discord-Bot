package ch.kalunight.zoe.model.dangerosityreport;

import ch.kalunight.zoe.exception.NoValueRankException;
import ch.kalunight.zoe.model.player_data.FullTier;
import ch.kalunight.zoe.translation.LanguageManager;

public class DangerosityReportHighEloDiff extends DangerosityReport {

  private static final int HIGH_ELO_DIFF_LOW_VALUE = 10;

  private static final int HIGH_ELO_DIFF_LOW_RAW_RANK_VALUE_NEEDED = 400; //4 divisions / 1 rank
  
  private static final int HIGH_ELO_DIFF_MEDIUM_VALUE = 20;

  private static final int HIGH_ELO_DIFF_MEDIUM_RAW_RANK_VALUE_NEEDED = 600; //6 divisions
  
  private static final int HIGH_ELO_DIFF_HIGH_VALUE = 30;

  private static final int HIGH_ELO_DIFF_HIGH_RAW_RANK_VALUE_NEEDED = 800; //8 divisions / 2 ranks
  
  private static final int BAD_ELO_DIFF_LOW_VALUE = -10;

  private static final int BAD_ELO_DIFF_LOW_RAW_RANK_VALUE_NEEDED = -400; //4 divisions / 1 rank
  
  private static final int BAD_ELO_DIFF_MEDIUM_VALUE = -20;

  private static final int BAD_ELO_DIFF_MEDIUM_RAW_RANK_VALUE_NEEDED = -600; //6 divisions
  
  private static final int BAD_ELO_DIFF_HIGH_VALUE = -30;

  private static final int BAD_ELO_DIFF_HIGH_RAW_RANK_VALUE_NEEDED = -800; //8 divisions / 2 ranks
  
  private FullTier teamAverage;
  
  private FullTier player;
  
  public DangerosityReportHighEloDiff(FullTier teamAverage, FullTier player) {
    super(DangerosityReportType.ELO_DIFF, DangerosityReportSource.PLAYER);
    this.teamAverage = teamAverage;
    this.player = player;
  }

  @Override
  public String getInfoToShow(String lang) {
    if(getReportValue() > BASE_SCORE) {
    return String.format(LanguageManager.getText(lang, "dangerosityReportHighEloDiffInfo"), player.toStringWithoutLp(lang));
    }else {
      return String.format(LanguageManager.getText(lang, "dangerosityReportLowEloDiffInfo"), player.toStringWithoutLp(lang));
    }
  }
  
  @Override
  public int getReportValue() {
    
    int rankDiff;
    try {
      rankDiff = player.value() - teamAverage.value();
    } catch (NoValueRankException e) {
      return BASE_SCORE;
    }
    
    if(HIGH_ELO_DIFF_HIGH_RAW_RANK_VALUE_NEEDED <= rankDiff) {
      return HIGH_ELO_DIFF_HIGH_VALUE;
    }
    
    if(HIGH_ELO_DIFF_MEDIUM_RAW_RANK_VALUE_NEEDED <= rankDiff) {
      return HIGH_ELO_DIFF_MEDIUM_VALUE;
    }
    
    if(HIGH_ELO_DIFF_LOW_RAW_RANK_VALUE_NEEDED <= rankDiff) {
      return HIGH_ELO_DIFF_LOW_VALUE;
    }
    
    if(rankDiff > BAD_ELO_DIFF_LOW_RAW_RANK_VALUE_NEEDED) {
      return BASE_SCORE;
    }
    
    if(BAD_ELO_DIFF_LOW_RAW_RANK_VALUE_NEEDED <= rankDiff) {
      return BAD_ELO_DIFF_LOW_VALUE;
    }
    
    if(BAD_ELO_DIFF_MEDIUM_RAW_RANK_VALUE_NEEDED <= rankDiff) {
      return BAD_ELO_DIFF_MEDIUM_VALUE;
    }
    
    if(BAD_ELO_DIFF_HIGH_RAW_RANK_VALUE_NEEDED <= rankDiff) {
      return BAD_ELO_DIFF_HIGH_VALUE;
    }
    
    return BASE_SCORE;
  }

  public FullTier getTeamAverage() {
    return teamAverage;
  }

  public FullTier getPlayer() {
    return player;
  }
  
}
