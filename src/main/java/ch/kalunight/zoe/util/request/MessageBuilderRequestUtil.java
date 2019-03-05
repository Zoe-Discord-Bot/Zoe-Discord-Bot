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
import net.rithms.riot.constant.Platform;

public class MessageBuilderRequestUtil {
  
  private MessageBuilderRequestUtil() {
    //Hide default public constructor
  }

  public static void createTeamData1Summoner(Summoner summoner, List<CurrentGameParticipant> teamParticipant, StringBuilder teamString,
      StringBuilder teamRankString, StringBuilder teamWinRateLastMonth, Platform platform) {

    for(int i = 0; i < teamParticipant.size(); i++) {
      CurrentGameParticipant participant = teamParticipant.get(i);
      Champion champion = null;
      champion = Ressources.getChampionDataById(participant.getChampionId());

      FullTier fullTier = RiotRequest.getSoloqRank(participant.getSummonerId(), platform);
      String rank;
      try {
        rank = Ressources.getTierEmote().get(fullTier.getTier()).getEmote().getAsMention() + " " + fullTier.toString();
      }catch(NullPointerException e) {
        rank = fullTier.toString();
      }
      
      if(summoner.getName().equals(participant.getSummonerName())) {
        teamString.append(
            champion.getDisplayName() + " | __**" + NameConversion.convertStringToTinyString(participant.getSummonerName()) + "**__" + "\n");
      } else {
        teamString
        .append(champion.getDisplayName() + " | " + NameConversion.convertStringToTinyString(participant.getSummonerName()) + "\n");
      }

      teamRankString.append(rank + "\n");

      teamWinRateLastMonth.append(RiotRequest.getMasterysScore(participant.getSummonerId(), participant.getChampionId()) + " | "
          + RiotRequest.getWinrateLateMonthWithGivenChampion(participant.getSummonerId(), platform, participant.getChampionId()) + "\n");
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
      StringBuilder teamString, StringBuilder teamRankString, StringBuilder teamWinrateString, Platform platform) {
    
    for(int i = 0; i < teamParticipant.size(); i++) {
      CurrentGameParticipant participant = teamParticipant.get(i);
      
      Champion champion = null;
      champion = Ressources.getChampionDataById(participant.getChampionId());

      FullTier fullTier = RiotRequest.getSoloqRank(participant.getSummonerId(), platform);
      String rank;
      try {
        rank = Ressources.getTierEmote().get(fullTier.getTier()).getEmote().getAsMention() + " " + fullTier.toString();
      }catch(NullPointerException e) {
        rank = fullTier.toString();
      }

      if(listIdPlayers.contains(participant.getSummonerId())) {
        teamString.append(
            champion.getDisplayName() + " | __**" + NameConversion.convertStringToTinyString(participant.getSummonerName()) + "**__" + "\n");
      } else {
        teamString
        .append(champion.getDisplayName() + " | " + NameConversion.convertStringToTinyString(participant.getSummonerName()) + "\n");
      }

      teamRankString.append(rank + "\n");
      
      teamWinrateString.append(RiotRequest.getMasterysScore(participant.getSummonerId(), participant.getChampionId()) + " | "
          + RiotRequest.getWinrateLateMonthWithGivenChampion(participant.getSummonerId(), platform, participant.getChampionId()) + "\n");
    }
  }

  public static void createTitle(List<Player> players, CurrentGameInfo currentGameInfo, StringBuilder title) {
    title.append("Info on the game of");

    for(int i = 0; i < players.size(); i++) {
      if(i + 1 == players.size()) {
        title.append(" and of " + players.get(i).getDiscordUser().getName());
      } else if(i + 2 == players.size()) {
        title.append(" " + players.get(i).getDiscordUser().getName());
      } else {
        title.append(" " + players.get(i).getDiscordUser().getName() + ",");
      }
    }

    title.append(" : " + NameConversion.convertGameQueueIdToString(currentGameInfo.getGameQueueConfigId()));
  }
}