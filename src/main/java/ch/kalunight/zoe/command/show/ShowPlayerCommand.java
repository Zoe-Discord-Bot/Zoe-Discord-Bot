package ch.kalunight.zoe.command.show;

import java.awt.Color;
import java.util.concurrent.TimeUnit;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.Paginator;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.command.CommandUtil;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.player_data.LeagueAccount;
import ch.kalunight.zoe.model.player_data.Player;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.request.RiotRequest;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.rithms.riot.constant.CallPriority;

public class ShowPlayerCommand extends Command {

  public static final String USAGE_NAME = "players";

  private final EventWaiter waiter;

  public ShowPlayerCommand(EventWaiter eventWaiter) {
    this.name = USAGE_NAME;
    String[] aliases = {"p", "player"};
    this.arguments = "";
    this.aliases = aliases;
    this.waiter = eventWaiter;
    this.help = "showPlayerHelpMessage";
    this.cooldown = 10;
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(ShowCommand.USAGE_NAME, name, arguments, help);
    Permission[] botPermissionNeeded = {Permission.MANAGE_EMOTES, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_MANAGE};
    this.botPermissions = botPermissionNeeded;
  }

  @Override
  protected void execute(CommandEvent event) {
    CommandUtil.sendTypingInFonctionOfChannelType(event);

    Paginator.Builder pbuilder = new Paginator.Builder()
        .setColumns(1)
        .setItemsPerPage(5)
        .showPageNumbers(true)
        .waitOnSinglePage(false)
        .useNumberedItems(true)
        .setFinalAction(m -> {
          try {
            m.clearReactions().queue();
          } catch(PermissionException ex) {
            m.delete().queue();
          }
        })
        .setEventWaiter(waiter)
        .setTimeout(1, TimeUnit.MINUTES);

    int page = 1;

    Server server = ServerData.getServers().get(event.getGuild().getId());

    if(server.getPlayers().isEmpty()) {
      event.reply(LanguageManager.getText(server.getLangage(), "showPlayerServerEmpty"));
      return;
    }

    for(Player player : server.getPlayers()) {
      StringBuilder playerInfo = new StringBuilder();
      playerInfo.append(String.format(LanguageManager.getText(server.getLangage(), "showPlayerName"),
          player.getDiscordUser().getName()) + "\n");

      if(player.getLolAccounts().isEmpty()) {
        playerInfo.append(LanguageManager.getText(server.getLangage(), "showPlayerNoAccount") + "\n");
      }

      for(LeagueAccount leagueAccount : player.getLolAccounts()) {
        playerInfo.append("-" + leagueAccount.getSummoner().getName() 
            + " (" + leagueAccount.getRegion().getName().toUpperCase() + ") Soloq Rank : "
            + RiotRequest.getSoloqRank(leagueAccount.getSummoner().getId(), leagueAccount.getRegion(), CallPriority.HIGH).toString() + "\n");
      }
      pbuilder.addItems(playerInfo.toString().substring(0, playerInfo.toString().length() - 1));
    }

    int accountsNmb = 0;
    for(Player player : server.getPlayers()) {
      accountsNmb += player.getLolAccounts().size();
    }
    Paginator p = pbuilder.setColor(Color.GREEN)
        .setText("List of players registered with their accounts (Players : " + server.getPlayers().size() + " | Accounts : " + accountsNmb + ") :")
        .setUsers(event.getAuthor())
        .build();
    p.paginate(event.getChannel(), page);

  }
}