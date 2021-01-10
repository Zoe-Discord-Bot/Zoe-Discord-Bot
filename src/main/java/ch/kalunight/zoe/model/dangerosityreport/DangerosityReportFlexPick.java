package ch.kalunight.zoe.model.dangerosityreport;

import java.util.List;

import ch.kalunight.zoe.service.analysis.ChampionRole;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.TeamUtil;

public class DangerosityReportFlexPick extends DangerosityReport {

  private List<ChampionRole> rolesWherePlayed;
  
  public DangerosityReportFlexPick(List<ChampionRole> flexRoles) {
    super(DangerosityReportType.FLEX_PICK);
    this.rolesWherePlayed = flexRoles;
  }

  @Override
  protected String getInfoToShow(String lang) {
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
  
  public List<ChampionRole> getRolesWherePlayed() {
    return rolesWherePlayed;
  }
  
}
