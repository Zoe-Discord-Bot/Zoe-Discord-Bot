package ch.kalunight.zoe.command.add;

import java.sql.SQLException;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.repositories.TeamRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;

public class AddPlayerToTeamCommand extends ZoeCommand {

  public static final String USAGE_NAME = "playerToTeam";
  public static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\(([^)]+)\\)");
  
  public AddPlayerToTeamCommand() {
    this.name = USAGE_NAME;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.arguments = "@MentionPlayer (teamName)";
    this.userPermissions = permissionRequired;
    this.help = "addPlayerToTeamCommandHelp";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(AddCommand.USAGE_NAME, USAGE_NAME, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    event.getTextChannel().sendTyping().complete();
    DTO.Server server = getServer(event.getGuild().getIdLong());
    
    if(event.getMessage().getMentionedMembers().size() != 1) {
      event.reply(LanguageManager.getText(server.getLanguage(), "mentionOfPlayerNeeded"));
    } else {
      
      DTO.Player player = PlayerRepository.getPlayer(server.serv_guildId,
          event.getMessage().getMentionedMembers().get(0).getUser().getIdLong());
      
      if(player == null) {
        event.reply(LanguageManager.getText(server.getLanguage(), "mentionOfUserNeedToBeAPlayer"));
      } else {

        DTO.Team team = TeamRepository.getTeamByPlayerAndGuild(server.serv_guildId, player.player_discordId);
        if(team != null) {
          event.reply(String.format(LanguageManager.getText(server.getLanguage(), "mentionnedPlayerIsAlreadyInATeam"), team.team_name));
        } else {
          Matcher matcher = PARENTHESES_PATTERN.matcher(event.getArgs());
          String teamName = "";
          while(matcher.find()) {
            teamName = matcher.group(1);
          }

          DTO.Team teamToAdd = TeamRepository.getTeam(server.serv_guildId, teamName);
          if(teamToAdd == null) {
            event.reply(LanguageManager.getText(server.getLanguage(), "givenTeamNotExist"));
          } else {
            PlayerRepository.updateTeamOfPlayer(player.player_id, teamToAdd.team_id);
            event.reply(LanguageManager.getText(server.getLanguage(), "playerAddedInTheTeam"));
          }
        }
      }
    }
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
