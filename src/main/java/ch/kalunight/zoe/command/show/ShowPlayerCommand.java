package ch.kalunight.zoe.command.show;

import java.awt.Color;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.Paginator;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.command.CommandUtil;
import ch.kalunight.zoe.model.LeagueAccount;
import ch.kalunight.zoe.model.Player;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.util.request.RiotRequest;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.exceptions.PermissionException;

public class ShowPlayerCommand extends Command {

  public static final String USAGE_NAME = "players";
  private final Paginator.Builder pbuilder;

  public ShowPlayerCommand(EventWaiter eventWaiter) {
    this.name = USAGE_NAME;
    String[] aliases = {"p", "player"};
    this.arguments = "";
    this.aliases = aliases;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = "Show all players with their accounts in the server.";
    this.cooldown = 10;
    this.helpBiConsumer = getHelpMethod();
    pbuilder = new Paginator.Builder().setColumns(1)
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
        .setEventWaiter(eventWaiter)
        .setTimeout(1, TimeUnit.MINUTES);
  }

  @Override
  protected void execute(CommandEvent event) {
    int page = 1;
    pbuilder.clearItems();

    Server server = ServerData.getServers().get(event.getGuild().getId());

    for(Player player : server.getPlayers()) {
      StringBuilder playerInfo = new StringBuilder();
      playerInfo.append("**" + player.getDiscordUser().getName() + " Accounts** : \n");
      for(LeagueAccount leagueAccount : player.getLolAccounts()) {
        playerInfo.append("-" + leagueAccount.getSummoner().getName() + " Soloq Rank : "
            + RiotRequest.getSoloqRank(leagueAccount.getSummoner().getId(), leagueAccount.getRegion()).toString() + "\n");
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
