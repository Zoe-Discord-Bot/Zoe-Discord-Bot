package ch.kalunight.zoe.command.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.SelectionDialog;

import ch.kalunight.zoe.command.LanguageCommandRunnable;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;

public class LanguageCommandClassicDefinition extends ZoeCommand {

  private EventWaiter waiter;
  
  public LanguageCommandClassicDefinition(EventWaiter waiter) {
    this.name = "language";
    String[] aliasesTable = {"lang", "l", "languages"};
    this.aliases = aliasesTable;
    this.help = "languageCommandHelp";
    this.hidden = false;
    this.ownerCommand = false;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL, Permission.MESSAGE_ADD_REACTION};
    this.userPermissions = permissionRequired;
    Permission[] permissionBot = {Permission.MESSAGE_ADD_REACTION, Permission.MANAGE_EMOTES, Permission.MESSAGE_EMBED_LINKS};
    this.botPermissions = permissionBot;
    this.guildOnly = true;
    this.waiter = waiter;
    this.helpBiConsumer = CommandUtil.getHelpMethod(name, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    SelectionDialog dialog = LanguageCommandRunnable.executeCommand(getServer(event.getGuild().getIdLong()), waiter, event.getAuthor());
    dialog.display(event.getChannel());
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
