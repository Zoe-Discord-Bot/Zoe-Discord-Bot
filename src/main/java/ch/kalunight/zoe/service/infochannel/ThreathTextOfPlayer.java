package ch.kalunight.zoe.service.infochannel;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.LastRank;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Player;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.player_data.FullTier;
import ch.kalunight.zoe.repositories.CurrentGameInfoRepository;
import ch.kalunight.zoe.repositories.LastRankRepository;
import ch.kalunight.zoe.riotapi.CachedRiotApi;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.FullTierUtil;
import ch.kalunight.zoe.util.InfoPanelRefresherUtil;
import ch.kalunight.zoe.util.Ressources;
import ch.kalunight.zoe.util.request.RiotRequest;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;

public class ThreathTextOfPlayer implements Runnable {

  protected static final List<Player> playersInWork = Collections.synchronizedList(new ArrayList<>());

  protected static final Logger logger = LoggerFactory.getLogger(ThreathTextOfPlayer.class);

  protected static final CachedRiotApi riotApi = Zoe.getRiotApi();

  private Player player;

  private Server server;
  
  private ServerConfiguration serverConfig;

  private StringBuilder stringMessage;

  public ThreathTextOfPlayer(Server server, Player player, ServerConfiguration configuration) {
    this.player = player;
    this.server = server;
    this.stringMessage = new StringBuilder();
    this.serverConfig = configuration;
    playersInWork.add(player);
  }


  @Override
  public void run() {
    try {
      List<DTO.LeagueAccount> leagueAccounts = player.leagueAccounts;

      List<DTO.LeagueAccount> accountsInGame = new ArrayList<>();
      List<DTO.LeagueAccount> accountNotInGame = new ArrayList<>();

      for(DTO.LeagueAccount leagueAccount : leagueAccounts) {
        DTO.CurrentGameInfo currentGameInfo = CurrentGameInfoRepository.getCurrentGameWithLeagueAccountID(leagueAccount.leagueAccount_id);
        if(currentGameInfo != null) {
          accountsInGame.add(leagueAccount);
        }else {
          accountNotInGame.add(leagueAccount);
        }
      }

      if(accountsInGame.isEmpty()) {

        if(serverConfig.getInfopanelRankedOption().isOptionActivated()) {

          if(accountNotInGame.size() == 1) {

            LeagueAccount leagueAccount = accountNotInGame.get(0);

            getTextInformationPanelRankOption(stringMessage, player, leagueAccount, false);
          }else {

            stringMessage.append(String.format(LanguageManager.getText(server.serv_language, "infoPanelRankedTitleMultipleAccount"), player.user.getAsMention()) + "\n");

            for(DTO.LeagueAccount leagueAccount : accountNotInGame) {

              getTextInformationPanelRankOption(stringMessage, player, leagueAccount, true);
            }
          }

        } else {
          notInGameWithoutRankInfo(stringMessage, player);
        }
      }else if (accountsInGame.size() == 1) {
        stringMessage.append(player.user.getAsMention() + " : " 
            + InfoPanelRefresherUtil.getCurrentGameInfoStringForOneAccount(accountsInGame.get(0), server.serv_language) + "\n");
      }else {
        stringMessage.append(player.user.getAsMention() + " : " 
            + LanguageManager.getText(server.serv_language, "informationPanelMultipleAccountInGame") + "\n"
            + InfoPanelRefresherUtil.getCurrentGameInfoStringForMultipleAccounts(accountsInGame, server.serv_language));
      }
    }catch(SQLException e) {
      logger.error("Unexpected SQLException when threathing text", e);
    }catch(Exception e) {
      logger.error("Unexpected exception when threathing text", e);
    }finally {
      playersInWork.remove(player);
    }
  }

  private void getTextInformationPanelRankOption(final StringBuilder stringMessage, DTO.Player player,
      DTO.LeagueAccount leagueAccount, boolean mutlipleAccount) throws SQLException {
    LastRank lastRank = LastRankRepository.getLastRankWithLeagueAccountId(leagueAccount.leagueAccount_id);

    if(lastRank == null) {
      Set<LeagueEntry> leaguesEntry;
      try {
        leaguesEntry = Zoe.getRiotApi().getLeagueEntriesBySummonerIdWithRateLimit(leagueAccount.leagueAccount_server, leagueAccount.leagueAccount_summonerId);
      } catch (RiotApiException e) {
        notInGameWithoutRankInfo(stringMessage, player);
        return;
      }

      LeagueEntry soloq = null;
      LeagueEntry flex = null;

      for(LeagueEntry leaguePosition : leaguesEntry) {
        if(leaguePosition.getQueueType().equals("RANKED_SOLO_5x5")) {
          soloq = leaguePosition;
        }else if(leaguePosition.getQueueType().equals("RANKED_FLEX_SR")) {
          flex = leaguePosition;
        }
      }


      if((soloq != null || flex != null) && LastRankRepository.getLastRankWithLeagueAccountId(leagueAccount.leagueAccount_id) == null) {
        LastRankRepository.createLastRank(leagueAccount.leagueAccount_id);


        if(soloq != null) {
          LastRankRepository.updateLastRankSoloqWithLeagueAccountId(soloq, leagueAccount.leagueAccount_id);

        }

        if(flex != null) {
          LastRankRepository.updateLastRankFlexWithLeagueAccountId(flex, leagueAccount.leagueAccount_id);
        }
        lastRank = LastRankRepository.getLastRankWithLeagueAccountId(leagueAccount.leagueAccount_id);
      }

    }

    if(lastRank == null) {
      if(mutlipleAccount) {
        notInGameUnranked(stringMessage, leagueAccount);
      }else {
        notInGameWithoutRankInfo(stringMessage, player);
      }
      return;
    }

    FullTier soloqFullTier;
    FullTier flexFullTier;

    String accountString;
    String baseText;
    if(mutlipleAccount) {
      baseText = "infoPanelRankedTextMultipleAccount";
      accountString = leagueAccount.leagueAccount_name;
    }else {
      baseText = "infoPanelRankedTextOneAccount";
      accountString = player.user.getAsMention();
    }

    if(lastRank.lastRank_soloqLastRefresh != null && lastRank.lastRank_flexLastRefresh == null) {

      soloqFullTier = new FullTier(lastRank.lastRank_soloq);

      stringMessage.append(getDetailledSoloQRank(lastRank, soloqFullTier, accountString, baseText));
    }else if(lastRank.lastRank_soloqLastRefresh == null && lastRank.lastRank_flexLastRefresh != null) {

      flexFullTier = new FullTier(lastRank.lastRank_flex);

      stringMessage.append(getDetailledFlexRank(lastRank, flexFullTier, accountString, baseText));
    }else if(lastRank.lastRank_soloqLastRefresh != null && lastRank.lastRank_flexLastRefresh != null) {

      if(lastRank.lastRank_flexLastRefresh.isAfter(lastRank.lastRank_soloqLastRefresh)) {
        flexFullTier = new FullTier(lastRank.lastRank_flex);

        stringMessage.append(getDetailledFlexRank(lastRank, flexFullTier, accountString, baseText));
      }else {
        soloqFullTier = new FullTier(lastRank.lastRank_soloq);

        stringMessage.append(getDetailledSoloQRank(lastRank, soloqFullTier, accountString, baseText));
      }
    }else {
      LeagueEntry entrySoloQ = RiotRequest.getLeagueEntrySoloq(leagueAccount.leagueAccount_summonerId, leagueAccount.leagueAccount_server);

      if(entrySoloQ != null) {
        FullTier soloQTier = new FullTier(entrySoloQ);

        stringMessage.append(getDetailledSoloQRank(lastRank, soloQTier, accountString, baseText));
        LastRankRepository.updateLastRankSoloqWithLeagueAccountId(entrySoloQ, leagueAccount.leagueAccount_id);
      }else {
        if(mutlipleAccount) {
          notInGameUnranked(stringMessage, leagueAccount);
        }else {
          notInGameWithoutRankInfo(stringMessage, player);
        }
      }
    }
  }

  private String getDetailledFlexRank(LastRank lastRank, FullTier flexFullTier, String accountString, String baseText) {
    return String.format(LanguageManager.getText(server.serv_language, baseText), accountString, 
        Ressources.getTierEmote().get(flexFullTier.getTier()).getUsableEmote() + " " + flexFullTier.toString(server.serv_language),
        FullTierUtil.getTierRankTextDifference(lastRank.lastRank_soloqSecond, lastRank.lastRank_soloq, server.serv_language)
        + " / " + LanguageManager.getText(server.serv_language, "flex")) + "\n";
  }

  private String getDetailledSoloQRank(LastRank lastRank, FullTier soloqFullTier, String accountString,
      String baseText) {
    return String.format(LanguageManager.getText(server.serv_language, baseText), accountString, 
        Ressources.getTierEmote().get(soloqFullTier.getTier()).getUsableEmote() + " " + soloqFullTier.toString(server.serv_language),
        FullTierUtil.getTierRankTextDifference(lastRank.lastRank_soloqSecond, lastRank.lastRank_soloq, server.serv_language)
        + " / " + LanguageManager.getText(server.serv_language, "soloq")) + "\n";
  }

  private void notInGameWithoutRankInfo(final StringBuilder stringMessage, DTO.Player player) {
    stringMessage.append(player.user.getAsMention() + " : " 
        + LanguageManager.getText(server.serv_language, "informationPanelNotInGame") + " \n");
  }

  private void notInGameUnranked(final StringBuilder stringMessage, DTO.LeagueAccount leagueAccount) {
    stringMessage.append("- " + leagueAccount.leagueAccount_name + " : " 
        + LanguageManager.getText(server.serv_language, "unranked") + " \n");
  }

  public String getStringMessage() {
    return stringMessage.toString();
  }
  
  public static void awaitAll(List<Player> playersToWait) {
    
    boolean needToWait;
    
    do {
      needToWait = false;
      for(Player playerToWait : playersToWait) {
        if(playersInWork.contains(playerToWait)) {
          needToWait = true;
          break;
        }
      }
      
      if(needToWait) {
        try {
          TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
          logger.error("Thread as been interupt when waiting Match Worker !", e);
          Thread.currentThread().interrupt();
        }
      }
    }while(needToWait);
  }

}
