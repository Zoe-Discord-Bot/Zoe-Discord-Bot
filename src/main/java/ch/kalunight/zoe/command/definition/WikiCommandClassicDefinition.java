package ch.kalunight.zoe.command.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.command.WikiCommandRunnable;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;

public class WikiCommandClassicDefinition extends ZoeCommand {

  public WikiCommandClassicDefinition() {
    this.name = "wiki";
    this.help = "wikiCommandHelp";
    this.hidden = false;
    this.ownerCommand = false;
    this.guildOnly = false;
    this.helpBiConsumer = CommandUtil.getHelpMethod(name, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    String language;
    if(event.getGuild() != null) {
      language = getServer(event.getGuild().getIdLong()).getLanguage();
    }else {
      language = LanguageManager.DEFAULT_LANGUAGE;
    }
    
    event.getChannel().sendMessage(WikiCommandRunnable.executeCommand(language)).queue();
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
  
}
