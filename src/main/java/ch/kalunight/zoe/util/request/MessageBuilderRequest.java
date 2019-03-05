package ch.kalunight.zoe.util.request;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import ch.kalunight.zoe.model.Player;
import ch.kalunight.zoe.util.NameConversion;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameParticipant;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;

public class MessageBuilderRequest {

  private static final DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("HH:mm");

  private MessageBuilderRequest() {}

  public static MessageEmbed createInfoCard1summoner(User user, Summoner summoner, CurrentGameInfo match, Platform region) {

    EmbedBuilder message = new EmbedBuilder();

    message.setAuthor(user.getName(), null, user.getAvatarUrl());

    message.setTitle(
        "Info sur la partie de " + user.getName() + " : " + NameConversion.convertGameQueueIdToString(match.getGameQueueConfigId()));

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

    MessageBuilderRequestUtil.createTeamData1Summoner(summoner, blueTeam, blueTeamString, blueTeamRankString, blueTeamWinRateLastMonth, region);

    message.addField("Équipe Bleu", blueTeamString.toString(), true);
    message.addField("Grades", blueTeamRankString.toString(), true);
    message.addField("Maitrise | *États d'esprit*", blueTeamWinRateLastMonth.toString(), true);

    StringBuilder redTeamString = new StringBuilder();
    StringBuilder redTeamRankString = new StringBuilder();
    StringBuilder redTeamWinrateString = new StringBuilder();

    MessageBuilderRequestUtil.createTeamData1Summoner(summoner, redTeam, redTeamString, redTeamRankString, redTeamWinrateString, region);

    message.addField("Équipe Rouge", redTeamString.toString(), true);
    message.addField("Grades", redTeamRankString.toString(), true);
    message.addField("Maitrise | *États d'esprit*", redTeamWinrateString.toString(), true);

    double minutesOfGames = (match.getGameLength() + 180.0) / 60.0;
    String[] stringMinutesSecondes = Double.toString(minutesOfGames).split("\\.");
    int minutesGameLength = Integer.parseInt(stringMinutesSecondes[0]);
    int secondesGameLength = (int) (Double.parseDouble("0." + stringMinutesSecondes[1]) * 60.0);

    String gameLenght = String.format("%02d", minutesGameLength) + ":" + String.format("%02d", secondesGameLength);

    message.setFooter("Heure de création du message : " + DateTime.now().plusHours(1).toString(dateFormatter)
        + " | Durée actuel de la partie : " + gameLenght, null);

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

    MessageBuilderRequestUtil.createTeamDataMultipleSummoner(blueTeam, listIdPlayers, blueTeamString, blueTeamRankString, blueTeamWinrateString, region);

    message.addField("Équipe Bleu", blueTeamString.toString(), true);
    message.addField("Grades", blueTeamRankString.toString(), true);
    message.addField("Maitrise | *États d'esprit*", blueTeamWinrateString.toString(), true);

    StringBuilder redTeamString = new StringBuilder();
    StringBuilder redTeamRankString = new StringBuilder();
    StringBuilder redTeamWinrateString = new StringBuilder();

    MessageBuilderRequestUtil.createTeamDataMultipleSummoner(redTeam, listIdPlayers, redTeamString, redTeamRankString, redTeamWinrateString, region);

    message.addField("Équipe Rouge", redTeamString.toString(), true);
    message.addField("Grades", redTeamRankString.toString(), true);
    message.addField("Maitrises | *États d'esprit*", redTeamWinrateString.toString(), true);

    double minutesOfGames = (currentGameInfo.getGameLength() + 180.0) / 60.0;
    String[] stringMinutesSecondes = Double.toString(minutesOfGames).split("\\.");
    int minutesGameLength = Integer.parseInt(stringMinutesSecondes[0]);
    int secondesGameLength = (int) (Double.parseDouble("0." + stringMinutesSecondes[1]) * 60.0);

    String gameLenght = String.format("%02d", minutesGameLength) + ":" + String.format("%02d", secondesGameLength);

    message.setFooter("Heure de création du message : " + DateTime.now().plusHours(1).toString(dateFormatter)
        + " | Durée actuel de la partie : " + gameLenght, null);

    message.setColor(Color.GREEN);

    return message.build();
  }
}
