package ch.kalunight.zoe.service.leaderboard;

import java.awt.Color;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Leaderboard;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Player;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.leaderboard.dataholder.PlayerPoints;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.ZoeUserRankManagementUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import no.stelar7.api.r4j.pojo.lol.championmastery.ChampionMastery;

public class MasteryPointLeaderboardService extends LeaderboardBaseService {
  
  public MasteryPointLeaderboardService(long guildId, long channelId, long leaderboardId) {
    super(guildId, channelId, leaderboardId);
  }

  @Override
  protected void runLeaderboardRefresh(Server server, Guild guild, TextChannel channel, Leaderboard leaderboard, Message message, List<Player> players)
      throws SQLException {
    List<PlayerPoints> playersPoints = orderAndGetPlayers(guild, players);
    
    List<String> playersName = new ArrayList<>();
    List<String> dataList = new ArrayList<>();
    
    for(PlayerPoints playerPoints : playersPoints) {
      playersName.add(ZoeUserRankManagementUtil.getEmotesByDiscordId(playerPoints.getPlayer().player_discordId) 
          + playerPoints.getPlayer().retrieveUser(guild.getJDA()).getName() + "#" + playerPoints.getPlayer().retrieveUser(guild.getJDA()).getDiscriminator());
      dataList.add(masteryPointsFormat.format(playerPoints.getPoints()) + " " 
      + LanguageManager.getText(server.getLanguage(), "pointsShort"));
    }
    
    String playerTitle = LanguageManager.getText(server.getLanguage(), "leaderboardPlayersTitle");
    String dataName = LanguageManager.getText(server.getLanguage(), "leaderboardObjectiveTotalMasterPoint");
    EmbedBuilder builder = buildBaseLeaderboardList(playerTitle, playersName, dataName, dataList, server);
    builder.setColor(Color.ORANGE);
    builder.setTitle(LanguageManager.getText(server.getLanguage(), "leaderboardObjectiveMasterPointTitle"));
    message.editMessage(LanguageManager.getText(server.getLanguage(), "leaderboardObjectiveMasterPointTitle")).setEmbeds(builder.build()).queue();
  }

  private List<PlayerPoints> orderAndGetPlayers(Guild guild, List<Player> players) throws SQLException {
    List<PlayerPoints> playersPoints = new ArrayList<>();
    
    for(DTO.Player player : players) {
      List<LeagueAccount> leaguesAccounts = LeagueAccountRepository.getLeaguesAccountsWithPlayerID(guild.getIdLong(), player.player_id);
      
      long bestAccountPoints = 0;
      for(DTO.LeagueAccount leagueAccount : leaguesAccounts) {
        List<ChampionMastery> masteries = Zoe.getRiotApi().getChampionMasteryBySummonerId(leagueAccount.leagueAccount_server, leagueAccount.leagueAccount_summonerId);
        
        long totalAccountPoints = 0;
        for(ChampionMastery mastery : masteries) {
          totalAccountPoints += mastery.getChampionPoints();
        }
        
        if(bestAccountPoints < totalAccountPoints) {
          bestAccountPoints = totalAccountPoints;
        }
      }
      playersPoints.add(new PlayerPoints(player, bestAccountPoints));
    }
    
    Collections.sort(playersPoints);
    
    return playersPoints;
  }

}
