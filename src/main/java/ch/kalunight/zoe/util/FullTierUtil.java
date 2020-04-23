package ch.kalunight.zoe.util;

import ch.kalunight.zoe.exception.NoValueRankException;
import ch.kalunight.zoe.model.player_data.FullTier;
import ch.kalunight.zoe.translation.LanguageManager;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;

public class FullTierUtil {

  private FullTierUtil() {
    // hide default public constructor
  }
  
  public static String getTierRankTextDifference(LeagueEntry oldEntry, LeagueEntry newEntry, String lang) {
    
    String usableGreenEmote = "";
    if(Ressources.getGreenTriangleEmote() != null) {
      usableGreenEmote = Ressources.getGreenTriangleEmote().getUsableEmote();
    }
    
    String usableRedEmote = "";
    if(Ressources.getRedTriangleEmote() != null) {
      usableRedEmote = Ressources.getRedTriangleEmote();
    }
    
    
    if(oldEntry == null || newEntry == null) {
      return "*?*";
    }
    
    FullTier oldFullTier = new FullTier(oldEntry);
    FullTier newFullTier = new FullTier(newEntry);

    if(oldFullTier.getLeaguePoints() == newFullTier.getLeaguePoints() || newFullTier.getLeaguePoints() == 100 
        || oldFullTier.getLeaguePoints() == 100) { //BO detected OR remake

      if(oldEntry.getMiniSeries() != null && newEntry.getMiniSeries() == null) { //BO ended
        
        if(oldFullTier.getTier().getValue() > newFullTier.getTier().getValue()) { //BO win
          return usableGreenEmote + " " + LanguageManager.getText(lang, "infoPanelRankedUpdateBoWin");
        } else { //BO lose
          return usableRedEmote + " " + LanguageManager.getText(lang, "infoPanelRankedUpdateBoLose");
        }
        
      }else if(oldEntry.getMiniSeries() == null && newEntry.getMiniSeries() != null) { //BO started
        
        return ":star: " + LanguageManager.getText(lang, "infoPanelRankedUpdateBoStarted");
        
      }else if(oldEntry.getMiniSeries() != null && newEntry.getMiniSeries() != null) { //BO in progress OR remake
        int nbrMatchOldEntry = oldEntry.getMiniSeries().getLosses() + oldEntry.getMiniSeries().getWins();
        int nbrMatchNewEntry = newEntry.getMiniSeries().getLosses() + newEntry.getMiniSeries().getWins();

        if(nbrMatchNewEntry != nbrMatchOldEntry) { //BO in progress
          return "" + LanguageManager.getText(lang, "infoPanelRankedUpdateBoInProgres");
        }
      }

    }else { //No BO
      if(oldFullTier.getRank().equals(newFullTier.getRank()) 
          && oldFullTier.getTier().equals(newFullTier.getTier())) { //Only LP change
        
        if(newFullTier.getLeaguePoints() < oldFullTier.getLeaguePoints()) {
          
          return usableRedEmote + " " + Integer.toString(newFullTier.getLeaguePoints() - oldFullTier.getLeaguePoints());
          
        }else if(newFullTier.getLeaguePoints() > oldFullTier.getLeaguePoints()) {
          
          return usableGreenEmote + " +" + (newFullTier.getLeaguePoints() - oldFullTier.getLeaguePoints());
          
        } else {
          return "*~0*";
        }

      }else { //Decay OR division skip
        try {
          if(oldFullTier.value() < newFullTier.value()) {
            return usableGreenEmote + " " + LanguageManager.getText(lang, "infoPanelRankedUpdateDivisionSkip");
          }else {
            return usableRedEmote + " " + LanguageManager.getText(lang, "infoPanelRankedUpdateDecay");
          }
        } catch (NoValueRankException e) {
          return "";
        }
      }
    }
    
    
    return "*?*";
  }
  
}
