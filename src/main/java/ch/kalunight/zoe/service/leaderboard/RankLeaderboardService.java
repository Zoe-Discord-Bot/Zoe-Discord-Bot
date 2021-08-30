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
import ch.kalunight.zoe.util.ZoeUserRankManagementUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import no.stelar7.api.r4j.basic.constants.types.lol.GameQueueType;
import no.stelar7.api.r4j.pojo.lol.league.LeagueEntry;

public class RankLeaderboardService extends LeaderboardBaseService {

  public RankLeaderboardService(long guildId, long channelId, long leaderboardId) {
    super(guildId, channelId, leaderboardId);
  }

  @Override
  protected void runLeaderboardRefresh(Server server, Guild guild, TextChannel channel, Leaderboard leaderboard,
      Message message, List<Player> players) throws SQLException {

    Objective objective = Objective.getObjectiveWithId(leaderboard.lead_type);
    QueueSelected queueSelected = null;
    if(Objective.SPECIFIC_QUEUE_RANK.equals(objective)) {
      queueSelected = gson.fromJson(leaderboard.lead_data, QueueSelected.class);
    }

    List<PlayerRank> playersRank = orderAndGetPlayers(guild, objective, queueSelected, players);

    List<String> playersName = new ArrayList<>();
    List<String> dataList = new ArrayList<>();

    for(PlayerRank playerRank : playersRank) {
      playersName.add(ZoeUserRankManagementUtil.getEmotesByDiscordId(playerRank.getPlayer().player_discordId) + playerRank.getPlayer().retrieveUser(guild.getJDA()).getName() + "#" + playerRank.getPlayer().retrieveUser(guild.getJDA()).getDiscriminator());
      FullTier fullTier = playerRank.getFullTier();
      if(queueSelected == null) {
        dataList.add(Ressources.getTierEmote().get(fullTier.getTier()).getUsableEmote() + " " + fullTier.toString(server.getLanguage()) 
        + " (" + LanguageManager.getText(server.getLanguage(), GameQueueConfigId.getGameQueueIdWithQueueType(playerRank.getQueue()).getNameId()) + ")");
      }else {
        dataList.add(Ressources.getTierEmote().get(fullTier.getTier()).getUsableEmote() + " " + fullTier.toString(server.getLanguage()));
      }
    }

    String playerTitle = LanguageManager.getText(server.getLanguage(), "leaderboardPlayersTitle");
    String dataName;
    if(queueSelected == null) {
      dataName = LanguageManager.getText(server.getLanguage(), "leaderboardRankTitle");
    }else {
      dataName = String.format(LanguageManager.getText(server.getLanguage(), "leaderboardRankSpecificQueueTitle"), LanguageManager.getText(server.getLanguage(), 
          queueSelected.getGameQueueId().getNameId()));
    }

    EmbedBuilder builder = buildBaseLeaderboardList(playerTitle, playersName, dataName, dataList, server);
    builder.setColor(Color.ORANGE);
    
    String leaderboardMessageTitle;
    
    if(queueSelected == null) {
      builder.setTitle(LanguageManager.getText(server.getLanguage(), "leaderboardObjectiveRankAllTitle"));
      leaderboardMessageTitle = LanguageManager.getText(server.getLanguage(), "leaderboardObjectiveRankAllTitle");
    }else {
      builder.setTitle(String.format(LanguageManager.getText(server.getLanguage(), "leaderboardObjectiveRankSpecificTitle"), 
          LanguageManager.getText(server.getLanguage(), queueSelected.getGameQueueId().getNameId())));
      leaderboardMessageTitle = String.format(LanguageManager.getText(server.getLanguage(), "leaderboardObjectiveRankSpecificTitle"), 
          LanguageManager.getText(server.getLanguage(), queueSelected.getGameQueueId().getNameId()));
    }
    
    message.editMessage(leaderboardMessageTitle).setEmbeds(builder.build()).queue();
  }

  private List<PlayerRank> orderAndGetPlayers(Guild guild, Objective objective, QueueSelected queueSelected, List<Player> players) throws SQLException {
    List<PlayerRank> playersPoints = new ArrayList<>();

    for(DTO.Player player : players) {
      List<LeagueAccount> leaguesAccounts = LeagueAccountRepository.getLeaguesAccountsWithPlayerID(guild.getIdLong(), player.player_id);

      long bestAccountRank = 0;
      FullTier fullTierBestAccountRank = null;
      GameQueueType queue = null;
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
        GameQueueType queueOfRankValue = null;
        if(queueSelected != null) {
          for(LeagueEntry leagueEntry : leaguesEntries) {
            if(queueSelected.getGameQueueId().equals(
                GameQueueConfigId.getGameQueueIdWithQueueType(leagueEntry.getQueueType()))) {
              try {
                fullTierRankValue = new FullTier(leagueEntry);
                rankValue = fullTierRankValue.value();
                queueOfRankValue = leagueEntry.getQueueType();
              } catch (NoValueRankException e) {
                logger.debug("FullTier impossible to create", e);
              }
            }
          }
        }else {
          long bestRankQueueAccountValue = 0;
          FullTier bestRankQueueAccountFullTier = null;
          GameQueueType queueSelectedByQueue = null;
          for(LeagueEntry leagueEntry : leaguesEntries) {
            try {
              FullTier queueToEvaluate = new FullTier(leagueEntry);
              if(bestRankQueueAccountValue < queueToEvaluate.value() || bestRankQueueAccountFullTier == null 
                  || (bestRankQueueAccountFullTier.compareTo(queueToEvaluate) > 0)) {
                bestRankQueueAccountFullTier = queueToEvaluate;
                bestRankQueueAccountValue = bestRankQueueAccountFullTier.value();
                queueSelectedByQueue = leagueEntry.getQueueType();
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
