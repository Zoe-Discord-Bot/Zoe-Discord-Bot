package ch.kalunight.zoe.service.leaderboard;

import java.awt.Color;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.kalunight.zoe.model.KDAReceiver;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Leaderboard;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.leaderboard.dataholder.Objective;
import ch.kalunight.zoe.model.leaderboard.dataholder.PlayerKDA;
import ch.kalunight.zoe.model.leaderboard.dataholder.SpecificChamp;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.request.RiotRequest;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.rithms.riot.api.RiotApiException;

public class KDALeaderboardService extends LeaderboardBaseService {

  public KDALeaderboardService(long guildId, long channelId, long leaderboardId) {
    super(guildId, channelId, leaderboardId);
  }

  @Override
  protected void runLeaderboardRefresh(Server server, Guild guild, TextChannel channel, Leaderboard leaderboard, Message message)
      throws SQLException, RiotApiException {
    
    Objective objective = Objective.getObjectiveWithId(leaderboard.lead_type);
    SpecificChamp specificChamp = null;
    if(Objective.AVERAGE_KDA_SPECIFIC_CHAMP.equals(objective)) {
      specificChamp = gson.fromJson(leaderboard.lead_data, SpecificChamp.class);
    }
    
    List<PlayerKDA> playersKDA = orderAndGetPlayers(guild, specificChamp);
    
    List<String> playersName = new ArrayList<>();
    List<String> dataList = new ArrayList<>();
    
    for(PlayerKDA playerKDA : playersKDA) {
      playersName.add(playerKDA.getPlayer().getUser().getAsMention());
      dataList.add("**"+ KDAReceiver.DECIMAL_FORMAT_KDA.format(playerKDA.getKdaReceiver().getAverageKDA()) + "** *(" + playerKDA.getKdaReceiver().getAverageStats() + ")*");
    }
    
    String playerTitle = LanguageManager.getText(server.serv_language, "leaderboardPlayersTitle");
    String dataName;
    if(specificChamp != null) {
      dataName = String.format(LanguageManager.getText(server.serv_language, "leaderboardObjectiveDataNameKDAWithSpecificChampion"), 
          specificChamp.getChampion().getEmoteUsable() + " " + specificChamp.getChampion().getName());
    }else {
      dataName = LanguageManager.getText(server.serv_language, "leaderboardObjectiveDataNameKDA");
    }
    
    EmbedBuilder builder = buildBaseLeaderboardList(playerTitle, playersName, dataName, dataList);
    builder.setColor(Color.ORANGE);
    
    if(specificChamp != null) {
      String leaderboardTitle = String.format(LanguageManager.getText(server.serv_language, "leaderboardObjectiveKDAWithSpecificChampionTitle"), 
          specificChamp.getChampion().getName());
      builder.setTitle(leaderboardTitle);
      message.editMessage(leaderboardTitle).queue();
    }else {
      String leaderboardTitle = LanguageManager.getText(server.serv_language, "leaderboardObjectiveKDATitle");
      builder.setTitle(leaderboardTitle);
      message.editMessage(leaderboardTitle).queue();
    }
    
    builder.setFooter(LanguageManager.getText(server.serv_language, "leaderboardRefreshMessage"));
    message.editMessage(builder.build()).queue();
  }
  
  private List<PlayerKDA> orderAndGetPlayers(Guild guild, SpecificChamp champ) throws SQLException {
    List<DTO.Player> players = PlayerRepository.getPlayers(guild.getIdLong());
    List<PlayerKDA> playersKDA = new ArrayList<>();
    
    for(DTO.Player player : players) {
      List<LeagueAccount> leaguesAccounts = LeagueAccountRepository.getLeaguesAccountsWithPlayerID(guild.getIdLong(), player.player_id);
      
      KDAReceiver bestAccountKDA = null;
      for(DTO.LeagueAccount leagueAccount : leaguesAccounts) {
        
        KDAReceiver kdaReceiver;
        
        if(champ == null) {
          kdaReceiver = RiotRequest.getKDALastMonth(leagueAccount.leagueAccount_summonerId, leagueAccount.leagueAccount_server);
        }else {
          kdaReceiver = RiotRequest.getKDALastMonthOneChampionOnly(leagueAccount.leagueAccount_summonerId, leagueAccount.leagueAccount_server, champ.getChampion().getKey());
        }
        
        if(kdaReceiver == null) {
          continue;
        }
        
        if(bestAccountKDA == null || bestAccountKDA.getAverageKDA() < kdaReceiver.getAverageKDA()) {
          bestAccountKDA = kdaReceiver;
        }
      }
      
      if(bestAccountKDA != null) {
        playersKDA.add(new PlayerKDA(player, bestAccountKDA));
      }
    }
    
    Collections.sort(playersKDA);
    
    return playersKDA;
  }
  
}
