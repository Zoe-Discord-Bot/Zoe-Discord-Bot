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
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.leaderboard.dataholder.PlayerPoints;
import ch.kalunight.zoe.model.leaderboard.dataholder.SpecificChamp;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.champion_mastery.dto.ChampionMastery;

public class MasteryPointSpecificChampLeaderboardService extends LeaderboardBaseService {
  
  public MasteryPointSpecificChampLeaderboardService(long guildId, long channelId, long leaderboardId) {
    super(guildId, channelId, leaderboardId);
  }

  @Override
  protected void runLeaderboardRefresh(Server server, Guild guild, TextChannel channel, Leaderboard leaderboard, Message message)
      throws SQLException, RiotApiException {
    
    SpecificChamp specificChamp = gson.fromJson(leaderboard.lead_data, SpecificChamp.class);
    List<PlayerPoints> playersPoints = orderAndGetPlayers(guild, specificChamp.getChampion().getKey());
    
    List<String> playersName = new ArrayList<>();
    List<String> dataList = new ArrayList<>();
    
    for(PlayerPoints playerPoints : playersPoints) {
      playersName.add(playerPoints.getPlayer().getUser().getAsMention());
      dataList.add(masteryPointsFormat.format(playerPoints.getPoints()) + " " 
      + LanguageManager.getText(server.serv_language, "pointsShort"));
    }
    
    String playerTitle = LanguageManager.getText(server.serv_language, "leaderboardPlayersTitle");
    String dataName = LanguageManager.getText(server.serv_language, "leaderboardObjectiveMasterPoint");
    EmbedBuilder builder = buildBaseLeaderboardList(playerTitle, playersName,
        specificChamp.getChampion().getDisplayName() + " " + dataName, dataList);
    builder.setColor(Color.ORANGE);
    builder.setTitle(String.format(LanguageManager.getText(server.serv_language, "leaderboardObjectiveMasterPointGivenSpecifiedChamp"), 
        specificChamp.getChampion().getName()));
    builder.setFooter(LanguageManager.getText(server.serv_language, "leaderboardRefreshMessage"));
    message.editMessage(String.format(LanguageManager.getText(server.serv_language, "leaderboardObjectiveMasterPointGivenSpecifiedChamp"),
        specificChamp.getChampion().getName())).queue();
    message.editMessage(builder.build()).queue();
  }
  
  private List<PlayerPoints> orderAndGetPlayers(Guild guild, int championId) throws SQLException, RiotApiException {
    List<DTO.Player> players = PlayerRepository.getPlayers(guild.getIdLong());
    List<PlayerPoints> playersPoints = new ArrayList<>();
    
    for(DTO.Player player : players) {
      List<LeagueAccount> leaguesAccounts = LeagueAccountRepository.getLeaguesAccountsWithPlayerID(guild.getIdLong(), player.player_id);
      
      long bestAccountPoints = 0;
      for(DTO.LeagueAccount leagueAccount : leaguesAccounts) {
        ChampionMastery mastery = Zoe.getRiotApi().getChampionMasteriesBySummonerByChampionWithRateLimit(leagueAccount.leagueAccount_server,
            leagueAccount.leagueAccount_summonerId, championId);
        
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
