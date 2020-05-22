package ch.kalunight.zoe.command.create;

import java.awt.Color;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.SelectionDialog;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.Objective;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

public class CreateLeaderboardCommand extends ZoeCommand {

  private EventWaiter waiter;
  
  public CreateLeaderboardCommand(EventWaiter waiter) {
    this.name = "leaderboard";
    String[] aliases = {"leader", "lb", "lead"};
    this.aliases = aliases;
    this.arguments = "";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.guildOnly = true;
    this.help = "createLeaderboardHelpMessage";
    this.waiter = waiter;
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(CreateCommand.USAGE_NAME, name, arguments, help);
  }
  
  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    
    DTO.Server server = getServer(event.getGuild().getIdLong());
    
    event.reply(LanguageManager.getText(server.serv_language, "createLeaderboardExplainMessage"));
    
    List<Objective> objectiveList = new ArrayList<>();
    List<String> objectiveChoices = new ArrayList<>();
    
    SelectionDialog.Builder selectAccountBuilder = new SelectionDialog.Builder()
        .addUsers(event.getAuthor())
        .setEventWaiter(waiter)
        .useLooping(true)
        .setColor(Color.BLUE)
        .setSelectedEnds("**", "**")
        .setCanceled(getSelectionCancelAction(server.serv_language))
        .setSelectionConsumer(getSelectionConsumer(server.serv_language, event, objectiveList))
        .setTimeout(2, TimeUnit.MINUTES);
    

    for(Objective objective : Objective.values()) {
      String actualChoice = String.format(LanguageManager.getText(server.serv_language, objective.getTranslationId()));
      
      objectiveChoices.add(actualChoice);
      selectAccountBuilder.addChoices(actualChoice);
      objectiveList.add(objective);
    }
    
    selectAccountBuilder.setText(LanguageManager.getText(server.serv_language, "createLeaderboardTitleListeObjective"));
    
    SelectionDialog choiceLeaderBoard = selectAccountBuilder.build();
    choiceLeaderBoard.display(event.getChannel());
  }

  private Consumer<Message> getSelectionCancelAction(String language){
    return new Consumer<Message>() {
      @Override
      public void accept(Message message) {
        message.clearReactions().queue();
        message.editMessage(LanguageManager.getText(language, "createLeaderboardCancelMessage")).queue();
      }
    };
  }
  
  private BiConsumer<Message, Integer> getSelectionConsumer(String language, CommandEvent event, List<Objective> objectiveList) {
    return new BiConsumer<Message, Integer>() {
      @Override
      public void accept(Message selectionMessage, Integer objectiveSelection) {
        Objective objective = objectiveList.get(objectiveSelection);
        
        event.reply(String.format(LanguageManager.getText(language, "leaderboardObjectiveSelected"), LanguageManager.getText(language, objective.getTranslationId())));
      }
    };
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    // TODO Auto-generated method stub
    return null;
  }

}
