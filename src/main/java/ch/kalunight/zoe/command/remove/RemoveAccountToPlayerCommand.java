package ch.kalunight.zoe.command.remove;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.command.CommandUtil;
import ch.kalunight.zoe.command.create.CreatePlayerCommand;
import ch.kalunight.zoe.model.LeagueAccount;
import ch.kalunight.zoe.model.Player;
import ch.kalunight.zoe.model.Server;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.User;
import net.rithms.riot.constant.Platform;

public class RemoveAccountToPlayerCommand extends Command {

  public static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\(([^)]+)\\)");
  public static final String USAGE_NAME = "accountToPlayer";

  public RemoveAccountToPlayerCommand() {
    this.name = USAGE_NAME;
    this.help = "Remove the given account to the mentionned player. Manage Channel permission needed.";
    this.arguments = "@MentionOfPlayer (Region) (SummonerName)";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.helpBiConsumer = getHelpMethod();
  }
  
  @Override
  protected void execute(CommandEvent event) {
    event.getTextChannel().sendTyping().complete();
    
    Server server = ServerData.getServers().get(event.getGuild().getId());

    User user = CreatePlayerCommand.getMentionedUser(event.getMessage().getMentionedMembers());
    if(user == null) {
      event.reply("Please mention 1 member of the server "
          + "(e.g. `>create player @" + event.getMember().getUser().getName() + " (Region) (SummonerName)`)");
      return;
    }
    
    Player player = server.getPlayerByDiscordId(user.getId());
    if(player == null) {
      event.reply("The mentionned user is not registered !");
      return;
    }

    List<String> listArgs = CreatePlayerCommand.getParameterInParenteses(event.getArgs());
    if(listArgs.size() != 2) {
      event.reply("The command is malformed. Please respect this pattern : `>create player @DiscordPlayerMention (Region) (SummonerName)`");
      return;
    }
    
    String regionName = listArgs.get(0);
    String summonerName = listArgs.get(1);

    Platform region = CreatePlayerCommand.getPlatform(regionName);
    if(region == null) {
      event.reply("The region tag is invalid. (Valid tag : EUW, EUNE, NA, BR, JP, KR, LAN, LAS, OCE, RU, TR)");
      return;
    }
    
    LeagueAccount account = player.getLeagueAccountsBySummonerName(region, summonerName);
    if(account == null) {
      event.reply("The given summoner is not linked to the mentionned player !");
      return;
    }
    
    player.getLolAccounts().remove(account);
    event.reply("The account \"" + account.getSummoner().getName() + "\" has been unlink of the player " + user.getName() + ".");
  }
  
  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Remove " + name + " command :\n");
        stringBuilder.append("--> `>remove " + name + " " + arguments + "` : " + help);

        event.reply(stringBuilder.toString());
      }
    };
  }

}
