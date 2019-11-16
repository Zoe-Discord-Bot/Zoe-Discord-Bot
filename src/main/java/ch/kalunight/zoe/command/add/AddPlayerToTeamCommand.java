package ch.kalunight.zoe.command.add;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.player_data.Player;
import ch.kalunight.zoe.model.player_data.Team;
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
  protected void executeCommand(CommandEvent event) {
    event.getTextChannel().sendTyping().complete();
    Server server = ServerData.getServers().get(event.getGuild().getId());
    
    if(event.getMessage().getMentionedMembers().size() != 1) {
      event.reply(LanguageManager.getText(server.getLangage(), "mentionOfPlayerNeeded"));
    } else {
      Player player = server.getPlayerByDiscordId(event.getMessage().getMentionedMembers().get(0).getUser().getIdLong());

      if(player == null) {
        event.reply(LanguageManager.getText(server.getLangage(), "mentionOfUserNeedToBeAPlayer"));
      } else {

        Team team = server.getTeamByPlayer(player);
        if(team != null) {
          event.reply(String.format(LanguageManager.getText(server.getLangage(), "mentionnedPlayerIsAlreadyInATeam"), team.getName()));
        } else {
          Matcher matcher = PARENTHESES_PATTERN.matcher(event.getArgs());
          String teamName = "";
          while(matcher.find()) {
            teamName = matcher.group(1);
          }

          Team teamToAdd = server.getTeamByName(teamName);
          if(teamToAdd == null) {
            event.reply(LanguageManager.getText(server.getLangage(), "givenTeamNotExist"));
          } else {
            teamToAdd.getPlayers().add(player);
            event.reply(LanguageManager.getText(server.getLangage(), "playerAddedInTheTeam"));
          }
        }
      }
    }
  }
}
