package ch.kalunight.zoe.command.remove;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.command.CommandUtil;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.player_data.Player;
import ch.kalunight.zoe.model.player_data.Team;
import ch.kalunight.zoe.model.static_data.SpellingLangage;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;

public class RemovePlayerToTeamCommand extends Command {

  public static final String USAGE_NAME = "playerToTeam";

  public RemovePlayerToTeamCommand() {
    this.name = USAGE_NAME;
    this.help = "removePlayerToTeamHelpMessage";
    this.arguments = "@MentionOfPlayer";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(RemoveCommand.USAGE_NAME, name, arguments, help);
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
      event.reply(LanguageManager.getText(server.getLangage(), "removePlayerToTeamMissingMention"));
      return;
    }

    Player player = server.getPlayerByDiscordId(event.getMessage().getMentionedMembers().get(0).getUser().getId());

    if(player == null) {
      event.reply(LanguageManager.getText(server.getLangage(), "removePlayerToTeamMentionnedPlayerNotPlayer"));
      return;
    }

    Team teamWhereRemove = server.getTeamByPlayer(player);
    
    if(teamWhereRemove == null) {
      event.reply(LanguageManager.getText(server.getLangage(), "removePlayerToTeamNotInTheTeam"));
      return;
    }

    teamWhereRemove.getPlayers().remove(player);
    event.reply(String.format(LanguageManager.getText(server.getLangage(), "removePlayerToTeamDoneMessage"),
        player.getDiscordUser().getName(), teamWhereRemove.getName()));
  }
}
