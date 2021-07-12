package ch.kalunight.zoe.command.show.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.show.ShowCommandRunnable;
import ch.kalunight.zoe.command.show.ShowPlayerCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

public class ShowPlayerCommandClassicDefinition extends ZoeCommand {

  private final EventWaiter waiter;

  public ShowPlayerCommandClassicDefinition(EventWaiter eventWaiter) {
    this.name = ShowPlayerCommandRunnable.USAGE_NAME;
    String[] aliases = {"p", "player"};
    this.arguments = "";
    this.aliases = aliases;
    this.waiter = eventWaiter;
    this.help = "showPlayerHelpMessage";
    this.cooldown = 10;
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(ShowCommandRunnable.USAGE_NAME, name, arguments, help);
    Permission[] botPermissionNeeded = {Permission.MANAGE_EMOTES, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_MANAGE};
    this.botPermissions = botPermissionNeeded;
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    Server server = getServer(event.getGuild().getIdLong());
    
    Message message = event.getTextChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "loadingSummoner")).complete();
    
    ShowPlayerCommandRunnable.executeCommand(server, waiter, event.getMember(), event.getTextChannel(), message, null);
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}