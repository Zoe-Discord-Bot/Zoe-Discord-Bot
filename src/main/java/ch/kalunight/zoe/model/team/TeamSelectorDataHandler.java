package ch.kalunight.zoe.model.team;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.SelectionDialog;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.create.CreatePlayerCommand;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.TeamUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.clash.constant.TeamPosition;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;

public class TeamSelectorDataHandler {

  private static final String STATS_TEAM_ANALYSIS_CANCEL_MESSAGE_ID = "statsTeamAnalysisCancelMessage";

  private List<AccountDataWithRole> accountsSelected;
  
  private EventWaiter waiter;
  
  private Server server;
  
  private CommandEvent baseEvent;
  
  private TeamSelectorDataManager dataManager;
  
  private ThreadPoolExecutor whereToExecute;
  
  public TeamSelectorDataHandler(EventWaiter waiter, Server server, CommandEvent baseEvent, TeamSelectorDataManager dataManager, ThreadPoolExecutor whereToExecute) {
    accountsSelected = Collections.synchronizedList(new ArrayList<>());
    this.waiter = waiter;
    this.server = server;
    this.baseEvent = baseEvent;
    this.dataManager = dataManager;
    this.whereToExecute = whereToExecute;
  }
  
  public void askSelectionAccount() {
    baseEvent.getTextChannel().sendTyping().queue();
    baseEvent.getTextChannel().sendMessage(String.format(LanguageManager.getText(server.getLanguage(), "statsTeamAnalysisAskAccount"), accountsSelected.size())
        + " " + LanguageManager.getText(server.getLanguage(), STATS_TEAM_ANALYSIS_CANCEL_MESSAGE_ID)).queue();
    
    defineAccountSelectionWaiter();
  }

  private void defineAccountSelectionWaiter() {
    waiter.waitForEvent(MessageReceivedEvent.class,
        e -> e.getAuthor().equals(baseEvent.getAuthor()) && e.getChannel().equals(baseEvent.getChannel())
        && !e.getMessage().getId().equals(baseEvent.getMessage().getId()),
        e -> threatAccountSelection(e), 3, TimeUnit.MINUTES,
        () -> cancelSelectionOfAnAccount());
  }

  private void threatAccountSelection(MessageReceivedEvent messageReceivedEvent) {
    baseEvent.getTextChannel().sendTyping().queue();
    String messageReceived = messageReceivedEvent.getMessage().getContentRaw();
    
    List<String> listArgs = CreatePlayerCommand.getParameterInParenteses(messageReceived);
    
    if(listArgs.size() != 2) {
      baseEvent.reply(LanguageManager.getText(server.getLanguage(), "statsTeamAnalysisMalformedAccount") 
          + " " + LanguageManager.getText(server.getLanguage(), STATS_TEAM_ANALYSIS_CANCEL_MESSAGE_ID));
      
      return;
    }

    String regionName = listArgs.get(0);
    String summonerName = listArgs.get(1);

    Platform region = CreatePlayerCommand.getPlatform(regionName);
    if(region == null) {
      baseEvent.reply(LanguageManager.getText(server.getLanguage(), "statsTeamAnalysisRegionTagInvalid") 
          + " " + LanguageManager.getText(server.getLanguage(), STATS_TEAM_ANALYSIS_CANCEL_MESSAGE_ID));
      defineAccountSelectionWaiter();
      return;
    }

    Summoner summoner;
    try {
      summoner = Zoe.getRiotApi().getSummonerByName(region, summonerName);
    }catch(RiotApiException e) {
      if(e.getErrorCode() == RiotApiException.DATA_NOT_FOUND) {
        baseEvent.reply(LanguageManager.getText(server.getLanguage(), "statsTeamAnalysisSummonerNotFound") 
            + " " + LanguageManager.getText(server.getLanguage(), STATS_TEAM_ANALYSIS_CANCEL_MESSAGE_ID));
      }else {
        baseEvent.reply(LanguageManager.getText(server.getLanguage(), "statsTeamAnalysisRiotApi") 
            + " " + LanguageManager.getText(server.getLanguage(), STATS_TEAM_ANALYSIS_CANCEL_MESSAGE_ID));
      }
      defineAccountSelectionWaiter();
      return;
    }
    
    
    defineRoleSelectionWaiter(region, summoner);
  }
  
  private void defineRoleSelectionWaiter(Platform platform, Summoner summoner) {
    
    SelectionDialog.Builder selectRoleBuilder = new SelectionDialog.Builder()
        .addUsers(baseEvent.getAuthor())
        .setEventWaiter(waiter)
        .useLooping(true)
        .setColor(Color.GREEN)
        .setSelectedEnds("**", "**")
        .setCanceled(getSelectionCancelAction(server.getLanguage()))
        .setTimeout(2, TimeUnit.MINUTES);
    
    List<TeamPosition> rolesOrder = new ArrayList<>();
    
    selectRoleBuilder.addChoices(LanguageManager.getText(server.getLanguage(), TeamUtil.getTeamPositionId(TeamPosition.FILL))
    + " " + LanguageManager.getText(server.getLanguage(), "statsTeamAnalysisRoleSelectionFillInfo"));
    rolesOrder.add(TeamPosition.FILL);
    
    for(TeamPosition roleToSelect : TeamPosition.values()) {
      if(roleToSelect != TeamPosition.UNSELECTED && roleToSelect != TeamPosition.FILL) {
        selectRoleBuilder.addChoices(LanguageManager.getText(server.getLanguage(), TeamUtil.getTeamPositionId(roleToSelect)));
        rolesOrder.add(roleToSelect);
      }
    }
    
    selectRoleBuilder.setSelectionConsumer(roleSelectedDoneAction(rolesOrder, platform, summoner));
  }
  
  private BiConsumer<Message, Integer> roleSelectedDoneAction(List<TeamPosition> rolesOrder, Platform platform, Summoner summoner) {
    return new BiConsumer<Message, Integer>() {
      
      @Override
      public void accept(Message baseMessage, Integer seletedAnswerId) {
        TeamPosition selected = rolesOrder.get(seletedAnswerId);
        
        String summonerName = "*" + platform.getName().toUpperCase() + "* " + summoner.getName();
        
        baseMessage.reply(String.format(LanguageManager.getText(server.getLanguage(), "statsTeamAnalysisAccountCorrectlySelected"), summonerName,
            LanguageManager.getText(server.getLanguage(), TeamUtil.getTeamPositionId(selected))));
        
        accountsSelected.add(new AccountDataWithRole(summoner, platform, selected));
        manageAccountNeededOrNot();
      }
    };
  }
  
  private void manageAccountNeededOrNot() {
    if(accountsSelected.size() == 5) {
      baseEvent.reply("*" + LanguageManager.getText(server.getLanguage(), "clashAnalyzeLoadStarted") + "*");
      dataManager.feedWithData(accountsSelected);
      whereToExecute.execute(dataManager);
    }else {
      askSelectionAccount();
    }
  }

  private Consumer<Message> getSelectionCancelAction(String language){
    return new Consumer<Message>() {
      @Override
      public void accept(Message message) {
        message.clearReactions().queue();
        message.editMessage(LanguageManager.getText(language, "statsTeamAnalysisRoleSelectionCancel")).queue();
      }
    };
  }

  private void cancelSelectionOfAnAccount() {
    baseEvent.getTextChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "statsTeamAnalysisSelectionTimeOut")).queue();
  }
  
}
