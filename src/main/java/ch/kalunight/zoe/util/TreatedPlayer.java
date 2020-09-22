package ch.kalunight.zoe.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.LeagueAccount;
import ch.kalunight.zoe.model.dto.DTO.Player;
import ch.kalunight.zoe.model.player_data.FullTier;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;

public class TreatedPlayer {
  
  private Player player;
  private String infochannelMessage;
  private List<FullTier> soloqRank;
  private Map<DTO.CurrentGameInfo, LeagueAccount> gamesToDelete = Collections.synchronizedMap(new HashMap<>());
  private Map<CurrentGameInfo, LeagueAccount> gamesToCreate = Collections.synchronizedMap(new HashMap<>());
  
  public TreatedPlayer(Player player, String infochannelMessage, List<FullTier> soloqRank,
      Map<DTO.CurrentGameInfo, LeagueAccount> gamesToDelete, Map<CurrentGameInfo, LeagueAccount> gamesToCreate) {
    this.player = player;
    this.infochannelMessage = infochannelMessage;
    this.soloqRank = soloqRank;
    this.gamesToDelete = gamesToDelete;
    this.gamesToCreate = gamesToCreate;
  }

  public Player getPlayer() {
    return player;
  }

  public String getInfochannelMessage() {
    return infochannelMessage;
  }

  public List<FullTier> getSoloqRank() {
    return soloqRank;
  }

  public Map<DTO.CurrentGameInfo, LeagueAccount> getGamesToDelete() {
    return gamesToDelete;
  }

  public Map<CurrentGameInfo, LeagueAccount> getGamesToCreate() {
    return gamesToCreate;
  }
  
}
