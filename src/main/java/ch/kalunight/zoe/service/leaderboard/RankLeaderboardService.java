package ch.kalunight.zoe.service.leaderboard;

import java.awt.Color;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ch.kalunight.zoe.exception.NoValueRankException;
import ch.kalunight.zoe.model.GameQueueConfigId;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.LastRank;
import ch.kalunight.zoe.model.dto.DTO.Leaderboard;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Player;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.leaderboard.dataholder.Objective;
import ch.kalunight.zoe.model.leaderboard.dataholder.PlayerRank;
import ch.kalunight.zoe.model.leaderboard.dataholder.QueueSelected;
import ch.kalunight.zoe.model.player_data.FullTier;
import ch.kalunight.zoe.repositories.LastRankRepository;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.Ressources;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;

public class RankLeaderboardService extends LeaderboardBaseService {

  public RankLeaderboardService(long guildId, long channelId, long leaderboardId, boolean forceRefreshCache) {
    super(guildId, channelId, leaderboardId, forceRefreshCache);
  }

  @Override
  protected void runLeaderboardRefresh(Server server, Guild guild, TextChannel channel, Leaderboard leaderboard,
      Message message, List<Player> players, boolean forceRefreshCache) throws SQLException, RiotApiException {

    Objective objective = Objective.getObjectiveWithId(leaderboard.lead_type);
    QueueSelected queueSelected = null;
    if(Objective.SPECIFIC_QUEUE_RANK.equals(objective)) {
      queueSelected = gson.fromJson(leaderboard.lead_data, QueueSelected.class);
    }

    List<PlayerRank> playersRank = orderAndGetPlayers(guild, objective, queueSelected, players, forceRefreshCache);

    List<String> playersName = new ArrayList<>();
    List<String> dataList = new ArrayList<>();

    for(PlayerRank playerRank : playersRank) {
      playersName.add(playerRank.getPlayer().getUser().getName() + "#" + playerRank.getPlayer().getUser().getDiscriminator());
      FullTier fullTier = playerRank.getFullTier();
      if(queueSelected == null) {
        dataList.add(Ressources.getTierEmote().get(fullTier.getTier()).getUsableEmote() + " " + fullTier.toString(server.getLanguage()) 
        + " (" + LanguageManager.getText(server.getLanguage(), playerRank.getQueue().getNameId()) + ")");
      }else {
        dataList.add(Ressources.getTierEmote().get(fullTier.getTier()).getUsableEmote() + " " + fullTier.toString(server.getLanguage()));
      }
    }

    String playerTitle = LanguageManager.getText(server.getLanguage(), "leaderboardPlayersTitle");
    String dataName;
    if(queueSelected == null) {
      dataName = LanguageManager.getText(server.getLanguage(), "leaderboardRankTitle");
    }else {
      dataName = String.format(LanguageManager.getText(server.getLanguage(), "leaderboardRankSpecificQueueTitle"), LanguageManager.getText(server.getLanguage(), queueSelected.getGameQueue().getNameId()));
    }

    EmbedBuilder builder = buildBaseLeaderboardList(playerTitle, playersName, dataName, dataList);
    builder.setColor(Color.ORANGE);
    if(queueSelected == null) {
      builder.setTitle(LanguageManager.getText(server.getLanguage(), "leaderboardObjectiveRankAllTitle"));
      message.editMessage(LanguageManager.getText(server.getLanguage(), "leaderboardObjectiveRankAllTitle")).queue();
    }else {
      builder.setTitle(String.format(LanguageManager.getText(server.getLanguage(), "leaderboardObjectiveRankSpecificTitle"), 
          LanguageManager.getText(server.getLanguage(), queueSelected.getGameQueue().getNameId())));
      message.editMessage(String.format(LanguageManager.getText(server.getLanguage(), "leaderboardObjectiveRankSpecificTitle"), 
          LanguageManager.getText(server.getLanguage(), queueSelected.getGameQueue().getNameId()))).queue();
    }

    builder.setFooter(LanguageManager.getText(server.getLanguage(), "leaderboardRefreshMessage"));
    message.editMessage(builder.build()).queue();

  }

  private List<PlayerRank> orderAndGetPlayers(Guild guild, Objective objective, QueueSelected queueSelected, List<Player> players, boolean forceRefreshCache) throws SQLException, RiotApiException {
    List<PlayerRank> playersPoints = new ArrayList<>();

    for(DTO.Player player : players) {
      List<LeagueAccount> leaguesAccounts = LeagueAccountRepository.getLeaguesAccountsWithPlayerID(guild.getIdLong(), player.player_id);

      long bestAccountRank = 0;
      FullTier fullTierBestAccountRank = null;
      GameQueueConfigId queue = null;
      for(DTO.LeagueAccount leagueAccount : leaguesAccounts) {
        List<LeagueEntry> leaguesEntries = new ArrayList<>();
        
        LastRank lastRank = LastRankRepository.getLastRankWithLeagueAccountId(leagueAccount.leagueAccount_id);

        if(lastRank != null) {
          if(lastRank.getLastRankSoloq() != null) {
            leaguesEntries.add(lastRank.getLastRankSoloq());
          }
          
          if(lastRank.getLastRankFlex() != null) {
            leaguesEntries.add(lastRank.getLastRankFlex());
          }
          
          if(lastRank.getLastRankTft() != null) {
            leaguesEntries.add(lastRank.getLastRankTft());
          }
        }

        long rankValue = 0;
        FullTier fullTierRankValue = null;
        GameQueueConfigId queueOfRankValue = null;
        if(queueSelected != null) {
          for(LeagueEntry leagueEntry : leaguesEntries) {
            if(leagueEntry.getQueueType().equals(queueSelected.getGameQueue().getQueueType())) {
              try {
                fullTierRankValue = new FullTier(leagueEntry);
                rankValue = fullTierRankValue.value();
                queueOfRankValue = GameQueueConfigId.getGameQueueWithQueueType(leagueEntry.getQueueType());
              } catch (NoValueRankException e) {
                logger.debug("FullTier impossible to create", e);
              }
            }
          }
        }else {
          long bestRankQueueAccountValue = 0;
          FullTier bestRankQueueAccountFullTier = null;
          GameQueueConfigId queueSelectedByQueue = null;
          for(LeagueEntry leagueEntry : leaguesEntries) {
            try {
              FullTier queueToEvaluate = new FullTier(leagueEntry);
              if(bestRankQueueAccountValue < queueToEvaluate.value() || bestRankQueueAccountFullTier == null 
                  || (bestRankQueueAccountFullTier.compareTo(queueToEvaluate) > 0)) {
                bestRankQueueAccountFullTier = queueToEvaluate;
                bestRankQueueAccountValue = bestRankQueueAccountFullTier.value();
                queueSelectedByQueue = GameQueueConfigId.getGameQueueWithQueueType(leagueEntry.getQueueType());
              }
            } catch (NoValueRankException e) {
              logger.debug("FullTier impossible to create", e);
            }
          }

          rankValue = bestRankQueueAccountValue;
          fullTierRankValue = bestRankQueueAccountFullTier;
          queueOfRankValue = queueSelectedByQueue;
        }

        if(bestAccountRank < rankValue || (bestAccountRank == rankValue 
            && fullTierBestAccountRank != null && fullTierRankValue != null 
            && (fullTierBestAccountRank.compareTo(fullTierRankValue) < 0))) {
          bestAccountRank = rankValue;
          fullTierBestAccountRank = fullTierRankValue;

          queue = queueOfRankValue;
        }
      }
      if(fullTierBestAccountRank != null) {
        playersPoints.add(new PlayerRank(player, fullTierBestAccountRank, queue));
      }
    }

    Collections.sort(playersPoints);

    return playersPoints;
  }

}
