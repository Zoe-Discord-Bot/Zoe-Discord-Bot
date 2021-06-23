package ch.kalunight.zoe.command.create;

import java.sql.SQLException;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.TeamRepository;
import ch.kalunight.zoe.translation.LanguageManager;

public class CreateTeamCommandRunnable {

  public static final String USAGE_NAME = "team";

  private CreateTeamCommandRunnable() {
    // hide default public constructor
  }
  
  public static String executeCommand(Server server, String args) throws SQLException {
    
    String nameTeam = args;
    
    if(!checkNameValid(nameTeam)) {
      return LanguageManager.getText(server.getLanguage(), "nameUseIllegalCharacter");
    }
    
    if(nameTeam.equals("--server")) {
      return LanguageManager.getText(server.getLanguage(), "nameAlreadyUsedByTheSystem");
    }

    if(nameTeam.equals("")) {
      return LanguageManager.getText(server.getLanguage(), "createTeamNeedName");
    } else {
      DTO.Team team = TeamRepository.getTeam(server.serv_guildId, nameTeam);

      if(team != null) {
        return LanguageManager.getText(server.getLanguage(), "createTeamNameAlreadyExist");
      } else {
        TeamRepository.createTeam(server.serv_id, nameTeam);
        return String.format(LanguageManager.getText(server.getLanguage(), "createTeamDoneMessage"), nameTeam);
      }
    }
  }

  private static boolean checkNameValid(String nameToCheck) {
    
    boolean nameInvalid = false;
    
    nameInvalid = nameToCheck.contains("*");
    if(nameInvalid) {
      return false;
    }
    
    nameInvalid = nameToCheck.contains("_");
    if(nameInvalid) {
      return false;
    }
    
    nameInvalid = nameToCheck.contains(">");
    if(nameInvalid) {
      return false;
    }
    
    nameInvalid = nameToCheck.contains("`");
    if(nameInvalid) {
      return false;
    }else {
      return true;
    }
  }
}
