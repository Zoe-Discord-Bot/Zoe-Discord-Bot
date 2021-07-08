package ch.kalunight.zoe.command.clash.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.clash.ClashAnalyseCommandRunnable;
import ch.kalunight.zoe.command.show.ShowCommand;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.entities.Message;

public class ClashAnalyseCommandClassicDefinition extends ZoeCommand {
  
  public ClashAnalyseCommandClassicDefinition() {
    this.name = ClashAnalyseCommandRunnable.USAGE_NAME;
    String[] aliases = {"stats"};
    this.arguments = "(Platform) (Summoner Name)";
    this.aliases = aliases;
    this.help = "clashAnalyzeHelpMessage";
    this.cooldown = 30;
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(ShowCommand.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    Server server = getServer(event.getGuild().getIdLong());
    
    Message loadData = event.getTextChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "loadingData")).complete();
    
    ClashAnalyseCommandRunnable.executeCommand(server, event.getTextChannel(), event.getArgs(), loadData, null);
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
  
}
