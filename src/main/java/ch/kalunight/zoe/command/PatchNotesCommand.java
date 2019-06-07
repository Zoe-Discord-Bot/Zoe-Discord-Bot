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
    
    event.reply("**Actual version : 1.2.0**\n"
        + "\n"
        + "**Multiples accounts system added !**\n"
        + "You can now link multiples accounts to a player ! Try the commands `>add accountToPlayer` and `>remove accountToPlayer`. \n\n"
        + "Changelog :\n"
        + "-Add more information for dev about Riot Api usage.\n"
        + "-Implement multiples accounts system.\n"
        + "-First implementation of show commands, try `>show players`.\n"
        + "-Remove recovery command (Was unusable with the new system of LoL accounts).\n"
        + "-Some minor fix.\n"
        + "\n"
        + "For more information, please join the support server (`>help`).");
  }

}
