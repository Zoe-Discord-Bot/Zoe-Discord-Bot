package ch.kalunight.zoe.command.clash.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.clash.ClashCommandRunnable;
import ch.kalunight.zoe.command.clash.ClashRefreshCommandRunnable;
import ch.kalunight.zoe.util.CommandUtil;

public class ClashRefreshCommandClassicDefinition extends ZoeCommand {

  public ClashRefreshCommandClassicDefinition() {
    this.name = ClashRefreshCommandRunnable.USAGE_NAME;
    String[] aliases = {"r"};
    this.arguments = "";
    this.aliases = aliases;
    this.help = "clashRefreshHelpMessage";
    this.cooldown = 10;
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(ClashCommandRunnable.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    ClashRefreshCommandRunnable.executeCommand(getServer(event.getGuild().getIdLong()), event.getTextChannel(), null);
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
