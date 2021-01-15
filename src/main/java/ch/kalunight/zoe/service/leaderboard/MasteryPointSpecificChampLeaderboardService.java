package ch.kalunight.zoe.service.leaderboard;

import java.awt.Color;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.SavedSimpleMastery;
import ch.kalunight.zoe.model.dto.DTO.Leaderboard;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Player;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.leaderboard.dataholder.PlayerPoints;
import ch.kalunight.zoe.model.leaderboard.dataholder.SpecificChamp;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.rithms.riot.api.RiotApiException;

public class MasteryPointSpecificChampLeaderboardService extends LeaderboardBaseService {
  
  public MasteryPointSpecificChampLeaderboardService(long guildId, long channelId, long leaderboardId, boolean forceRefreshCache) {
    super(guildId, channelId, leaderboardId, forceRefreshCache);
  }

  @Override
  protected void runLeaderboardRefresh(Server server, Guild guild, TextChannel channel, Leaderboard leaderboard, Message message, List<Player> players, boolean forceRefreshCache)
      throws SQLException, RiotApiException {
    
    SpecificChamp specificChamp = gson.fromJson(leaderboard.lead_data, SpecificChamp.class);
    List<PlayerPoints> playersPoints = orderAndGetPlayers(guild, specificChamp.getChampion().getKey(), players, forceRefreshCache);
    
    List<String> playersName = new ArrayList<>();
    List<String> dataList = new ArrayList<>();
    
    for(PlayerPoints playerPoints : playersPoints) {
      playersName.add(playerPoints.getPlayer().getUser().getName() + "#" + playerPoints.getPlayer().getUser().getDiscriminator());
      dataList.add(masteryPointsFormat.format(playerPoints.getPoints()) + " " 
      + LanguageManager.getText(server.getLanguage(), "pointsShort"));
    }
    
    String playerTitle = LanguageManager.getText(server.getLanguage(), "leaderboardPlayersTitle");
    String dataName = LanguageManager.getText(server.getLanguage(), "leaderboardObjectiveMasterPoint");
    EmbedBuilder builder = buildBaseLeaderboardList(playerTitle, playersName,
        specificChamp.getChampion().getDisplayName() + " " + dataName, dataList);
    builder.setColor(Color.ORANGE);
    builder.setTitle(String.format(LanguageManager.getText(server.getLanguage(), "leaderboardObjectiveMasterPointGivenSpecifiedChamp"), 
        specificChamp.getChampion().getName()));
    builder.setFooter(LanguageManager.getText(server.getLanguage(), "leaderboardRefreshMessage"));
    message.editMessage(String.format(LanguageManager.getText(server.getLanguage(), "leaderboardObjectiveMasterPointGivenSpecifiedChamp"),
        specificChamp.getChampion().getName())).queue();
    message.editMessage(builder.build()).queue();
  }
  
  private List<PlayerPoints> orderAndGetPlayers(Guild guild, int championId, List<Player> players, boolean forceRefreshCache) throws SQLException, RiotApiException {
    List<PlayerPoints> playersPoints = new ArrayList<>();
    
    for(DTO.Player player : players) {
      List<LeagueAccount> leaguesAccounts = LeagueAccountRepository.getLeaguesAccountsWithPlayerID(guild.getIdLong(), player.player_id);
      
      long bestAccountPoints = 0;
      for(DTO.LeagueAccount leagueAccount : leaguesAccounts) {
        SavedSimpleMastery mastery = Zoe.getRiotApi().getChampionMasteriesBySummonerByChampionWithRateLimit(leagueAccount.leagueAccount_server,
            leagueAccount.leagueAccount_summonerId, championId, forceRefreshCache);
        
        if(mastery == null) {
          continue;
        }
        
        if(bestAccountPoints < mastery.getChampionPoints()) {
          bestAccountPoints = mastery.getChampionPoints();
        }
      }
      playersPoints.add(new PlayerPoints(player, bestAccountPoints));
    }
    
    Collections.sort(playersPoints);
    
    return playersPoints;
  }
}
