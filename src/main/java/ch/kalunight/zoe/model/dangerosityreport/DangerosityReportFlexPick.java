package ch.kalunight.zoe.model.dangerosityreport;

import java.util.List;

import ch.kalunight.zoe.service.analysis.ChampionRole;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.TeamUtil;

public class DangerosityReportFlexPick extends DangerosityReport {

  private static final int FLEX_PICK_LOW_VALUE = 10;

  private static final int FLEX_PICK_LOW_PLAYER_NEEDED = 2;
  
  private static final int FLEX_PICK_MEDIUM_VALUE = 20;

  private static final int FLEX_PICK_MEDIUM_PLAYER_NEEDED = 3;
  
  private static final int FLEX_PICK_HIGH_VALUE = 30;

  private static final int FLEX_PICK_HIGH_PLAYER_NEEDED = 4;
  
  private List<ChampionRole> rolesWherePlayed;
  
  public DangerosityReportFlexPick(List<ChampionRole> flexRoles) {
    super(DangerosityReportType.FLEX_PICK);
    this.rolesWherePlayed = flexRoles;
  }

  @Override
  public String getInfoToShow(String lang) {
    StringBuilder builder = new StringBuilder();
    
    int treated = 0;
    for(ChampionRole role : rolesWherePlayed) {
      treated++;
      builder.append(LanguageManager.getText(lang, TeamUtil.getChampionRoleAbrID(role)));
      
      if(treated != rolesWherePlayed.size()) {
        builder.append(" / ");
      }
    }
    
    return String.format(LanguageManager.getText(lang, "dangerosityReportFlexPickInfo"), builder.toString());
  }
  
  @Override
  public int getReportValue() {
    
    if(rolesWherePlayed.size() >= FLEX_PICK_HIGH_PLAYER_NEEDED) {
      return FLEX_PICK_HIGH_VALUE;
    }
    
    if(rolesWherePlayed.size() >= FLEX_PICK_MEDIUM_PLAYER_NEEDED) {
      return FLEX_PICK_MEDIUM_VALUE;
    }
    
    if(rolesWherePlayed.size() >= FLEX_PICK_LOW_PLAYER_NEEDED) {
      return FLEX_PICK_LOW_VALUE;
    }
    
    return BASE_SCORE;
  }
  
  public List<ChampionRole> getRolesWherePlayed() {
    return rolesWherePlayed;
  }
  
}
