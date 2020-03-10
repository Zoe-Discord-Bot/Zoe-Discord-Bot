package ch.kalunight.zoe.command.add;

import java.sql.SQLException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.create.CreatePlayerCommand;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.config.option.RegionOption;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.player_data.LeagueAccount;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.LeagueAccountRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import ch.kalunight.zoe.util.RiotApiUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;

public class AddAccountCommand extends ZoeCommand {

  public static final String USAGE_NAME = "account";
  public static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\(([^)]+)\\)");

  public AddAccountCommand() {
    this.name = USAGE_NAME;
    String[] aliases = {"accountToPlayers", "accountsToPlayers", "accountToPlayers", "accountToPlayer", "accounts"};
    this.aliases = aliases;
    this.arguments = "@MentionPlayer (Region) (accountName)";
    this.help = "addAccountHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(AddCommand.USAGE_NAME, USAGE_NAME, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    event.getTextChannel().sendTyping().complete();
    
    DTO.Server server = getServer(event.getGuild().getIdLong());
    
    ServerConfiguration config = ConfigRepository.getServerConfiguration(server.serv_guildId);
    
    if(!config.getUserSelfAdding().isOptionActivated() && !event.getMember().getPermissions().contains(Permission.MANAGE_CHANNEL)) {
        event.reply(String.format(LanguageManager.getText(server.serv_language, "permissionNeededMessage"),
            Permission.MANAGE_CHANNEL.getName()));
        return;
    }
    
    User user = CreatePlayerCommand.getMentionedUser(event.getMessage().getMentionedMembers());
    if(user == null) {
      event.reply(String.format(LanguageManager.getText(server.serv_language, "mentionNeededMessageWithUser"),
          event.getMember().getUser().getName()));
      return;
    }else if(!user.equals(event.getAuthor()) && !event.getMember().getPermissions().contains(Permission.MANAGE_CHANNEL)) {
      event.reply(LanguageManager.getText(server.serv_language, "permissionNeededUpdateOtherPlayer"));
      return;
    }

    DTO.Player player = PlayerRepository.getPlayer(server.serv_guildId, user.getIdLong());
    if(player == null) {
      event.reply(LanguageManager.getText(server.serv_language, "mentionnedUserNotRegistered"));
      return;
    }

    RegionOption regionOption = config.getDefaultRegion();

    List<String> listArgs = CreatePlayerCommand.getParameterInParenteses(event.getArgs());
    if(listArgs.size() != 2 && regionOption.getRegion() == null) {
      event.reply(String.format(LanguageManager.getText(server.serv_language, "addCommandMalformedWithoutRegionOption"), name));
      return;
    }else if((listArgs.isEmpty() || listArgs.size() > 2) && regionOption.getRegion() != null) {
      event.reply(String.format(LanguageManager.getText(server.serv_language, "addCommandMalformedWithRegionOption"),
          name, regionOption.getRegion().getName().toUpperCase()));
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

    Platform region = CreatePlayerCommand.getPlatform(regionName);
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

    LeagueAccount newAccount = new LeagueAccount(summoner, region);

    DTO.Player playerAlreadyWithTheAccount = PlayerRepository
        .getPlayerByLeagueAccountAndGuild(server.serv_guildId, summoner.getId(), region);

    if(playerAlreadyWithTheAccount != null) {
      User userAlreadyWithTheAccount = Zoe.getJda().retrieveUserById(playerAlreadyWithTheAccount.player_discordId).complete();
      event.reply(String.format(LanguageManager.getText(server.serv_language, "accountAlreadyLinkedToAnotherPlayer"), 
          userAlreadyWithTheAccount.getName()));
      return;
    }

    
    LeagueAccountRepository.createLeagueAccount(player.player_id, summoner, region.getName());
    event.reply(String.format(LanguageManager.getText(server.serv_language, "accountAddedToPlayer"),
        newAccount.getSummoner().getName(), user.getName()));
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }

}
