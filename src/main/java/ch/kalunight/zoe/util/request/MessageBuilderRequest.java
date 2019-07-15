package ch.kalunight.zoe.util.request;

import java.awt.Color;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.stats.StatsProfileCommand;
import ch.kalunight.zoe.model.player_data.FullTier;
import ch.kalunight.zoe.model.player_data.LeagueAccount;
import ch.kalunight.zoe.model.player_data.Player;
import ch.kalunight.zoe.model.player_data.Rank;
import ch.kalunight.zoe.model.player_data.Tier;
import ch.kalunight.zoe.model.static_data.Champion;
import ch.kalunight.zoe.model.static_data.CustomEmote;
import ch.kalunight.zoe.model.static_data.Mastery;
import ch.kalunight.zoe.util.NameConversion;
import ch.kalunight.zoe.util.Ressources;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.MessageEmbed.Field;
import net.dv8tion.jda.core.entities.User;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.champion_mastery.dto.ChampionMastery;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;
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

    int blueTeamID = 100;

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

    Set<LeagueAccount> playersAccountsOfTheGame = new HashSet<>();
    for(Player player : players) {
      playersAccountsOfTheGame.addAll(player.getLeagueAccountsInTheGivenGame(currentGameInfo));
    }

    EmbedBuilder message = new EmbedBuilder();

    StringBuilder title = new StringBuilder();

    MessageBuilderRequestUtil.createTitle(players, currentGameInfo, title);

    message.setTitle(title.toString());

    int blueTeamID = 100;
    ArrayList<CurrentGameParticipant> blueTeam = new ArrayList<>();
    ArrayList<CurrentGameParticipant> redTeam = new ArrayList<>();

    MessageBuilderRequestUtil.getTeamPlayer(currentGameInfo, blueTeamID, blueTeam, redTeam);

    ArrayList<String> listIdPlayers = new ArrayList<>();

    for(LeagueAccount leagueAccount : playersAccountsOfTheGame) {
      listIdPlayers.add(leagueAccount.getSummoner().getId());
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

  public static MessageEmbed createProfileMessage(Player player, LeagueAccount leagueAccount, List<ChampionMastery> masteries) throws RiotApiException {

    EmbedBuilder message = new EmbedBuilder();

    Summoner summoner;
    try {
      summoner = Zoe.getRiotApi().getSummoner(leagueAccount.getRegion(), leagueAccount.getSummoner().getId());
      leagueAccount.setSummoner(summoner);
    } catch(RiotApiException e) {
      summoner = leagueAccount.getSummoner();
      if(e.getErrorCode() == RiotApiException.RATE_LIMITED) {
        throw e;
      }
    }

    message.setTitle(player.getDiscordUser().getName() + "'s Profile (" + leagueAccount.getSummoner().getName()
        + ") : Lvl " + summoner.getSummonerLevel());

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

    boolean isRiotApiError = false;
    MatchList matchList = null;

    try {
      matchList = Zoe.getRiotApi().getMatchListByAccountId(leagueAccount.getRegion(), leagueAccount.getSummoner().getAccountId(), 
          null, null, null, DateTime.now().minusWeeks(1).plusSeconds(10).getMillis(), DateTime.now().getMillis(), -1, -1, CallPriority.HIGH);
    } catch(RiotApiException e) {
      if(e.getErrorCode() == RiotApiException.RATE_LIMITED) {
        throw e;
      }else if (e.getErrorCode() == RiotApiException.UNAVAILABLE || e.getErrorCode() == RiotApiException.SERVER_ERROR) {
        isRiotApiError = true;
      }
      logger.info("Impossible to get match history : {}", e.getMessage());
    }

    if(matchList != null) {
      List<MatchReference> matchsReference = matchList.getMatches();

      List<Match> threeMostRecentMatch = new ArrayList<>();

      if(matchsReference.size() < 3) {
        for(MatchReference matchReference : matchsReference) {
          try {
            threeMostRecentMatch.add(Zoe.getRiotApi().getMatch(leagueAccount.getRegion(), matchReference.getGameId(), CallPriority.HIGH));
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
            threeMostRecentMatch.add(Zoe.getRiotApi().getMatch(leagueAccount.getRegion(), matchReference.getGameId(), CallPriority.HIGH));
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
          Champion champion = new Champion(-1, "unknown", "unknown", null);
          try {
            champion = Ressources.getChampionDataById(match.getParticipantBySummonerId(leagueAccount.getSummoner().getId()).getChampionId());
          }catch(NullPointerException e) {
            logger.debug("Data anonymize, can't detect champion");
          }
          recentMatchsString.append(champion.getEmoteUsable() + " " + champion.getName() + " - **" + MessageBuilderRequestUtil.getPastMoment(matchTime) + "**\n");
        }
      }
      field = new Field("Lastest played games", recentMatchsString.toString(), true);
    }else {
      if(isRiotApiError) {
        field = new Field("Lastest played games", "Riot api unavailable", true);
      }else {
        field = new Field("Lastest played games", "No game played this week", true);
      }
    }

    message.addField(field);

    Set<LeagueEntry> rankPosition = null;
    try {
      rankPosition = Zoe.getRiotApi().getLeagueEntriesBySummonerId(leagueAccount.getRegion(), leagueAccount.getSummoner().getId(), CallPriority.HIGH);
    }catch (RiotApiException e) {
      if(e.getErrorCode() == RiotApiException.RATE_LIMITED) {
        throw e;
      }
      logger.info("Error with the api : ", e);
    }

    if(rankPosition != null) {

      Iterator<LeagueEntry> iteratorPosition = rankPosition.iterator();

      String soloqRank = "Soloq : **Unranked**";
      String flexRank = "Flex : **Unranked**";
      String twistedThreeLine = "3x3 : **Unranked**";

      while(iteratorPosition.hasNext()) {
        LeagueEntry leaguePosition = iteratorPosition.next();
        Tier tier = Tier.valueOf(leaguePosition.getTier());
        Rank rank = Rank.valueOf(leaguePosition.getRank());

        FullTier fullTier = new FullTier(tier, rank, leaguePosition.getLeaguePoints());

        if(leaguePosition.getQueueType().equals("RANKED_SOLO_5x5")) {
          soloqRank = "Soloq : **" + Ressources.getTierEmote().get(tier).getUsableEmote() + " " + fullTier.toString() + "**";
        } else if(leaguePosition.getQueueType().equals("RANKED_FLEX_SR")) {
          flexRank = "Flex : **" + Ressources.getTierEmote().get(tier).getUsableEmote() + " " + fullTier.toString() + "**";
        }else if(leaguePosition.getQueueType().equals("RANKED_FLEX_TT")) {
          twistedThreeLine = "3x3 : **" + Ressources.getTierEmote().get(tier).getUsableEmote() + " " + fullTier.toString() + "**";
        }
      }

      stringBuilder = new StringBuilder();

      stringBuilder.append(soloqRank + "\n");
      stringBuilder.append(flexRank + "\n");
      stringBuilder.append(twistedThreeLine);

      field = new Field("Ranked Stats", stringBuilder.toString(), true);
    }else {
      field = new Field("Ranked Stats", "Not ranked", true);
    }

    message.addField(field);

    message.setImage("attachment://" + player.getDiscordUser().getId() + ".png");

    message.setColor(new Color(206, 20, 221));

    message.setFooter(player.getDiscordUser().getName() + "'s profile", player.getDiscordUser().getAvatarUrl());
    message.setTimestamp(Instant.now());

    return message.build();
  }
}
