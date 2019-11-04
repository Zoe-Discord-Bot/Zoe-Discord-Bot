package ch.kalunight.zoe.command.show;

import java.awt.Color;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.Paginator;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.player_data.LeagueAccount;
import ch.kalunight.zoe.model.player_data.Player;
import ch.kalunight.zoe.util.CommandUtil;
import ch.kalunight.zoe.util.request.RiotRequest;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.rithms.riot.constant.CallPriority;

public class ShowPlayerCommand extends ZoeCommand {

  public static final String USAGE_NAME = "players";

  private final EventWaiter waiter;

  public ShowPlayerCommand(EventWaiter eventWaiter) {
    this.name = USAGE_NAME;
    String[] aliases = {"p", "player"};
    this.arguments = "";
    this.aliases = aliases;
    this.waiter = eventWaiter;
    this.help = "Show all players with their accounts in the server.";
    this.cooldown = 10;
    this.helpBiConsumer = getHelpMethod();
    Permission[] botPermissionNeeded = {Permission.MANAGE_EMOTES, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_MANAGE};
    this.botPermissions = botPermissionNeeded;
  }

  @Override
  protected void executeCommand(CommandEvent event) {
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
      event.reply("The server have 0 player registered.");
      return;
    }

    for(Player player : server.getPlayers()) {
      StringBuilder playerInfo = new StringBuilder();
      playerInfo.append("**" + player.getDiscordUser().getName() + " Accounts** : \n");

      if(player.getLolAccounts().isEmpty()) {
        playerInfo.append("*No Account Link*\n");
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

  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Show players command :\n");
        stringBuilder.append("--> `>show " + name + " " + arguments + "` : " + help);

        event.reply(stringBuilder.toString());
      }
    };
  }
}
