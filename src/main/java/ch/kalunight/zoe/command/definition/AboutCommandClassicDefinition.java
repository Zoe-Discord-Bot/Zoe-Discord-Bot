package ch.kalunight.zoe.command.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.command.AboutCommandRunnable;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class AboutCommandClassicDefinition extends ZoeCommand {

  public AboutCommandClassicDefinition() {
    this.name = "about";
    this.help = "aboutCommandHelp";
    this.hidden = false;
    this.guildOnly = false;
    this.ownerCommand = false;
    this.helpBiConsumer = CommandUtil.getHelpMethod(name, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    MessageEmbed embed = AboutCommandRunnable.executeCommand(event.getJDA(), getLanguage(event.getGuild()));
    event.reply(embed);
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
  
}
