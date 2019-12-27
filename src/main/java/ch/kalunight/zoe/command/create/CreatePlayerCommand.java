package ch.kalunight.zoe.command.create;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.config.option.RegionOption;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import ch.kalunight.zoe.util.RiotApiUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;

public class CreatePlayerCommand extends ZoeCommand {

  public static final String USAGE_NAME = "player";

  private static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\(([^)]+)\\)");

  public CreatePlayerCommand() {
    this.name = USAGE_NAME;
    this.help = "createPlayerHelpMessage";
    this.arguments = "@DiscordMentionOfPlayer (Region) (SummonerName)";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(CreateCommand.USAGE_NAME, name, arguments, help);
  }

  @Override
  public void executeCommand(CommandEvent event) throws SQLException {
    event.getTextChannel().sendTyping().complete();
    
    DTO.Server server = getServer(event.getGuild().getIdLong());
    
    ServerConfiguration config = ConfigRepository.getServerConfiguration(server.serv_guildId);
    
    if(!config.getUserSelfAdding().isOptionActivated() && !event.getMember().getPermissions().contains(Permission.MANAGE_CHANNEL)) {
      event.reply(String.format(LanguageManager.getText(server.serv_language, "permissionNeededMessage"),
          Permission.MANAGE_CHANNEL.getName()));
        return;
    }
    
    User user = getMentionedUser(event.getMessage().getMentionedMembers());
    if(user == null) {
      event.reply(String.format(LanguageManager.getText(server.serv_language, "mentionNeededMessageWithUser"), event.getAuthor().getName()));
      return;
    }else if(!user.equals(event.getAuthor()) && !event.getMember().getPermissions().contains(Permission.MANAGE_CHANNEL)) {
      event.reply(String.format(LanguageManager.getText(server.serv_language, "permissionNeededCreateOtherPlayer"),
          Permission.MANAGE_CHANNEL.getName()));
      return;
    }

    if(isTheGivenUserAlreadyRegister(user, server)) {
      event.reply(LanguageManager.getText(server.serv_language, "createPlayerAlreadyRegistered"));
      return;
    }
    
    RegionOption regionOption = config.getDefaultRegion();

    List<String> listArgs = CreatePlayerCommand.getParameterInParenteses(event.getArgs());
    if(listArgs.size() != 2 && regionOption.getRegion() == null) {
      event.reply(LanguageManager.getText(server.serv_language, "createPlayerMalformedWithoutRegionOption"));
      return;
    }else if((listArgs.isEmpty() || listArgs.size() > 2) && regionOption.getRegion() != null) {
      event.reply(String.format(LanguageManager.getText(server.serv_language, "createPlayerMalformedWithRegionOption"), 
          regionOption.getRegion().getName().toUpperCase()));
      return;
    }

    String regionName;
    String summonerName;
    if(listArgs.size() == 2) {
      regionName = listArgs.get(0);
      summonerName = listArgs.get(1);
    }else {
      regionName = regionOption.getRegion().getName();
      summonerName = listArgs.get(0);
    }


    Platform region = getPlatform(regionName);
    if(region == null) {
      event.reply(LanguageManager.getText(server.serv_language, "regionTagInvalid"));
      return;
    }

    Summoner summoner;
    try {
      summoner = Zoe.getRiotApi().getSummonerByName(region, summonerName);
    }catch(RiotApiException e) {
      RiotApiUtil.handleRiotApi(event, e, server.serv_language);
      return;
    }
    
    DTO.Player playerAlreadyWithTheAccount = PlayerRepository
        .getPlayerByLeagueAccountAndGuild(server.serv_guildId, summoner.getId(), region.getName());
    
    if(playerAlreadyWithTheAccount != null) {
      event.reply(String.format(LanguageManager.getText(server.serv_language, "accountAlreadyLinkedToAnotherPlayer"),
          playerAlreadyWithTheAccount.user.getName()));
      return;
    }

    PlayerRepository.createPlayer(server.serv_id, user.getIdLong(), false);
    DTO.Player player = PlayerRepository.getPlayer(server.serv_guildId, user.getIdLong());
    LeagueAccountRepository.createLeagueAccount(player.player_id, summoner, region.getName());
    
    if(config.getZoeRoleOption().getRole() != null) {
      Member member = event.getGuild().getMember(user);
      if(member != null) {
        event.getGuild().addRoleToMember(member, config.getZoeRoleOption().getRole()).queue();
      }
    }
    event.reply(String.format(LanguageManager.getText(server.serv_language, "createPlayerDoneMessage"),
        user.getName(), summoner.getName()));
  }

  public static Platform getPlatform(String regionName) {
    Platform region;
    try {
      region = Platform.getPlatformByName(regionName);
    } catch(NoSuchElementException e) {
      return null;
    }
    return region;
  }

  public static List<String> getParameterInParenteses(String args) {
    Matcher matcher = PARENTHESES_PATTERN.matcher(args);
    List<String> listArgs = new ArrayList<>();
    while(matcher.find()) {
      listArgs.add(matcher.group(1));
    }
    return listArgs;
  }

  public static boolean isTheGivenUserAlreadyRegister(User user, DTO.Server server) throws SQLException {
    DTO.Player player = PlayerRepository.getPlayer(server.serv_guildId, user.getIdLong());
    
    return player != null;
  }

  public static User getMentionedUser(List<Member> members) {
    if(members.size() != 1) {
      return null;
    }
    return members.get(0).getUser();
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
