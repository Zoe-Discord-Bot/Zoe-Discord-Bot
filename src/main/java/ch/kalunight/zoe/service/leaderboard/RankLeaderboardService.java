package ch.kalunight.zoe.service.leaderboard;

import java.awt.Color;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.exception.NoValueRankException;
import ch.kalunight.zoe.model.GameQueueConfigId;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Leaderboard;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.leaderboard.dataholder.Objective;
import ch.kalunight.zoe.model.leaderboard.dataholder.PlayerRank;
import ch.kalunight.zoe.model.leaderboard.dataholder.QueueSelected;
import ch.kalunight.zoe.model.player_data.FullTier;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.Ressources;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;

public class RankLeaderboardService extends LeaderboardBaseService {

  public RankLeaderboardService(long guildId, long channelId, long leaderboardId) {
    super(guildId, channelId, leaderboardId);
  }

  @Override
  protected void runLeaderboardRefresh(Server server, Guild guild, TextChannel channel, Leaderboard leaderboard,
      Message message) throws SQLException, RiotApiException {

    Objective objective = Objective.getObjectiveWithId(leaderboard.lead_type);
    QueueSelected queueSelected = null;
    if(Objective.SPECIFIC_QUEUE_RANK.equals(objective)) {
      queueSelected = gson.fromJson(leaderboard.lead_data, QueueSelected.class);
    }
    
    List<PlayerRank> playersRank = orderAndGetPlayers(guild, objective, queueSelected);
    
    List<String> playersName = new ArrayList<>();
    List<String> dataList = new ArrayList<>();
    
    for(PlayerRank playerRank : playersRank) {
      playersName.add(playerRank.getPlayer().user.getAsMention());
      FullTier fullTier = new FullTier((int) playerRank.getPoints());
      if(queueSelected == null) {
        dataList.add(Ressources.getTierEmote().get(fullTier.getTier()).getUsableEmote() + " " + fullTier.toString(server.serv_language) 
        + " (" + LanguageManager.getText(server.serv_language, playerRank.getQueue().getNameId()) + ")");
      }else {
        dataList.add(Ressources.getTierEmote().get(fullTier.getTier()).getUsableEmote() + " " + fullTier.toString(server.serv_language));
      }
    }
    
    String playerTitle = LanguageManager.getText(server.serv_language, "leaderboardPlayersTitle");
    String dataName;
    if(queueSelected == null) {
      dataName = LanguageManager.getText(server.serv_language, "leaderboardRankTitle");
    }else {
      dataName = String.format(LanguageManager.getText(server.serv_language, "leaderboardRankSpecificQueueTitle"), LanguageManager.getText(server.serv_language, queueSelected.getGameQueue().getNameId()));
    }
    
    EmbedBuilder builder = buildBaseLeaderboardList(playerTitle, playersName, dataName, dataList);
    builder.setColor(Color.ORANGE);
    if(queueSelected == null) {
      builder.setTitle(LanguageManager.getText(server.serv_language, "leaderboardObjectiveRankAllTitle"));
      message.editMessage(LanguageManager.getText(server.serv_language, "leaderboardObjectiveRankAllTitle")).queue();
    }else {
      builder.setTitle(String.format(LanguageManager.getText(server.serv_language, "leaderboardObjectiveRankSpecificTitle"), 
          LanguageManager.getText(server.serv_language, queueSelected.getGameQueue().getNameId())));
      message.editMessage(String.format(LanguageManager.getText(server.serv_language, "leaderboardObjectiveRankSpecificTitle"), 
          LanguageManager.getText(server.serv_language, queueSelected.getGameQueue().getNameId()))).queue();
    }

    builder.setFooter(LanguageManager.getText(server.serv_language, "leaderboardRefreshMessage"));
    message.editMessage(builder.build()).queue();

  }

  private List<PlayerRank> orderAndGetPlayers(Guild guild, Objective objective, QueueSelected queueSelected) throws SQLException, RiotApiException {
    List<DTO.Player> players = PlayerRepository.getPlayers(guild.getIdLong());
    List<PlayerRank> playersPoints = new ArrayList<>();

    for(DTO.Player player : players) {
      List<LeagueAccount> leaguesAccounts = LeagueAccountRepository.getLeaguesAccountsWithPlayerID(guild.getIdLong(), player.player_id);

      long bestAccountRank = 0;
      GameQueueConfigId queue = null;
      for(DTO.LeagueAccount leagueAccount : leaguesAccounts) {
        Set<LeagueEntry> leaguesEntry = Zoe.getRiotApi().getLeagueEntriesBySummonerIdWithRateLimit(leagueAccount.leagueAccount_server,
            leagueAccount.leagueAccount_summonerId);

        long rankValue = 0;
        GameQueueConfigId queueOfRankValue = null;
        if(queueSelected != null) {
          for(LeagueEntry leagueEntry : leaguesEntry) {
            if(leagueEntry.getQueueType().equals(queueSelected.getGameQueue().getQueueType())) {
              try {
                rankValue = new FullTier(leagueEntry).value();
                queueOfRankValue = GameQueueConfigId.getGameQueueWithQueueType(leagueEntry.getQueueType());
              } catch (NoValueRankException e) {
                logger.debug("FullTier impossible to create", e);
              }
            }
          }
        }else {
          long bestRankAccount = 0;
          for(LeagueEntry leagueEntry : leaguesEntry) {
            try {
              rankValue = new FullTier(leagueEntry).value();
              if(bestRankAccount < rankValue) {
                bestRankAccount = rankValue;
                queueOfRankValue = GameQueueConfigId.getGameQueueWithQueueType(leagueEntry.getQueueType());
              }
            } catch (NoValueRankException e) {
              logger.debug("FullTier impossible to create", e);
            }
          }
        }

        if(bestAccountRank < rankValue) {
          bestAccountRank = rankValue;
          queue = queueOfRankValue;
        }
      }
      playersPoints.add(new PlayerRank(player, bestAccountRank, queue));
    }

    Collections.sort(playersPoints);

    return playersPoints;
  }

}
