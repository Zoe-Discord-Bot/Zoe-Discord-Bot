package ch.kalunight.zoe.util.request;

import java.awt.Color;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.stats.StatsProfileCommand;
import ch.kalunight.zoe.model.Champion;
import ch.kalunight.zoe.model.CustomEmote;
import ch.kalunight.zoe.model.Mastery;
import ch.kalunight.zoe.model.Player;
import ch.kalunight.zoe.util.NameConversion;
import ch.kalunight.zoe.util.Ressources;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.MessageEmbed.Field;
import net.dv8tion.jda.core.entities.User;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.champion_mastery.dto.ChampionMastery;
import net.rithms.riot.api.endpoints.match.dto.Match;
import net.rithms.riot.api.endpoints.match.dto.MatchList;
import net.rithms.riot.api.endpoints.match.dto.MatchReference;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameParticipant;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.CallPriority;
import net.rithms.riot.constant.Platform;

public class MessageBuilderRequest {

  private static final Logger logger = LoggerFactory.getLogger(MessageBuilderRequest.class);

  private static final String BLUE_TEAM_STRING = "Blue Team";
  private static final String RED_TEAM_STRING = "Red Team";
  private static final String MASTERIES_WR_THIS_MONTH_STRING = "Masteries | WR this month";
  private static final String SOLO_Q_RANK_STRING = "SoloQ Rank";
  private MessageBuilderRequest() {}

  public static MessageEmbed createInfoCard1summoner(User user, Summoner summoner, CurrentGameInfo match, Platform region) {

    EmbedBuilder message = new EmbedBuilder();

    message.setAuthor(user.getName(), null, user.getAvatarUrl());

    message.setTitle(
        "Info on the game of " + user.getName() + " : " + NameConversion.convertGameQueueIdToString(match.getGameQueueConfigId()));

    int blueTeamID = 0;

    for(int i = 0; i < match.getParticipants().size(); i++) {
      if(i == 0) {
        blueTeamID = match.getParticipants().get(i).getTeamId();
      }
    }

    ArrayList<CurrentGameParticipant> blueTeam = new ArrayList<>();
    ArrayList<CurrentGameParticipant> redTeam = new ArrayList<>();

    MessageBuilderRequestUtil.getTeamPlayer(match, blueTeamID, blueTeam, redTeam);

    StringBuilder blueTeamString = new StringBuilder();
    StringBuilder blueTeamRankString = new StringBuilder();
    StringBuilder blueTeamWinRateLastMonth = new StringBuilder();

    MessageBuilderRequestUtil.createTeamData1Summoner(summoner, blueTeam, blueTeamString, blueTeamRankString, blueTeamWinRateLastMonth,
        region);

    message.addField(BLUE_TEAM_STRING, blueTeamString.toString(), true);
    message.addField(SOLO_Q_RANK_STRING, blueTeamRankString.toString(), true);
    message.addField(MASTERIES_WR_THIS_MONTH_STRING, blueTeamWinRateLastMonth.toString(), true);

    StringBuilder redTeamString = new StringBuilder();
    StringBuilder redTeamRankString = new StringBuilder();
    StringBuilder redTeamWinrateString = new StringBuilder();

    MessageBuilderRequestUtil.createTeamData1Summoner(summoner, redTeam, redTeamString, redTeamRankString, redTeamWinrateString, region);

    message.addField(RED_TEAM_STRING, redTeamString.toString(), true);
    message.addField(SOLO_Q_RANK_STRING, redTeamRankString.toString(), true);
    message.addField(MASTERIES_WR_THIS_MONTH_STRING, redTeamWinrateString.toString(), true);

    double minutesOfGames = 0.0;

    if(match.getGameLength() != 0l) {
      minutesOfGames = match.getGameLength() + 180.0;
    }

    minutesOfGames = minutesOfGames / 60.0;
    String[] stringMinutesSecondes = Double.toString(minutesOfGames).split("\\.");
    int minutesGameLength = Integer.parseInt(stringMinutesSecondes[0]);
    int secondesGameLength = (int) (Double.parseDouble("0." + stringMinutesSecondes[1]) * 60.0);

    String gameLenght = String.format("%02d", minutesGameLength) + ":" + String.format("%02d", secondesGameLength);

    message.setFooter("Current duration of the game : " + gameLenght, null);

    message.setColor(Color.GREEN);

    return message.build();
  }

  public static MessageEmbed createInfoCardsMultipleSummoner(List<Player> players, CurrentGameInfo currentGameInfo, Platform region) {

    EmbedBuilder message = new EmbedBuilder();

    StringBuilder title = new StringBuilder();

    MessageBuilderRequestUtil.createTitle(players, currentGameInfo, title);

    message.setTitle(title.toString());

    int blueTeamID = 0;

    for(int i = 0; i < currentGameInfo.getParticipants().size(); i++) {
      if(i == 0) {
        blueTeamID = currentGameInfo.getParticipants().get(i).getTeamId();
      }
    }

    ArrayList<CurrentGameParticipant> blueTeam = new ArrayList<>();
    ArrayList<CurrentGameParticipant> redTeam = new ArrayList<>();

    MessageBuilderRequestUtil.getTeamPlayer(currentGameInfo, blueTeamID, blueTeam, redTeam);

    ArrayList<String> listIdPlayers = new ArrayList<>();

    for(int i = 0; i < players.size(); i++) {
      listIdPlayers.add(players.get(i).getSummoner().getId());
    }

    StringBuilder blueTeamString = new StringBuilder();
    StringBuilder blueTeamRankString = new StringBuilder();
    StringBuilder blueTeamWinrateString = new StringBuilder();

    MessageBuilderRequestUtil.createTeamDataMultipleSummoner(blueTeam, listIdPlayers, blueTeamString, blueTeamRankString,
        blueTeamWinrateString, region);

    message.addField(BLUE_TEAM_STRING, blueTeamString.toString(), true);
    message.addField(SOLO_Q_RANK_STRING, blueTeamRankString.toString(), true);
    message.addField(MASTERIES_WR_THIS_MONTH_STRING, blueTeamWinrateString.toString(), true);

    StringBuilder redTeamString = new StringBuilder();
    StringBuilder redTeamRankString = new StringBuilder();
    StringBuilder redTeamWinrateString = new StringBuilder();

    MessageBuilderRequestUtil.createTeamDataMultipleSummoner(redTeam, listIdPlayers, redTeamString, redTeamRankString, redTeamWinrateString,
        region);

    message.addField(RED_TEAM_STRING, redTeamString.toString(), true);
    message.addField(SOLO_Q_RANK_STRING, redTeamRankString.toString(), true);
    message.addField(MASTERIES_WR_THIS_MONTH_STRING, redTeamWinrateString.toString(), true);

    double minutesOfGames = 0.0;

    if(currentGameInfo.getGameLength() != 0l) {
      minutesOfGames = currentGameInfo.getGameLength() + 180.0;
    }

    minutesOfGames = minutesOfGames / 60.0;
    String[] stringMinutesSecondes = Double.toString(minutesOfGames).split("\\.");
    int minutesGameLength = Integer.parseInt(stringMinutesSecondes[0]);
    int secondesGameLength = (int) (Double.parseDouble("0." + stringMinutesSecondes[1]) * 60.0);

    String gameLenght = String.format("%02d", minutesGameLength) + ":" + String.format("%02d", secondesGameLength);

    message.setFooter("Current duration of the game : " + gameLenght, null);

    message.setColor(Color.GREEN);

    return message.build();
  }

  public static MessageEmbed createProfileMessage(Player player, List<ChampionMastery> masteries) throws RiotApiException {

    EmbedBuilder message = new EmbedBuilder();

    message.setAuthor(player.getDiscordUser().getName(), null, player.getDiscordUser().getAvatarUrl());

    Summoner summoner;
    try {
      summoner = Zoe.getRiotApi().getSummoner(player.getRegion(), player.getSummoner().getId());
      player.setSummoner(summoner);
    } catch(RiotApiException e) {
      summoner = player.getSummoner();
      if(e.getErrorCode() == RiotApiException.RATE_LIMITED) {
        throw e;
      }
    }

    message.setTitle(player.getDiscordUser().getName() + " Profile" + " : Lvl " + summoner.getSummonerLevel());

    List<ChampionMastery> threeBestchampionMasteries = StatsProfileCommand.getBestMasteries(masteries, 3);

    StringBuilder stringBuilder = new StringBuilder();

    for(ChampionMastery championMastery : threeBestchampionMasteries) {
      Champion champion = Ressources.getChampionDataById(championMastery.getChampionId());
      stringBuilder.append(champion.getDisplayName() + " " + champion.getName() + " - **" 
          + MessageBuilderRequestUtil.getMasteryUnit(championMastery.getChampionPoints()) +"**\n");
    }

    Field field = new Field("Top Champions", stringBuilder.toString(), true);
    message.addField(field);

    int nbrMastery7 = 0;
    int nbrMastery6 = 0;
    int nbrMastery5 = 0;
    long totalNbrMasteries = 0;

    for(ChampionMastery championMastery : masteries) {
      switch(championMastery.getChampionLevel()) {
        case 5: nbrMastery5++; break;
        case 6: nbrMastery6++; break;
        case 7: nbrMastery7++; break;
        default: break;
      }
      totalNbrMasteries += championMastery.getChampionPoints();
    }

    double moyennePoints = (double) totalNbrMasteries / masteries.size();

    CustomEmote masteryEmote7 = Ressources.getMasteryEmote().get(Mastery.getEnum(7));
    CustomEmote masteryEmote6 = Ressources.getMasteryEmote().get(Mastery.getEnum(6));
    CustomEmote masteryEmote5 = Ressources.getMasteryEmote().get(Mastery.getEnum(5));

    field = new Field("Mastery Stats",
        nbrMastery7 + "x" + masteryEmote7.getUsableEmote() + " "
            + nbrMastery6 + "x" + masteryEmote6.getUsableEmote() + " "
            + nbrMastery5 + "x" + masteryEmote5.getUsableEmote() + "\n"
            + MessageBuilderRequestUtil.getMasteryUnit(totalNbrMasteries) + " **Total Points**\n"
            + MessageBuilderRequestUtil.getMasteryUnit((long) moyennePoints) + " **Average/Champ**", true);


    message.addField(field);

    MatchList matchList = null;

    try {
      matchList = Zoe.getRiotApi().getMatchListByAccountId(player.getRegion(), player.getSummoner().getAccountId(), 
          null, null, null, DateTime.now().minusWeeks(1).getMillis(), DateTime.now().getMillis(), -1, -1, CallPriority.HIGH);
    } catch(RiotApiException e) {
      if(e.getErrorCode() == RiotApiException.RATE_LIMITED) {
        throw e;
      }
      logger.info("Impossible to get match history : {}", e);
    }
    
    if(matchList != null) {
      List<MatchReference> matchsReference = matchList.getMatches();

      List<Match> threeMostRecentMatch = new ArrayList<>();

      if(matchsReference.size() < 3) {
        for(MatchReference matchReference : matchsReference) {
          try {
            threeMostRecentMatch.add(Zoe.getRiotApi().getMatch(player.getRegion(), matchReference.getGameId(), CallPriority.HIGH));
          } catch(RiotApiException e) {
            if(e.getErrorCode() == RiotApiException.RATE_LIMITED) {
              throw e;
            }
            logger.info("Riot api got an error : {}", e);
          }
        }
      }else {
        for(int i = 0; i < 3; i++) {
          MatchReference matchReference = matchsReference.get(i);

          try {
            threeMostRecentMatch.add(Zoe.getRiotApi().getMatch(player.getRegion(), matchReference.getGameId(), CallPriority.HIGH));
          } catch(RiotApiException e) {
            if(e.getErrorCode() == RiotApiException.RATE_LIMITED) {
              throw e;
            }
            logger.info("Riot api got an error : {}", e);
          }
        }
      }

      StringBuilder recentMatchsString = new StringBuilder();

      if(!threeMostRecentMatch.isEmpty()) {
        for(Match match : threeMostRecentMatch) {
          LocalDateTime matchTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(match.getGameCreation()), ZoneId.ofOffset("UTC", ZoneOffset.UTC));
          Champion champion = Ressources.getChampionDataById(match.getParticipantByAccountId(player.getSummoner().getAccountId()).getChampionId());
          recentMatchsString.append(champion.getEmoteUsable() + " " + champion.getName() + " - **" + MessageBuilderRequestUtil.getPastMoment(matchTime) + "**\n");
        }
      }
      field = new Field("Lastest played games", recentMatchsString.toString(), false);
    }else {
      field = new Field("Lastest played games", "No game played this week", false);
    }

    message.addField(field);
    
    message.setImage("attachment://" + player.getDiscordUser().getName() + ".png");

    message.setColor(new Color(206, 20, 221));

    return message.build();
  }
}
