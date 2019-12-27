package ch.kalunight.zoe.command;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.function.BiConsumer;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;

public class PatchNotesCommand extends ZoeCommand {

  private static final File patchNoteFile = new File("ressources/patchnotes.txt");
  
  public PatchNotesCommand() {
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
    CommandUtil.sendTypingInFonctionOfChannelType(event);
    
    try(final BufferedReader reader = new BufferedReader(new FileReader(patchNoteFile));) {
      String line;
      
      StringBuilder builder = new StringBuilder();
      
      while((line = reader.readLine()) != null) {
        builder.append(line + "\n");
      }
      
      event.reply(builder.toString());
    } catch(IOException e) {
      DTO.Server server = getServer(event.getGuild().getIdLong());
      event.reply(LanguageManager.getText(server.serv_language, "patchNoteUnavailable"));
    }
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }

}
