package ch.kalunight.zoe.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ch.kalunight.zoe.exception.NoValueRankException;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Player;
import ch.kalunight.zoe.model.player_data.FullTier;
import ch.kalunight.zoe.model.player_data.Rank;
import ch.kalunight.zoe.model.player_data.Tier;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;

public class TreatedPlayer {
  
  private Player player;
  private DTO.Team team;
  private String infochannelMessage;
  private List<FullTier> soloqRanks;
  private Map<DTO.CurrentGameInfo, LeagueAccount> gamesToDelete = Collections.synchronizedMap(new HashMap<>());
  private Map<CurrentGameInfo, LeagueAccount> gamesToCreate = Collections.synchronizedMap(new HashMap<>());
  
  public TreatedPlayer(Player player, DTO.Team team, String infochannelMessage, List<FullTier> soloqRank,
      Map<DTO.CurrentGameInfo, LeagueAccount> gamesToDelete, Map<CurrentGameInfo, LeagueAccount> gamesToCreate) {
    this.player = player;
    this.team = team;
    this.infochannelMessage = infochannelMessage;
    this.soloqRanks = soloqRank;
    this.gamesToDelete = gamesToDelete;
    this.gamesToCreate = gamesToCreate;
  }

  public Player getPlayer() {
    return player;
  }

  public String getInfochannelMessage() {
    return infochannelMessage;
  }

  public List<FullTier> getSoloqRanks() {
    return soloqRanks;
  }
  
  public FullTier getAverageSoloQRank() {
    int totValue = 0;
    int nbrOfRank = 0;
    
    for(FullTier soloQRank : soloqRanks) {
      try {
        totValue += soloQRank.value();
        nbrOfRank++;
      } catch(NoValueRankException e) {
        //nothing to do
      }
    }
    
    if(totValue == 0) {
      return new FullTier(Tier.UNRANKED, Rank.UNRANKED, 0);
    }
    
    return new FullTier(totValue / nbrOfRank);
  }

  public Map<DTO.CurrentGameInfo, LeagueAccount> getGamesToDelete() {
    return gamesToDelete;
  }

  public Map<CurrentGameInfo, LeagueAccount> getGamesToCreate() {
    return gamesToCreate;
  }

  public DTO.Team getTeam() {
    return team;
  }
  
}
