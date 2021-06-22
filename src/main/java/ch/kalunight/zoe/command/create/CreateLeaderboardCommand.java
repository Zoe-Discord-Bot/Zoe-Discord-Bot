package ch.kalunight.zoe.command.create;

import java.awt.Color;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.SelectionDialog;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.create.definition.CreateCommandClassicDefinition;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.leaderboard.LeaderboardExtraDataHandler;
import ch.kalunight.zoe.model.leaderboard.dataholder.Objective;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

public class CreateLeaderboardCommand extends ZoeCommand {

  private EventWaiter waiter;

  public CreateLeaderboardCommand(EventWaiter waiter) {
    this.name = "leaderboard";
    String[] aliases = {"leader", "lb", "lead", "board"};
    this.aliases = aliases;
    this.arguments = "";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.guildOnly = true;
    this.help = "createLeaderboardHelpMessage";
    this.waiter = waiter;
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(CreateCommandClassicDefinition.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {

    DTO.Server server = getServer(event.getGuild().getIdLong());

    event.reply(LanguageManager.getText(server.getLanguage(), "createLeaderboardExplainMessage"));

    List<Objective> objectiveList = new ArrayList<>();
    List<String> objectiveChoices = new ArrayList<>();
    AtomicBoolean actionDone = new AtomicBoolean(false);

    SelectionDialog.Builder selectAccountBuilder = new SelectionDialog.Builder()
        .addUsers(event.getAuthor())
        .setEventWaiter(waiter)
        .useLooping(true)
        .setColor(Color.GREEN)
        .setSelectedEnds("**", "**")
        .setCanceled(getSelectionCancelAction(server.getLanguage(), actionDone))
        .setSelectionConsumer(getSelectionConsumer(server, event, objectiveList, actionDone))
        .setTimeout(2, TimeUnit.MINUTES);

    for(Objective objective : Objective.values()) {
      String actualChoice = String.format(LanguageManager.getText(server.getLanguage(), objective.getTranslationId()));

      objectiveChoices.add(actualChoice);
      selectAccountBuilder.addChoices(actualChoice);
      objectiveList.add(objective);
    }

    selectAccountBuilder.setText(LanguageManager.getText(server.getLanguage(), "createLeaderboardTitleListeObjective"));

    SelectionDialog choiceLeaderBoard = selectAccountBuilder.build();
    choiceLeaderBoard.display(event.getChannel());
  }

  private Consumer<Message> getSelectionCancelAction(String language, AtomicBoolean selectionDone){
    return new Consumer<Message>() {
      @Override
      public void accept(Message message) {
        if(!selectionDone.get()) {
          message.clearReactions().queue();
          message.editMessage(LanguageManager.getText(language, "createLeaderboardCancelMessage")).queue();
        }
      }
    };
  }

  private BiConsumer<Message, Integer> getSelectionConsumer(Server server, CommandEvent event, List<Objective> objectiveList, AtomicBoolean selectionDone) {
    return new BiConsumer<Message, Integer>() {
      @Override
      public void accept(Message selectionMessage, Integer objectiveSelection) {
        selectionMessage.clearReactions().queue();
        selectionDone.set(true);

        Objective objective = objectiveList.get(objectiveSelection - 1);

        event.reply(String.format(LanguageManager.getText(server.getLanguage(), "leaderboardObjectiveSelected"),
            LanguageManager.getText(server.getLanguage(), objective.getTranslationId())));

        LeaderboardExtraDataHandler dataNeeded = Objective.getDataNeeded(objective, waiter, server, event, true);

        dataNeeded.handleSecondCreationPart();
      }
    };
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }

}
