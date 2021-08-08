package ch.kalunight.zoe.command.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.command.PatchNotesCommandRunnable;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.util.CommandUtil;

public class PatchNotesCommandClassicDefinition extends ZoeCommand {

  public PatchNotesCommandClassicDefinition() {
    this.name = "patchNotes";
    String[] aliasesTable = {"notes", "note", "patch", "patchs", "patchNote"};
    this.aliases = aliasesTable;
    this.help = "patchNotesCommandHelp";
    this.hidden = false;
    this.ownerCommand = false;
    this.guildOnly = false;
    this.helpBiConsumer = CommandUtil.getHelpMethod(name, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    event.reply(PatchNotesCommandRunnable.executeCommand(getServer(event.getGuild().getIdLong())));
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
  
}
