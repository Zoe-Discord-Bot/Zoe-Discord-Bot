package ch.kalunight.zoe.model.dangerosityreport;

import java.util.List;

import ch.kalunight.zoe.service.analysis.ChampionRole;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.TeamUtil;
import net.dv8tion.jda.api.JDA;

public class DangerosityReportFlexPick extends DangerosityReport implements Comparable<DangerosityReportFlexPick> {

  public static final int NUMBER_OF_GAME_NEEDED_TO_BE_CONSIDERED = 5;
  
  private static final int FLEX_PICK_LOW_VALUE = 10;

  private static final int FLEX_PICK_LOW_PLAYER_NEEDED = 2;
  
  private static final int FLEX_PICK_MEDIUM_VALUE = 20;

  private static final int FLEX_PICK_MEDIUM_PLAYER_NEEDED = 3;
  
  private static final int FLEX_PICK_HIGH_VALUE = 30;

  private static final int FLEX_PICK_HIGH_PLAYER_NEEDED = 4;
  
  private List<ChampionRole> rolesWherePlayed;
  
  private int cumuledGames;
  
  public DangerosityReportFlexPick(List<ChampionRole> flexRoles, int cumuledGames) {
    super(DangerosityReportType.FLEX_PICK, DangerosityReportSource.CHAMPION);
    this.rolesWherePlayed = flexRoles;
    this.cumuledGames = cumuledGames;
  }

  @Override
  protected String getInfoToShow(String lang, JDA jda) {
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
  
  @Override
  public int compareTo(DangerosityReportFlexPick objectToCheck) {
    
    if(objectToCheck.getCumuledGames() == getCumuledGames()) {
      return 0;
    }
    
    if(objectToCheck.getCumuledGames() > getCumuledGames()) {
      return 1;
    }else if(objectToCheck.getCumuledGames() < getCumuledGames()) {
      return -1;
    }
    
    return 0;
  }
  
  public int getCumuledGames() {
    return cumuledGames;
  }

  public List<ChampionRole> getRolesWherePlayed() {
    return rolesWherePlayed;
  }
  
}
