package ch.kalunight.zoe.command.remove.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.remove.RemoveAccountCommandRunnable;
import ch.kalunight.zoe.command.remove.RemoveCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.util.CommandUtil;

public class RemoveAccountCommandClassicDefinition extends ZoeCommand {

  public RemoveAccountCommandClassicDefinition() {
    this.name = RemoveAccountCommandRunnable.USAGE_NAME;
    String[] aliases = {"accountToPlayers", "accountsToPlayers", "accountToPlayers", "accountToPlayer", "accounts"};
    this.aliases = aliases;
    this.help = "removeAccountHelpMessage";
    this.arguments = "@MentionOfPlayer (Region) (SummonerName)";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(RemoveCommandRunnable.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    Server server = getServer(event.getGuild().getIdLong());
    
    String message = RemoveAccountCommandRunnable.executeCommand(server, event.getMember(), event.getMessage().getMentionedMembers(), event.getArgs());
    
    event.reply(message);
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
  
}
