package ch.kalunight.zoe.command.create;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.create.definition.CreateCommandClassicDefinition;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.repositories.TeamRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;

public class CreateTeamCommand extends ZoeCommand {

  public static final String USAGE_NAME = "team";

  public CreateTeamCommand() {
    this.name = USAGE_NAME;
    this.arguments = "nameOfTheTeam";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = "createTeamHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(CreateCommandClassicDefinition.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    
    event.getTextChannel().sendTyping().complete();
    
    DTO.Server server = getServer(event.getGuild().getIdLong());
    
    String nameTeam = event.getArgs();
    
    if(!checkNameValid(nameTeam)) {
      event.reply(LanguageManager.getText(server.getLanguage(), "nameUseIllegalCharacter"));
      return;
    }
    
    if(nameTeam.equals("--server")) {
      event.reply(LanguageManager.getText(server.getLanguage(), "nameAlreadyUsedByTheSystem"));
      return;
    }

    if(nameTeam.equals("")) {
      event.reply(LanguageManager.getText(server.getLanguage(), "createTeamNeedName"));
    } else {
      DTO.Team team = TeamRepository.getTeam(server.serv_guildId, nameTeam);

      if(team != null) {
        event.reply(LanguageManager.getText(server.getLanguage(), "createTeamNameAlreadyExist"));
      } else {
        TeamRepository.createTeam(server.serv_id, nameTeam);
        event.reply(String.format(LanguageManager.getText(server.getLanguage(), "createTeamDoneMessage"), event.getArgs()));
      }
    }
  }

  private boolean checkNameValid(String nameToCheck) {
    
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

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
