package ch.kalunight.zoe.command.clash.definition;

import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.clash.ClashCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.util.CommandUtil;

public class ClashCommandClassicDefinition extends ZoeCommand {

  public ClashCommandClassicDefinition() {
    this.name = ClashCommandRunnable.USAGE_NAME;
    Command[] commandsChildren = {new ClashRefreshCommandClassicDefinition(), new ClashAnalyseCommandClassicDefinition()};
    this.children = commandsChildren;
    this.helpBiConsumer = CommandUtil.getHelpMethodHasChildren(ClashCommandRunnable.USAGE_NAME, commandsChildren);
  }
  
  @Override
  protected void executeCommand(CommandEvent event) {
    DTO.Server server = getServer(event.getGuild().getIdLong());
    ClashCommandRunnable.executeCommand(server, event.getTextChannel());
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
  
}
