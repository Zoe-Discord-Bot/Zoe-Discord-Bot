package ch.kalunight.zoe.command.clash;

import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;

public class ClashCommand extends ZoeCommand {

  public static final String USAGE_NAME = "clash";

  public ClashCommand() {
    this.name = USAGE_NAME;
    Command[] commandsChildren = {new ClashRefreshCommand(), new ClashAnalyseCommand()};
    this.children = commandsChildren;
    this.helpBiConsumer = CommandUtil.getHelpMethodHasChildren(USAGE_NAME, commandsChildren);
  }
  
  @Override
  protected void executeCommand(CommandEvent event) {
    DTO.Server server = getServer(event.getGuild().getIdLong());
    event.reply(LanguageManager.getText(server.getLanguage(), "mainClashCommandHelpMessage"));
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }

}
