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
    
    event.reply("**Actual version : 1.3.0**\n"
        + "\n"
        + "**Configurations Updates !**\n"
        + "Configurations options as been added to make Zoe perfectly suit with your server ! Try the command `>config`. "
        + "More options will be added in the time, if you have a idea you can come to the support server to request your idea ! (`>help`)\n\n"
        + "Changelog :\n"
        + "-**New Feature** 3 new options of configurations. Try the commands `>config`."
        + "-**New Feature** Zoe now detect your discord status to create your infocards in about 20 seconds after the champ select.\n"
        + "-Internal system has been reworked to improve performance.\n"
        + "-Bugs fix\n"
        + "\n"
        + "For help, feature request and bug report please join the support server (`>help`).");
  }

}
