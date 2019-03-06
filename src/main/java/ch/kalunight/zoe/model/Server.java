package ch.kalunight.zoe.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.joda.time.DateTime;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;

public class Server {

  private final Map<String, CurrentGameInfo> currentGames = new HashMap<>();
  private final List<Long> currentGamesIdAlreadySended = new ArrayList<>();
  
  private Guild guild;
  private List<Player> players;
  private List<Team> teams;
  private TextChannel infoChannel;
  private ControlPannel controlePannel;
  private SpellingLangage langage; //Not implement yet
  private DateTime lastRefresh;
  
  public Server(Guild guild, SpellingLangage langage) {
    this.guild = guild;
    this.langage = langage;
    players = new ArrayList<>();
    teams = new ArrayList<>();
    controlePannel = new ControlPannel();
    lastRefresh = DateTime.now();
  }
  
  public synchronized boolean isNeedToBeRefreshed() {
    boolean needToBeRefreshed = false;
    if(lastRefresh == null || lastRefresh.isBefore(DateTime.now().minusMinutes(3))) {
      needToBeRefreshed = true;
      lastRefresh = DateTime.now();
    }
    return needToBeRefreshed;
  }
  
  public List<Team> getAllPlayerTeams(){
    List<Player> playerWithNoTeam = new ArrayList<>();
    playerWithNoTeam.addAll(players);
    
    for(Team team : teams) {
      for(Player player : team.getPlayers()) {
        playerWithNoTeam.remove(player);
      }
    }
    
    List<Team> allTeams = new ArrayList<>();
    allTeams.addAll(teams);
    allTeams.add(new Team("No Team", playerWithNoTeam));
    
    return allTeams;
  }
  
  public void deletePlayer(Player player) {
    players.remove(player);
    
    for(Team team : teams) {
      team.getPlayers().remove(player);
    }
  }

  public void clearOldMatchOfSendedGamesIdList() {
    final Iterator<Entry<String, CurrentGameInfo>> iterator = currentGames.entrySet().iterator();
    
    final List<Long> gameIdToSave = new ArrayList<>();
    
    while(iterator.hasNext()) {
      final Entry<String, CurrentGameInfo> currentGameInfoEntry = iterator.next();
      
      final CurrentGameInfo currentGameInfo = currentGameInfoEntry.getValue();
      
      if(currentGameInfo != null) {
        gameIdToSave.add(currentGameInfo.getGameId());
      }
    }
    
    for(Long idCurrentGamesSaved : currentGamesIdAlreadySended) {
      if(!gameIdToSave.contains(idCurrentGamesSaved)) {
        currentGamesIdAlreadySended.remove(idCurrentGamesSaved);
      }
    }
  }
  
  public Player getPlayerByDiscordId(String discordId) {
    for(Player player : players) {
      if(player.getDiscordUser().getId().equals(discordId)) {
        return player;
      }
    }
    return null;
  }

  public List<Player> getPlayers() {
    return players;
  }

  public void setPlayers(List<Player> players) {
    this.players = players;
  }

  public List<Team> getTeams() {
    return teams;
  }

  public void setTeams(List<Team> teams) {
    this.teams = teams;
  }

  public Guild getGuild() {
    return guild;
  }

  public TextChannel getInfoChannel() {
    return infoChannel;
  }

  public void setInfoChannel(TextChannel infoChannel) {
    this.infoChannel = infoChannel;
  }

  public SpellingLangage getLangage() {
    return langage;
  }

  public void setLangage(SpellingLangage langage) {
    this.langage = langage;
  }

  public DateTime getLastRefresh() {
    return lastRefresh;
  }

  public void setLastRefresh(DateTime lastRefresh) {
    this.lastRefresh = lastRefresh;
  }

  public ControlPannel getControlePannel() {
    return controlePannel;
  }

  public void setControlePannel(ControlPannel controlePannel) {
    this.controlePannel = controlePannel;
  }

  public Map<String, CurrentGameInfo> getCurrentGames() {
    return currentGames;
  }

  public List<Long> getCurrentGamesIdAlreadySended() {
    return currentGamesIdAlreadySended;
  }
}
