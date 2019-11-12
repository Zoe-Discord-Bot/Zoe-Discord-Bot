package ch.kalunight.zoe.util.request;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import ch.kalunight.zoe.model.player_data.FullTier;
import ch.kalunight.zoe.model.player_data.Player;
import ch.kalunight.zoe.model.static_data.Champion;
import ch.kalunight.zoe.model.static_data.SpellingLanguage;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.NameConversion;
import ch.kalunight.zoe.util.Ressources;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameParticipant;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.CallPriority;
import net.rithms.riot.constant.Platform;

public class MessageBuilderRequestUtil {

  private static final DecimalFormat df = new DecimalFormat("#.##");
  
  private MessageBuilderRequestUtil() {
    // Hide default public constructor
  }
  
  public static void createTeamData1Summoner(Summoner summoner, List<CurrentGameParticipant> teamParticipant, StringBuilder teamString,
      StringBuilder teamRankString, StringBuilder teamWinRateLastMonth, Platform platform, SpellingLanguage language) {

    for(int i = 0; i < teamParticipant.size(); i++) {
      CurrentGameParticipant participant = teamParticipant.get(i);
      Champion champion = null;
      champion = Ressources.getChampionDataById(participant.getChampionId());
      if(champion == null) {
        champion = new Champion(-1, "-1", LanguageManager.getText(language, "unknown"), null);
      }

      FullTier fullTier = RiotRequest.getSoloqRank(participant.getSummonerId(), platform, CallPriority.NORMAL);
      String rank;
      try {
        rank = Ressources.getTierEmote().get(fullTier.getTier()).getEmote().getAsMention() + " " + fullTier.toString();
      } catch(NullPointerException e) {
        rank = fullTier.toString();
      }

      if(summoner.getName().equals(participant.getSummonerName())) {
        teamString.append(champion.getDisplayName() + " | __**" + NameConversion.convertStringToTinyString(participant.getSummonerName())
        + "**__" + "\n");
      } else {
        teamString
        .append(champion.getDisplayName() + " | " + NameConversion.convertStringToTinyString(participant.getSummonerName()) + "\n");
      }

      teamRankString.append(rank + "\n");

      teamWinRateLastMonth.append(RiotRequest.getMasterysScore(participant.getSummonerId(), participant.getChampionId(), platform) + " | "
          + RiotRequest.getWinrateLastMonthWithGivenChampion(participant.getSummonerId(), platform, participant.getChampionId()) + "\n");
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
      StringBuilder teamString, StringBuilder teamRankString, StringBuilder teamWinrateString, Platform platform,
      SpellingLanguage language) {

    String unknownChampion = LanguageManager.getText(language, "unknown");
    
    for(int i = 0; i < teamParticipant.size(); i++) {
      CurrentGameParticipant participant = teamParticipant.get(i);

      Champion champion = null;
      champion = Ressources.getChampionDataById(participant.getChampionId());
      if(champion == null) {
        champion = new Champion(-1, unknownChampion, unknownChampion, null);
      }

      FullTier fullTier = RiotRequest.getSoloqRank(participant.getSummonerId(), platform, CallPriority.NORMAL);
      String rank;
      try {
        rank = Ressources.getTierEmote().get(fullTier.getTier()).getEmote().getAsMention() + " " + fullTier.toString();
      } catch(NullPointerException e) {
        rank = fullTier.toString();
      }

      if(listIdPlayers.contains(participant.getSummonerId())) {
        teamString.append(champion.getDisplayName() + " | __**" + NameConversion.convertStringToTinyString(participant.getSummonerName())
        + "**__" + "\n");
      } else {
        teamString
        .append(champion.getDisplayName() + " | " + NameConversion.convertStringToTinyString(participant.getSummonerName()) + "\n");
      }

      teamRankString.append(rank + "\n");

      teamWinrateString.append(RiotRequest.getMasterysScore(participant.getSummonerId(), participant.getChampionId(), platform) + " | "
          + RiotRequest.getWinrateLastMonthWithGivenChampion(participant.getSummonerId(), platform, participant.getChampionId()) + "\n");
    }
  }

  public static void createTitle(List<Player> players, CurrentGameInfo currentGameInfo, StringBuilder title, SpellingLanguage language) {
    ArrayList<Player> playersNotTwice = new ArrayList<>();
    
    for(Player player : players) {
      if(!playersNotTwice.contains(player)) {
        playersNotTwice.add(player);
      }
    }
    
    title.append(LanguageManager.getText(language, "infoCardsGameInfoOnTheGameOf"));

    String andOfTranslated = LanguageManager.getText(language, "infoCardsGameInfoAndOf");
    
    for(int i = 0; i < playersNotTwice.size(); i++) {
      if(i == 0) {
        title.append(" " + playersNotTwice.get(i).getDiscordUser().getName());
      } else if(i + 1 == playersNotTwice.size()) {
        title.append(" " + andOfTranslated + " " + playersNotTwice.get(i).getDiscordUser().getName());
      } else if(i + 2 == playersNotTwice.size()) {
        title.append(" " + playersNotTwice.get(i).getDiscordUser().getName());
      } else {
        title.append(" " + playersNotTwice.get(i).getDiscordUser().getName() + ",");
      }
    }

    title.append(" : " + LanguageManager.getText(language, NameConversion.convertGameQueueIdToString(currentGameInfo.getGameQueueConfigId())));
  }

  public static String getMasteryUnit(Long masteryPoints) {
    if(masteryPoints > 1000 && masteryPoints < 1000000) {
      return masteryPoints / 1000 + "k";
    } else if(masteryPoints > 1000000) {
      return df.format((double) masteryPoints / 1000000) + "m";
    }
    return masteryPoints.toString();
  }
  
  //TODO: Improve this method, make it automated adaptable
  public static String getPastMoment(LocalDateTime pastMoment, SpellingLanguage language) {
    LocalDateTime now = LocalDateTime.now();
    if(pastMoment.isBefore(now.minusWeeks(1))) {
      return LanguageManager.getText(language, "aWeekAgo");
    }else if(pastMoment.isBefore(now.minusDays(6))) {
      return LanguageManager.getText(language, "6DaysAgo");
    }else if(pastMoment.isBefore(now.minusDays(5))) {
      return LanguageManager.getText(language, "5DaysAgo");
    }else if(pastMoment.isBefore(now.minusDays(4))) {
      return LanguageManager.getText(language, "4DaysAgo");
    }else if(pastMoment.isBefore(now.minusDays(3))) {
      return LanguageManager.getText(language, "3DaysAgo");
    }else if(pastMoment.isBefore(now.minusDays(2))) {
      return LanguageManager.getText(language, "2DaysAgo");
    }else if(pastMoment.isBefore(now.minusDays(1))) {
      return LanguageManager.getText(language, "yesterday");
    }else if(pastMoment.isBefore(now.minusHours(6))) {
      return LanguageManager.getText(language, "Today");
    }else if(pastMoment.isBefore(now.minusHours(5))) {
      return LanguageManager.getText(language, "5HoursAgo");
    }else if(pastMoment.isBefore(now.minusHours(4))) {
      return LanguageManager.getText(language, "4HoursAgo");
    }else if(pastMoment.isBefore(now.minusHours(3))) {
      return LanguageManager.getText(language, "3HoursAgo");
    }else if(pastMoment.isBefore(now.minusHours(2))) {
      return LanguageManager.getText(language, "2HoursAgo");
    }else if(pastMoment.isBefore(now.minusHours(1))) {
      return LanguageManager.getText(language, "1HourAgo");
    }else if(pastMoment.isBefore(now.minusMinutes(30))) {
      return LanguageManager.getText(language, "30MinutesAgo");
    }else if(pastMoment.isBefore(now.minusMinutes(10))) {
      return LanguageManager.getText(language, "fewsMinutesAgo");
    }else {
      return LanguageManager.getText(language, "unknown");
    }
  }
}
