package ch.kalunight.zoe.command.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.command.BanAccountCommandRunnable;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.entities.Message;

public class BanAccountCommandClassicDefinition extends ZoeCommand {

  private EventWaiter waiter;

  public BanAccountCommandClassicDefinition(EventWaiter eventWaiter) {
    this.name = "banaccount";
    this.help = "banAccountCommandHelp";
    String[] aliasesTable = {"ban", "banList", "banAccountList"};
    this.aliases = aliasesTable;
    this.waiter = eventWaiter;
    this.hidden = false;
    this.ownerCommand = false;
    this.guildOnly = false;
    this.cooldown = 180;
    this.helpBiConsumer = CommandUtil.getHelpMethod(name, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    String language = LanguageManager.DEFAULT_LANGUAGE;
    if(event.getGuild() != null) {
      Server server = getServer(event.getGuild().getIdLong());
      language = server.getLanguage();
    }
    
    Message message = event.getChannel().sendMessage(LanguageManager.getText(language, "loading")).complete();
    
    BanAccountCommandRunnable.executeCommand(language, waiter, event.getChannel(), event.getGuild(), event.getAuthor(), message, null);
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
  
}
