package ch.kalunight.zoe.command.add.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.add.AddAccountCommandRunnable;
import ch.kalunight.zoe.command.add.AddCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.entities.Message;

public class AddAccountCommandClassicDefinition extends ZoeCommand {

  public AddAccountCommandClassicDefinition() {
    this.name = AddAccountCommandRunnable.USAGE_NAME;
    String[] aliases = {"accountToPlayers", "accountsToPlayers", "accountToPlayers", "accountToPlayer", "accounts"};
    this.aliases = aliases;
    this.arguments = "@MentionPlayer (Region) (accountName)";
    this.help = "addAccountHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(AddCommandRunnable.USAGE_NAME, AddAccountCommandRunnable.USAGE_NAME, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    Server server = getServer(event.getGuild().getIdLong());
    
    Message message = event.getTextChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "loadingData")).complete();
    
    String messageToSend = AddAccountCommandRunnable.executeCommand(server, event.getMember(), event.getMessage().getMentionedMembers(), event.getArgs());
    
    message.editMessage(messageToSend).queue();
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    // TODO Auto-generated method stub
    return null;
  }
  
}
