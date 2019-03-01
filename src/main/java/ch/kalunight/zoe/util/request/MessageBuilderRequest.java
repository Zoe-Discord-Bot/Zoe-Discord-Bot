package ch.kalunight.zoe.util.request;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import ch.euclidian.main.model.Player;
import ch.euclidian.main.model.Postulation;
import ch.euclidian.main.util.MessageBuilderRequestUtil;
import ch.euclidian.main.util.NameConversion;
import ch.euclidian.main.util.Ressources;
import me.philippheuer.twitch4j.model.Channel;
import me.philippheuer.twitch4j.model.Stream;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.MessageEmbed.Field;
import net.dv8tion.jda.core.entities.User;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameParticipant;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;

public class MessageBuilderRequest {

  private static final DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("HH:mm");

  private static final Color STREAM_COLOR = Color.getHSBColor(281, 85, 84);

  private MessageBuilderRequest() {}

  public static MessageEmbed createInfoCard1summoner(User user, Summoner summoner, CurrentGameInfo match) {

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

    MessageBuilderRequestUtil.createTeamData1Summoner(summoner, blueTeam, blueTeamString, blueTeamRankString, blueTeamWinRateLastMonth);

    message.addField("Équipe Bleu", blueTeamString.toString(), true);
    message.addField("Grades", blueTeamRankString.toString(), true);
    message.addField("Maitrise | *États d'esprit*", blueTeamWinRateLastMonth.toString(), true);

    StringBuilder redTeamString = new StringBuilder();
    StringBuilder redTeamRankString = new StringBuilder();
    StringBuilder redTeamWinrateString = new StringBuilder();

    MessageBuilderRequestUtil.createTeamData1Summoner(summoner, redTeam, redTeamString, redTeamRankString, redTeamWinrateString);

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

  public static MessageEmbed createInfoCardsMultipleSummoner(List<Player> players, CurrentGameInfo currentGameInfo) {

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

    MessageBuilderRequestUtil.createTeamDataMultipleSummoner(blueTeam, listIdPlayers, blueTeamString, blueTeamRankString, blueTeamWinrateString);

    message.addField("Équipe Bleu", blueTeamString.toString(), true);
    message.addField("Grades", blueTeamRankString.toString(), true);
    message.addField("Maitrise | *États d'esprit*", blueTeamWinrateString.toString(), true);

    StringBuilder redTeamString = new StringBuilder();
    StringBuilder redTeamRankString = new StringBuilder();
    StringBuilder redTeamWinrateString = new StringBuilder();

    MessageBuilderRequestUtil.createTeamDataMultipleSummoner(redTeam, listIdPlayers, redTeamString, redTeamRankString, redTeamWinrateString);

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
  
  public static MessageEmbed createShowPostulation(Postulation postulation, int postulationNbr) {

    EmbedBuilder message = new EmbedBuilder();

    message.setAuthor(postulation.getMember().getUser().getName(), null, postulation.getMember().getUser().getAvatarUrl());

    message.setTitle("Postulation numéro " + postulationNbr + " de " + postulation.getMember().getUser().getName());

    String rank = RiotRequest.getSoloqRank(postulation.getSummoner().getId()).toString();
    Field field = new Field("**Pseudo & Rang Soloq**", postulation.getSummoner().getName() + " - " + rank, false);
    message.addField(field);

    String role = "";

    for(int i = 0; i < postulation.getRoles().size(); i++) {
      role += postulation.getRoles().get(i).getName();
      if((i + 1) != postulation.getRoles().size()) {
        role += ", ";
      }
    }

    field = new Field("**Postes**", role, false);
    message.addField(field);

    field = new Field("**Horaires**", postulation.getHoraires(), false);
    message.addField(field);

    message.setColor(Color.GREEN);

    return message.build();
  }


  public static MessageEmbed createInfoStreamMessage(Channel channel) {
    Stream actualStream = Ressources.getStreamEndpoint().getByChannel(channel);

    EmbedBuilder message = new EmbedBuilder();
    message.setAuthor(channel.getDisplayName(), null, channel.getLogo());
    message.setTitle(channel.getStatus(), "https://www.twitch.tv/batailloneuclidien");

    Field field = new Field("Jeu", channel.getGame(), true);
    message.addField(field);

    field = new Field("Viewers", Integer.toString(actualStream.getViewers()), true);
    message.addField(field);

    message.setThumbnail(channel.getLogo());

    message.setImage(actualStream.getPreview().getLarge()); // TODO: When we have a banner, add it

    message.setColor(STREAM_COLOR);

    return message.build();
  }
}
