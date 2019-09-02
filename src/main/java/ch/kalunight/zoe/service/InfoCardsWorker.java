package ch.kalunight.zoe.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.kalunight.zoe.model.InfoCard;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.player_data.LeagueAccount;
import ch.kalunight.zoe.model.player_data.Player;
import ch.kalunight.zoe.util.InfoPanelRefresherUtil;
import ch.kalunight.zoe.util.NameConversion;
import ch.kalunight.zoe.util.request.MessageBuilderRequest;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.constant.CallPriority;

public class InfoCardsWorker implements Runnable {

  private Server server;
  
  private Player registedPlayer;
  
  public InfoCardsWorker(Player player, Server server) {
    this.server = server;
    this.registedPlayer = player;
  }
  
  
  @Override
  public void run() {
    
    try {
      server.wait(5000);
    } catch (InterruptedException e) { //TODO: Make it more cleaner ?
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    registedPlayer.refreshAllLeagueAccounts(CallPriority.NORMAL);
    
    ArrayList<InfoCard> cards = new ArrayList<>();
    for(LeagueAccount leagueAccount : registedPlayer.getLeagueAccountsInGame()) {
        
      CurrentGameInfo gameInfo = leagueAccount.getCurrentGameInfo();
      
      server.getCurrentGamesIdAlreadySended().add(gameInfo.getGameId());
      
      List<Player> playersInTheGame = InfoPanelRefresherUtil.checkIfOthersPlayersIsKnowInTheMatch(gameInfo, server);
      
      InfoCard card = null;
      
      if(playersInTheGame.size() == 1) {
        Player player = playersInTheGame.get(0);
        MessageEmbed messageCard = MessageBuilderRequest.createInfoCard1summoner(player.getDiscordUser(), leagueAccount.getSummoner(),
            gameInfo, leagueAccount.getRegion());
        if(messageCard != null) {
          card = new InfoCard(playersInTheGame, messageCard, gameInfo);
        }
      } else if(playersInTheGame.size() > 1) {
        MessageEmbed messageCard =
            MessageBuilderRequest.createInfoCardsMultipleSummoner(playersInTheGame, gameInfo, leagueAccount.getRegion());

        if(messageCard != null) {
          card = new InfoCard(playersInTheGame, messageCard, gameInfo);
        }
      }
      
      if(card != null) {
        List<Player> players = card.getPlayers();

        StringBuilder title = new StringBuilder();
        title.append("Info on the game of");

        List<String> playersName = NameConversion.getListNameOfPlayers(players);

        Set<Player> cardPlayersNotTwice = new HashSet<>();
        for(Player playerToCheck : card.getPlayers()) {
          cardPlayersNotTwice.add(playerToCheck);
        }

        for(int j = 0; j < cardPlayersNotTwice.size(); j++) {
          if(j == 0) {
            title.append(" " + playersName.get(j));
          } else if(j + 1 == playersName.size()) {
            title.append(" and of " + playersName.get(j));
          } else if(j + 2 == playersName.size()) {
            title.append(" " + playersName.get(j));
          } else {
            title.append(" " + playersName.get(j) + ",");
          }
        }

        card.setTitle(server.getInfoChannel().sendMessage(title.toString()).complete());
        card.setMessage(server.getInfoChannel().sendMessage(card.getCard()).complete());

        cards.add(card);
      }
    }
    
    server.getControlePannel().getInfoCards().addAll(cards);
  }

}
