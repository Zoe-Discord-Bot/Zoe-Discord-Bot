package ch.kalunight.zoe.command.create.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.create.CreateLeaderboardCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

public class CreateLeaderboardCommandClassicDefinition extends ZoeCommand {

  private EventWaiter waiter;

  public CreateLeaderboardCommandClassicDefinition(EventWaiter waiter) {
    this.name = "leaderboard";
    String[] aliases = {"leader", "lb", "lead", "board"};
    this.aliases = aliases;
    this.arguments = "";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.guildOnly = true;
    this.help = "createLeaderboardHelpMessage";
    this.waiter = waiter;
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(CreateCommandClassicDefinition.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    Server server = getServer(event.getGuild().getIdLong());
    
    Message message = event.getTextChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "loadingData")).complete();
    
    CreateLeaderboardCommandRunnable.executeCommand(server, event.getMember(), event.getTextChannel(), message, waiter, null);
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
  
}
