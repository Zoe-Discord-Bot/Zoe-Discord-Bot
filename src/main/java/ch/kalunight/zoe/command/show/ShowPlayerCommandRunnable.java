package ch.kalunight.zoe.command.show;

import java.awt.Color;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.Paginator;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.dto.DTO.SummonerCache;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import ch.kalunight.zoe.util.RiotApiUtil;
import ch.kalunight.zoe.util.request.RiotRequest;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.rithms.riot.api.RiotApiException;

public class ShowPlayerCommandRunnable {

  public static final String USAGE_NAME = "players";

  private ShowPlayerCommandRunnable() {
    // hide default public command
  }

  public static void executeCommand(Server server, EventWaiter waiter, Member member, TextChannel channel, Message messageToEdit, InteractionHook hook, Member mentionnedUser) throws SQLException {

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

    List<DTO.Player> players = PlayerRepository.getPlayers(server.serv_guildId);

    if(players.isEmpty()) {
      CommandUtil.sendMessageWithClassicOrSlashCommand(LanguageManager.getText(server.getLanguage(), "showPlayerServerEmpty"), messageToEdit, hook);
      return;
    }

    if(mentionnedUser == null) {
      try {
        int accountsNmb = 0;
        for(DTO.Player player : players) {
          accountsNmb = addPlayerToEmbed(server, member, messageToEdit, hook, pbuilder, accountsNmb, player);
        }

        CommandUtil.sendMessageWithClassicOrSlashCommand(String.format(LanguageManager.getText(server.getLanguage(), "showPlayerEmbedTitle"), players.size(), accountsNmb), messageToEdit, hook);
      }catch (RiotApiException e) {
        CommandUtil.sendMessageWithClassicOrSlashCommand(RiotApiUtil.getTextHandlerRiotApiError(e, server.getLanguage()), messageToEdit, hook);
        return;
      }
    }else {
      
      boolean playerDetected = false;
      for(DTO.Player player : players) {
        if(player.player_discordId == mentionnedUser.getIdLong()) {
          try {
            addPlayerToEmbed(server, member, messageToEdit, hook, pbuilder, 0, player);
            playerDetected = true;
          } catch (RiotApiException e) {
            CommandUtil.sendMessageWithClassicOrSlashCommand(RiotApiUtil.getTextHandlerRiotApiError(e, server.getLanguage()), messageToEdit, hook);
            return;
          }
        }
      }
      
      if(!playerDetected) {
        CommandUtil.sendMessageWithClassicOrSlashCommand(LanguageManager.getText(server.getLanguage(), "showPlayersCommandNeedToMentionAPlayer"), messageToEdit, hook);
        return;
      }else {
        CommandUtil.sendMessageWithClassicOrSlashCommand(LanguageManager.getText(server.getLanguage(), "showPlayersCommandMentionPlayerShow"), messageToEdit, hook);
      }
    }

    Paginator p = pbuilder.setColor(Color.GREEN)
        .setUsers(member.getUser())
        .setText("")
        .build();
    p.paginate(channel, page);
  }

  private static int addPlayerToEmbed(Server server, Member member, Message messageToEdit, InteractionHook hook,
      Paginator.Builder pbuilder, int accountsNmb, DTO.Player player) throws SQLException, RiotApiException {
    StringBuilder playerInfo = new StringBuilder();
    User user = member.getGuild().retrieveMemberById(player.player_discordId).complete().getUser();
    playerInfo.append(String.format(LanguageManager.getText(server.getLanguage(), "showPlayerName"),
        user.getName()) + "\n");

    List<DTO.LeagueAccount> leagueAccounts = LeagueAccountRepository.getLeaguesAccounts(server.serv_guildId, user.getIdLong());

    if(leagueAccounts.isEmpty()) {
      playerInfo.append(LanguageManager.getText(server.getLanguage(), "showPlayerNoAccount") + "\n");
    }
    accountsNmb += leagueAccounts.size();
    for(DTO.LeagueAccount leagueAccount : leagueAccounts) {
      SummonerCache summoner = Zoe.getRiotApi().getSummoner(leagueAccount.leagueAccount_server,
          leagueAccount.leagueAccount_summonerId, false);
      playerInfo.append(String.format(LanguageManager.getText(server.getLanguage(), "showPlayerAccount"),
          summoner.getSumCacheData().getName(), leagueAccount.leagueAccount_server.getName().toUpperCase(),
          RiotRequest.getSoloqRank(leagueAccount.leagueAccount_summonerId,
              leagueAccount.leagueAccount_server).toString(server.getLanguage())) + "\n");
    }
    pbuilder.addItems(playerInfo.toString().substring(0, playerInfo.toString().length() - 1));
    return accountsNmb;
  }
}