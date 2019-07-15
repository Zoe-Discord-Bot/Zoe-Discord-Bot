package ch.kalunight.zoe.command.add;

import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.command.CommandUtil;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.player_data.Player;
import ch.kalunight.zoe.model.player_data.Team;
import ch.kalunight.zoe.model.static_data.SpellingLangage;
import net.dv8tion.jda.core.Permission;

public class AddPlayerToTeamCommand extends Command {

  public static final String USAGE_NAME = "playerToTeam";
  public static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\(([^)]+)\\)");
  
  public AddPlayerToTeamCommand() {
    this.name = USAGE_NAME;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.arguments = "@MentionPlayer (teamName)";
    this.userPermissions = permissionRequired;
    this.help = "Add the mentioned player to the given team. Manage Channel permission needed.";
    this.helpBiConsumer = getHelpMethod();
  }

  @Override
  protected void execute(CommandEvent event) {
    event.getTextChannel().sendTyping().complete();
    Server server = ServerData.getServers().get(event.getGuild().getId());

    if(server == null) {
      server = new Server(event.getGuild(), SpellingLangage.EN, new ServerConfiguration());
      ServerData.getServers().put(event.getGuild().getId(), server);
    }

    if(event.getMessage().getMentionedMembers().size() != 1) {
      event.reply("Please mention one player !");
    } else {
      Player player = server.getPlayerByDiscordId(event.getMessage().getMentionedMembers().get(0).getUser().getId());

      if(player == null) {
        event.reply("The mentioned poeple is not a player !");
      } else {

        Team team = server.getTeamByPlayer(player);
        if(team != null) {
          event.reply("The mentioned player have already another team (" + team.getName() + ") !");
        } else {
          Matcher matcher = PARENTHESES_PATTERN.matcher(event.getArgs());
          String teamName = "";
          while(matcher.find()) {
            teamName = matcher.group(1);
          }

          Team teamToAdd = server.getTeamByName(teamName);
          if(teamToAdd == null) {
            event.reply("The given team does not exist ! "
                + "(Hint: The team name need to be in parantheses like this : `>add playerToTeam @PlayerMentioned (TeamName)`)");
          } else {
            teamToAdd.getPlayers().add(player);
            event.reply("The player has been added !");
          }
        }
      }
    }
  }

  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Add playerToTeam command :\n");
        stringBuilder.append("--> `>add " + name + " " + arguments + "` : " + help);

        event.reply(stringBuilder.toString());
      }
    };
  }
}
