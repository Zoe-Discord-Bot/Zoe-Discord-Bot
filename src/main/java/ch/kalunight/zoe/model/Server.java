package ch.kalunight.zoe.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.joda.time.DateTime;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.player_data.LeagueAccount;
import ch.kalunight.zoe.model.player_data.Player;
import ch.kalunight.zoe.model.player_data.Team;
import ch.kalunight.zoe.model.static_data.SpellingLanguage;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;

public class Server {

  /**
   * Default time to force refresh of channel
   */
  private static final int DEFAULT_INIT_TIME = 3;

  private final List<Long> currentGamesIdAlreadySended = new ArrayList<>();

  private ServerConfiguration config;
  private long guildId;
  private List<Player> players;
  private List<Team> teams;
  private Long infoChannelId;
  private ControlPannel controlePannel;
  private SpellingLanguage langage;
  private DateTime lastRefresh;

  public Server(long guildId, SpellingLanguage langage, ServerConfiguration configuration) {
    this.guildId = guildId;
    this.config = configuration;
    this.langage = langage;
    players = Collections.synchronizedList(new ArrayList<>());
    teams = Collections.synchronizedList(new ArrayList<>());
    controlePannel = new ControlPannel();
    lastRefresh = DateTime.now().minusMinutes(DEFAULT_INIT_TIME);
  }

  public synchronized boolean isNeedToBeRefreshed() {
    boolean needToBeRefreshed = false;
    if(lastRefresh == null || lastRefresh.isBefore(DateTime.now().minusMinutes(DEFAULT_INIT_TIME))) {
      needToBeRefreshed = true;
    }
    return needToBeRefreshed;
  }
  
  public Player isLeagueAccountAlreadyExist(LeagueAccount accountToSearch) {
    for(Player player : players) {
      for(LeagueAccount account : player.getLolAccounts()) {
        if(account.equals(accountToSearch)) {
          return player;
        }
      }
    }
    return null;
  }
  
  public Player getPlayerByLeagueAccount(LeagueAccount account) {
    for(Player player : players) {
      for(LeagueAccount potentialGoodAccount : player.getLolAccounts()) {
        if(potentialGoodAccount.getSummoner().getId().equals(account.getSummoner().getId()) 
            && potentialGoodAccount.getRegion() == account.getRegion()) {
          return player;
        }
      }
    }
    return null;
  }
  
  public List<LeagueAccount> getAllAccountsOfTheServer() {
    List<LeagueAccount> accountsOfServer = new ArrayList<>();
    for(Player player : players) {
      for(LeagueAccount account : player.getLeagueAccountsInGame()) {
        accountsOfServer.add(account);
      }
    }
    return accountsOfServer;
  }

  public List<Team> getAllPlayerTeams() {
    List<Player> playerWithNoTeam = new ArrayList<>();
    playerWithNoTeam.addAll(players);

    for(Team team : teams) {
      for(Player player : team.getPlayers()) {
        playerWithNoTeam.remove(player);
      }
    }

    List<Team> allTeams = new ArrayList<>();
    allTeams.addAll(teams);
    if(!playerWithNoTeam.isEmpty()) {
      allTeams.add(new Team("No Team", playerWithNoTeam));
    }

    return allTeams;
  }

  public void deletePlayer(Player player) {
    players.remove(player);

    for(Team team : teams) {
      team.getPlayers().remove(player);
    }
  }

  public void clearOldMatchOfSendedGamesIdList() {
    
    final List<CurrentGameInfo> allCurrentGamesOfPlayers = new ArrayList<>();
    for(Player player : players) {
      for(LeagueAccount leagueAccount : player.getLolAccounts()) {
        allCurrentGamesOfPlayers.add(leagueAccount.getCurrentGameInfo());
      }
    }
    
    final Iterator<CurrentGameInfo> iterator = allCurrentGamesOfPlayers.iterator();

    final List<Long> gameIdToSave = new ArrayList<>();

    while(iterator.hasNext()) {

      final CurrentGameInfo currentGameInfo = iterator.next();
      
      if(currentGameInfo != null) {
        gameIdToSave.add(currentGameInfo.getGameId());
      }
    }

    Iterator<Long> idCurrentGamesIterator = currentGamesIdAlreadySended.iterator();

    while(idCurrentGamesIterator.hasNext()) {
      Long actualCurrentGamesId = idCurrentGamesIterator.next();
      if(!gameIdToSave.contains(actualCurrentGamesId)) {
        idCurrentGamesIterator.remove();
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

  public Team getTeamByName(String teamName) {
    for(Team team : teams) {
      if(team.getName().equals(teamName)) {
        return team;
      }
    }
    return null;
  }

  public Team getTeamByPlayer(Player player) {
    for(Team team : teams) {
      if(team.getPlayers().contains(player)) {
        return team;
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
    return Zoe.getJda().getGuildById(guildId);
  }

  public TextChannel getInfoChannel() {
    if(infoChannelId == null) {
      return null;
    }
    return Zoe.getJda().getGuildById(guildId).getTextChannelById(infoChannelId);
  }

  public void setInfoChannel(Long infoChannelId) {
    this.infoChannelId = infoChannelId;
  }

  public SpellingLanguage getLangage() {
    return langage;
  }

  public void setLangage(SpellingLanguage langage) {
    this.langage = langage;
  }

  public synchronized DateTime getLastRefresh() {
    return lastRefresh;
  }

  public synchronized void setLastRefresh(DateTime lastRefresh) {
    this.lastRefresh = lastRefresh;
  }

  public ControlPannel getControlePannel() {
    return controlePannel;
  }

  public void setControlePannel(ControlPannel controlePannel) {
    this.controlePannel = controlePannel;
  }
  
  public List<Long> getCurrentGamesIdAlreadySended() {
    return currentGamesIdAlreadySended;
  }

  public ServerConfiguration getConfig() {
    return config;
  }

  public void setConfig(ServerConfiguration config) {
    this.config = config;
  }
}
