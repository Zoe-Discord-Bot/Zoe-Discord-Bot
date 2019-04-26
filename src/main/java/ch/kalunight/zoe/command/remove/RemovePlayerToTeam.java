package ch.kalunight.zoe.command.remove;

import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.command.CommandUtil;
import ch.kalunight.zoe.model.Player;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.SpellingLangage;
import ch.kalunight.zoe.model.Team;
import net.dv8tion.jda.core.Permission;

public class RemovePlayerToTeam extends Command {

  private static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\(([^)]+)\\)");

  public RemovePlayerToTeam() {
    this.name = "playerToTeam";
    this.help = "Delete the given player from the given team. Manage Channel permission needed.";
    this.arguments = "@MentionOfPlayer (teamName)";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.helpBiConsumer = getHelpMethod();
  }

  @Override
  protected void execute(CommandEvent event) {
    event.getTextChannel().sendTyping().complete();
    Server server = ServerData.getServers().get(event.getGuild().getId());

    if(server == null) {
      server = new Server(event.getGuild(), SpellingLangage.EN);
      ServerData.getServers().put(event.getGuild().getId(), server);
    }

    if(event.getMessage().getMentionedMembers().size() != 1) {
      event.reply("Please mentions one people !");
      return;
    }

    Player player = server.getPlayerByDiscordId(event.getMessage().getMentionedMembers().get(0).getUser().getId());

    if(player == null) {
      event.reply("The mentioned people is not a registed player !");
      return;
    }

    Matcher matcher = PARENTHESES_PATTERN.matcher(event.getArgs());
    String teamName = "";
    while(matcher.find()) {
      teamName = matcher.group(1);
    }

    Team teamWhereRemove = server.getTeamByName(teamName);
    if(teamWhereRemove == null) {
      event.reply("The given team does not exist ! "
          + "(Hint: The team name need to be in parantheses like this : `>remmove playerToTeam @PlayerMentioned (TeamName)`)");
      return;
    }

    if(!teamWhereRemove.isPlayerInTheTeam(player)) {
      event.reply("The player is not in the given Team !");
      return;
    }

    teamWhereRemove.getPlayers().remove(player);
    event.reply("The player has been deleted from the team !");
  }

  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Remove playerToTeam command :\n");
        stringBuilder.append("--> `>remove " + name + " " + arguments + "` : " + help);

        event.reply(stringBuilder.toString());
      }
    };
  }

}
