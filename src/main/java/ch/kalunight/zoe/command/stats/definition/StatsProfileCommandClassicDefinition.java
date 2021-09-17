package ch.kalunight.zoe.command.stats.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.stats.StatsCommandRunnable;
import ch.kalunight.zoe.command.stats.StatsProfileCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

public class StatsProfileCommandClassicDefinition extends ZoeCommand {

  private EventWaiter waiter;
  
  public StatsProfileCommandClassicDefinition(EventWaiter eventWaiter) {
    this.name = "profile";
    String[] aliases = {"player", "players", "p"};
    this.aliases = aliases;
    this.arguments = "@playerMention OR (Region) (summonerName)";
    this.help = "statsProfileHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(StatsCommandRunnable.USAGE_NAME, name, arguments, help);
    this.waiter = eventWaiter;
    Permission[] botPermissionNeeded = {Permission.MANAGE_EMOTES, Permission.MESSAGE_EMBED_LINKS,
        Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_MANAGE};
    this.botPermissions = botPermissionNeeded;
    this.guildOnly = true;
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    Server server = getServer(event.getGuild().getIdLong());
    
    Message messageLoading = event.getTextChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "loadingSummoner")).complete();
    
    StatsProfileCommandRunnable.executeCommand(server, event.getTextChannel(), event.getArgs(), event.getMessage().getMentionedUsers(), messageLoading, null, waiter, event.getMember(), false);
  }
  
  public void executeCommandExternal(CommandEvent event) throws SQLException {
    executeCommand(event);
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
  
}
