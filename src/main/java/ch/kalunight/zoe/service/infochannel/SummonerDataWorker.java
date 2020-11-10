package ch.kalunight.zoe.service.infochannel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.model.GameQueueConfigId;
import ch.kalunight.zoe.model.InfocardPlayerData;
import ch.kalunight.zoe.model.player_data.FullTier;
import ch.kalunight.zoe.model.static_data.Champion;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.NameConversion;
import ch.kalunight.zoe.util.Ressources;
import ch.kalunight.zoe.util.request.RiotRequest;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameParticipant;
import net.rithms.riot.constant.Platform;

public class SummonerDataWorker implements Runnable {
  
  private static final List<InfocardPlayerData> playersDataInWork = Collections.synchronizedList(new ArrayList<>());
  
  private static final Logger logger = LoggerFactory.getLogger(SummonerDataWorker.class);
  
  private List<String> listIdPlayers;

  private Platform platform;

  private String language;

  private CurrentGameParticipant participant;

  private InfocardPlayerData playerData;
  
  private int gameQueueConfigId;

  public SummonerDataWorker(CurrentGameParticipant participant, List<String> listIdPlayers, Platform platform, String language,
      InfocardPlayerData playerData, int gameQueueConfigId) {
    this.listIdPlayers = listIdPlayers;
    this.platform = platform;
    this.language = language;
    this.participant = participant;
    this.playerData = playerData;
    this.gameQueueConfigId = gameQueueConfigId;
    playersDataInWork.add(playerData);
  }

  @Override
  public void run() {
    try {
      logger.debug("Start loading Summoner data worker for {} {}", platform.getName(), participant.getSummonerName());
      String unknownChampion = LanguageManager.getText(language, "unknown");

      Champion champion = null;
      champion = Ressources.getChampionDataById(participant.getChampionId());
      if(champion == null) {
        champion = new Champion(-1, unknownChampion, unknownChampion, null);
      }

      FullTier fullTier;
      if(gameQueueConfigId == GameQueueConfigId.FLEX.getId()) {
        fullTier = RiotRequest.getFlexRank(participant.getSummonerId(), platform);
      }else {
        fullTier = RiotRequest.getSoloqRank(participant.getSummonerId(), platform);
      }
      
      String rank;
      try {
        rank = Ressources.getTierEmote().get(fullTier.getTier()).getEmote().getAsMention() + " " 
            + fullTier.toString(language);
      } catch(NullPointerException e) {
        rank = fullTier.toString(language);
      }

      if(listIdPlayers.contains(participant.getSummonerId())) {
        playerData.setSummonerNameData(champion.getDisplayName() + " | __**" + NameConversion.convertStringToTinyString(participant.getSummonerName())
        + "**__");
      } else {
        playerData.setSummonerNameData(champion.getDisplayName() + " | " + NameConversion.convertStringToTinyString(participant.getSummonerName()));
      }

      playerData.setRankData(rank);

      logger.debug("Start loading Winrate Summoner data worker for {} {}", platform.getName(), participant.getSummonerName());
      playerData.setWinRateData(RiotRequest.getMasterysScore(participant.getSummonerId(), participant.getChampionId(), platform) + " | "
          + RiotRequest.getWinrateLastMonthWithGivenChampion(participant.getSummonerId(), platform, participant.getChampionId(), language));
      logger.debug("End loading Summoner data worker for {} {}", platform.getName(), participant.getSummonerName());
    } catch (Exception e) {
      logger.error("Unexpected error in SummonerDataWorker !", e);
    } finally {
      playersDataInWork.remove(playerData);
    }
  }
  
  public static void awaitAll(List<InfocardPlayerData> playersDataToWait) {
    
    boolean needToWait;
    do {
      needToWait = false;
      for(InfocardPlayerData playerToWait : playersDataToWait) {
        if(playersDataInWork.contains(playerToWait)) {
          needToWait = true;
          break;
        }
      }
      
      if(needToWait) {
        try {
          TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
          logger.error("Thread as been interupt when waiting Match Worker !");
          Thread.currentThread().interrupt();
        }
      }
    }while(needToWait);
  }

}
