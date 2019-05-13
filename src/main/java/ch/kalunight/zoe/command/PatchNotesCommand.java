package ch.kalunight.zoe.command;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class PatchNotesCommand extends Command {

  public PatchNotesCommand() {
    this.name = "patchNotes";
    String[] aliasesTable = {"notes", "note", "patch", "patchs"};
    this.aliases = aliasesTable;
    this.help = "Send the last patch note.";
    this.hidden = false;
    this.ownerCommand = false;
    this.guildOnly = false;
  }
  
  @Override
  protected void execute(CommandEvent event) {
    //TODO: Patch note are actually hard coded -> Implement a Patch note reader (reader of an external file).
    
    event.reply("**Actual version : 1.1.0**\n"
        + "\n"
        + "Changelog :\n"
        + "-Save system improvement.\n"
        + "-Added Recover and Reset commands.\n"
        + "-First implementation of stats commands, try `>stats help`.\n"
        + "-Fix the bug where infocards was not deleted correcly.\n"
        + "\n"
        + "For more information, please join the support server (`>help`).");
  }

}
