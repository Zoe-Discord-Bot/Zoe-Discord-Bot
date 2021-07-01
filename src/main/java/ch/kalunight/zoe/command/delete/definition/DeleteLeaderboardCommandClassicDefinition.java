package ch.kalunight.zoe.command.delete.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.delete.DeleteLeaderboardCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

public class DeleteLeaderboardCommandClassicDefinition extends ZoeCommand {

  private EventWaiter eventWaiter;

  public DeleteLeaderboardCommandClassicDefinition(EventWaiter waiter) {
    this.name = "leaderboard";
    String[] aliases = {"leader", "lb", "lead", "board"};
    this.aliases = aliases;
    this.arguments = "";
    this.eventWaiter = waiter;
    this.help = "deleteLeaderboardCommandHelpMessage";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(DeletePlayerCommandClassicDefinition.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    Server server = getServer(event.getGuild().getIdLong());
    
    Message message = event.getChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "loadingData")).complete();
    
    DeleteLeaderboardCommandRunnable.executeCommand(server, event.getMember(), event.getTextChannel(), message, eventWaiter, null);
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
  
}
