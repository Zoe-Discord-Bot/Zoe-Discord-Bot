package ch.kalunight.zoe.command.remove;

import java.util.List;
import java.util.regex.Pattern;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.command.CommandUtil;
import ch.kalunight.zoe.command.create.CreatePlayerCommand;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.player_data.LeagueAccount;
import ch.kalunight.zoe.model.player_data.Player;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.rithms.riot.constant.Platform;

public class RemoveAccountCommand extends Command {

  public static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\(([^)]+)\\)");
  public static final String USAGE_NAME = "account";

  public RemoveAccountCommand() {
    this.name = USAGE_NAME;
    String[] aliases = {"accountToPlayers", "accountsToPlayers", "accountToPlayers", "accountToPlayer", "accounts"};
    this.aliases = aliases;
    this.help = "removeAccountHelpMessage";
    this.arguments = "@MentionOfPlayer (Region) (SummonerName)";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(RemoveCommand.USAGE_NAME, name, arguments, help);
  }
  
  @Override
  protected void execute(CommandEvent event) {
    event.getTextChannel().sendTyping().complete();
    
    Server server = ServerData.getServers().get(event.getGuild().getId());
    
    if(!server.getConfig().getUserSelfAdding().isOptionActivated() &&
        !event.getMember().getPermissions().contains(Permission.MANAGE_CHANNEL)) {
        event.reply(String.format(LanguageManager.getText(server.getLangage(), "deletePlayerMissingPermission"),
            Permission.MANAGE_CHANNEL.getName()));
        return;
    }
    
    User user = CreatePlayerCommand.getMentionedUser(event.getMessage().getMentionedMembers());
    if(user == null) {
      event.reply(String.format(LanguageManager.getText(server.getLangage(), "removeAccountMissingMention"), event.getMember().getUser().getName()));
      return;
    } else if(!user.equals(event.getAuthor()) && !event.getMember().getPermissions().contains(Permission.MANAGE_CHANNEL)) {
      event.reply(String.format(LanguageManager.getText(server.getLangage(), "removeAccountMissingRight"),
          Permission.MANAGE_CHANNEL.getName()));
      return;
    }
    
    Player player = server.getPlayerByDiscordId(user.getId());
    if(player == null) {
      event.reply(LanguageManager.getText(server.getLangage(), "removeAccountUserNotRegistered"));
      return;
    }

    List<String> listArgs = CreatePlayerCommand.getParameterInParenteses(event.getArgs());
    if(listArgs.size() != 2) {
      event.reply(LanguageManager.getText(server.getLangage(), "removeAccountMalformed"));
      return;
    }
    
    String regionName = listArgs.get(0);
    String summonerName = listArgs.get(1);

    Platform region = CreatePlayerCommand.getPlatform(regionName);
    if(region == null) {
      event.reply(LanguageManager.getText(server.getLangage(), "regionTagInvalid"));
      return;
    }

    LeagueAccount account = player.getLeagueAccountsBySummonerName(region, summonerName);
    if(account == null) {
      event.reply(LanguageManager.getText(server.getLangage(), "removeAccountNotLinkedToPlayer"));
      return;
    }
    
    player.getLolAccounts().remove(account);
    event.reply(String.format(LanguageManager.getText(server.getLangage(), "removeAccountDoneMessage"),
        account.getSummoner().getName(), user.getName()));
  }
}
