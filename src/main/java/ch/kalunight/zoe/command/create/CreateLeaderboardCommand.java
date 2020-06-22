package ch.kalunight.zoe.command.create;

import java.awt.Color;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.SelectionDialog;

import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Leaderboard;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.leaderboard.Objective;
import ch.kalunight.zoe.repositories.LeaderboardRepository;
import ch.kalunight.zoe.service.leaderboard.LeaderboardBaseService;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

public class CreateLeaderboardCommand extends ZoeCommand {

  private static Logger logger = LoggerFactory.getLogger(CreateLeaderboardCommand.class);
  
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
        .setSelectionConsumer(getSelectionConsumer(server, event, objectiveList))
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
  
  private BiConsumer<Message, Integer> getSelectionConsumer(Server server, CommandEvent event, List<Objective> objectiveList) {
    return new BiConsumer<Message, Integer>() {
      @Override
      public void accept(Message selectionMessage, Integer objectiveSelection) {
        Objective objective = objectiveList.get(objectiveSelection - 1);
        
        event.reply(String.format(LanguageManager.getText(server.serv_language, "leaderboardObjectiveSelected"),
            LanguageManager.getText(server.serv_language, objective.getTranslationId())));
        
        waiter.waitForEvent(MessageReceivedEvent.class,
            e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel())
              && !e.getMessage().getId().equals(event.getMessage().getId()),
            e -> threatChannelSelection(e, server, objective), 2, TimeUnit.MINUTES,
            () -> cancelProcedure(event.getTextChannel(), server));
      }
    };
  }
  
  private void cancelProcedure(TextChannel channel, Server server) {
    channel.sendMessage(LanguageManager.getText(server.serv_language, "leaderboardObjectiveChannelSelectionTimeOut")).queue();
  }
  
  private void threatChannelSelection(MessageReceivedEvent event, Server server, Objective objectiveSelected) {
    Message message = event.getMessage();
    
    TextChannel leaderboardChannel;
    
    if(event.getMessage().getMentionedChannels().size() != 1) {
      message.getChannel().sendMessage(LanguageManager.getText(server.serv_language, "createLeaderboardNeedOneMentionnedChannel")).queue();
      waiter.waitForEvent(MessageReceivedEvent.class,
          e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel())
            && !e.getMessage().getId().equals(event.getMessage().getId()),
          e -> threatChannelSelection(e, server, objectiveSelected), 2, TimeUnit.MINUTES,
          () -> cancelProcedure(event.getTextChannel(), server));
      return;
    }
    
    leaderboardChannel = message.getMentionedChannels().get(0);
    
    try {
      Message leaderboardMessage = leaderboardChannel.sendMessage(LanguageManager.getText(server.serv_language, "leaderboardObjectiveBaseMessage")).complete();
      
      Leaderboard leaderboard = LeaderboardRepository.createLeaderboard(server.serv_id, leaderboardChannel.getIdLong(), leaderboardMessage.getIdLong(), objectiveSelected.toString());
      
      LeaderboardBaseService baseLeaderboardService = LeaderboardBaseService.getServiceWithId(objectiveSelected, server.serv_guildId, leaderboardChannel.getIdLong(), leaderboard.lead_id);
      
      ServerData.getLeaderboardExecutor().execute(baseLeaderboardService);
      
      event.getTextChannel().sendMessage(LanguageManager.getText(server.serv_language, "leaderboardSuccessfullyCreated")).queue();
    }catch(ErrorResponseException error) {
      message.getChannel().sendMessage(LanguageManager.getText(server.serv_language, "leaderboardMissingPermission")).queue();
      waiter.waitForEvent(MessageReceivedEvent.class,
          e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel())
            && !e.getMessage().getId().equals(event.getMessage().getId()),
          e -> threatChannelSelection(e, server, objectiveSelected), 2, TimeUnit.MINUTES,
          () -> cancelProcedure(event.getTextChannel(), server));
    } catch (SQLException e) {
      event.getTextChannel().sendMessage(LanguageManager.getText(server.serv_language, "errorSQLPleaseReport")).queue();
      logger.warn("SQL Error when creating leaderboard", e);
    }
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }

}
