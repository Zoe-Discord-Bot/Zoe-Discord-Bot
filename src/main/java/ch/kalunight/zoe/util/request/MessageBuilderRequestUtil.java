package ch.kalunight.zoe.util.request;

import java.util.List;

import ch.kalunight.zoe.model.Champion;
import ch.kalunight.zoe.model.FullTier;
import ch.kalunight.zoe.model.Player;
import ch.kalunight.zoe.util.NameConversion;
import ch.kalunight.zoe.util.Ressources;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameParticipant;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;

public class MessageBuilderRequestUtil {
  
  private MessageBuilderRequestUtil() {
    //Hide default public constructor
  }

  public static void createTeamData1Summoner(Summoner summoner, List<CurrentGameParticipant> teamParticipant, StringBuilder teamString,
      StringBuilder teamRankString, StringBuilder teamWinRateLastMonth) {

    for(int i = 0; i < teamParticipant.size(); i++) {
      Champion champion = null;
      champion = Ressources.getChampionDataById(teamParticipant.get(i).getChampionId());

      FullTier fullTier = RiotRequest.getSoloqRank(teamParticipant.get(i).getSummonerId());
      String rank;
      try {
        rank = Ressources.getTierEmote().get(fullTier.getTier()).getEmote().getAsMention() + " " + fullTier.toString();
      }catch(NullPointerException e) {
        rank = fullTier.toString();
      }
      
      if(summoner.getName().equals(teamParticipant.get(i).getSummonerName())) {
        teamString.append(
            champion.getDisplayName() + " | __**" + NameConversion.convertStringToTinyString(teamParticipant.get(i).getSummonerName()) + "**__" + "\n");
      } else {
        teamString
        .append(champion.getDisplayName() + " | " + NameConversion.convertStringToTinyString(teamParticipant.get(i).getSummonerName()) + "\n");
      }

      teamRankString.append(rank + "\n");

      teamWinRateLastMonth.append(RiotRequest.getMasterysScore(teamParticipant.get(i).getSummonerId(), teamParticipant.get(i).getChampionId()) + " | "
          + RiotRequest.getMood(teamParticipant.get(i).getSummonerId()) + "\n"); //TODO : Update to getWinrateLastMonthWitchOneChampion()
    }
  }

  public static void getTeamPlayer(CurrentGameInfo match, int teamID, List<CurrentGameParticipant> teamParticipant,
      List<CurrentGameParticipant> redTeam) {
    for(int i = 0; i < match.getParticipants().size(); i++) {
      if(match.getParticipants().get(i).getTeamId() == teamID) {
        teamParticipant.add(match.getParticipants().get(i));
      } else {
        redTeam.add(match.getParticipants().get(i));
      }
    }
  }


  public static void createTeamDataMultipleSummoner(List<CurrentGameParticipant> teamParticipant, List<String> listIdPlayers,
      StringBuilder teamString, StringBuilder teamRankString, StringBuilder teamWinrateString) {
    for(int i = 0; i < teamParticipant.size(); i++) {
      Champion champion = null;
      champion = Ressources.getChampionDataById(teamParticipant.get(i).getChampionId());

      FullTier fullTier = RiotRequest.getSoloqRank(teamParticipant.get(i).getSummonerId());
      String rank;
      try {
        rank = Ressources.getTierEmote().get(fullTier.getTier()).getEmote().getAsMention() + " " + fullTier.toString();
      }catch(NullPointerException e) {
        rank = fullTier.toString();
      }

      if(listIdPlayers.contains(teamParticipant.get(i).getSummonerId())) {
        teamString.append(
            champion.getDisplayName() + " | __**" + NameConversion.convertStringToTinyString(teamParticipant.get(i).getSummonerName()) + "**__" + "\n");
      } else {
        teamString
        .append(champion.getDisplayName() + " | " + NameConversion.convertStringToTinyString(teamParticipant.get(i).getSummonerName()) + "\n");
      }

      teamRankString.append(rank + "\n");

      teamWinrateString.append(RiotRequest.getMasterysScore(teamParticipant.get(i).getSummonerId(), teamParticipant.get(i).getChampionId()) + " | "
          + RiotRequest.getMood(teamParticipant.get(i).getSummonerId()) + "\n"); //TODO : Update to getWinrateLastMonthWitchOneChampion()
    }
  }

  public static void createTitle(List<Player> players, CurrentGameInfo currentGameInfo, StringBuilder title) {
    title.append("Info sur la partie de");

    for(int i = 0; i < players.size(); i++) {
      if(i + 1 == players.size()) {
        title.append(" et de " + players.get(i).getDiscordUser().getName());
      } else if(i + 2 == players.size()) {
        title.append(" " + players.get(i).getDiscordUser().getName());
      } else {
        title.append(" " + players.get(i).getDiscordUser().getName() + ",");
      }
    }

    title.append(" : " + NameConversion.convertGameQueueIdToString(currentGameInfo.getGameQueueConfigId()));
  }
}