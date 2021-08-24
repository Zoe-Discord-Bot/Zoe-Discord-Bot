package ch.kalunight.zoe.model.team;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.SelectionDialog;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.create.CreatePlayerCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.dto.ZoePlatform;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.TeamUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import no.stelar7.api.r4j.basic.exceptions.APIHTTPErrorReason;
import no.stelar7.api.r4j.basic.exceptions.APIResponseException;
import no.stelar7.api.r4j.pojo.lol.clash.ClashPosition;
import no.stelar7.api.r4j.pojo.lol.summoner.Summoner;

public class TeamSelectorDataHandler {

  private static final String STATS_TEAM_ANALYSIS_CANCEL_MESSAGE_ID = "statsTeamAnalysisCancelMessage";

  private List<AccountDataWithRole> accountsSelected;
  
  private EventWaiter waiter;
  
  private Server server;
  
  private TextChannel channel;
  
  private Member author;
  
  private TeamSelectorDataManager dataManager;
  
  private ThreadPoolExecutor whereToExecute;
  
  public TeamSelectorDataHandler(EventWaiter waiter, Server server, TextChannel channel, Member author, TeamSelectorDataManager dataManager, ThreadPoolExecutor whereToExecute) {
    accountsSelected = Collections.synchronizedList(new ArrayList<>());
    this.waiter = waiter;
    this.server = server;
    this.channel = channel;
    this.author = author;
    this.dataManager = dataManager;
    this.whereToExecute = whereToExecute;
  }
  
  public void askSelectionAccount() {
    channel.sendMessage(String.format(LanguageManager.getText(server.getLanguage(), "statsTeamAnalysisAskAccount"), accountsSelected.size())
        + " " + LanguageManager.getText(server.getLanguage(), STATS_TEAM_ANALYSIS_CANCEL_MESSAGE_ID)).queue();
    
    defineAccountSelectionWaiter();
  }

  private void defineAccountSelectionWaiter() {
    waiter.waitForEvent(MessageReceivedEvent.class,
        e -> e.getAuthor().equals(author.getUser()) && e.getChannel().equals(channel),
        e -> threatAccountSelection(e), 3, TimeUnit.MINUTES,
        () -> cancelSelectionOfAnAccount());
  }

  private void threatAccountSelection(MessageReceivedEvent messageReceivedEvent) {
    String messageReceived = messageReceivedEvent.getMessage().getContentRaw();
    
    if(messageReceived.equalsIgnoreCase("Stop")) {
      messageReceivedEvent.getTextChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "statsTeamAnalysisRoleSelectionCancel")).queue();
      return;
    }
    
    List<String> listArgs = CreatePlayerCommandRunnable.getParameterInParenteses(messageReceived);
    
    if(listArgs.size() != 2) {
      messageReceivedEvent.getTextChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "statsTeamAnalysisMalformedAccount") 
          + " " + LanguageManager.getText(server.getLanguage(), STATS_TEAM_ANALYSIS_CANCEL_MESSAGE_ID)).queue();
      defineAccountSelectionWaiter();
      return;
    }

    String regionName = listArgs.get(0);
    String summonerName = listArgs.get(1);

    ZoePlatform region = CreatePlayerCommandRunnable.getPlatform(regionName);
    if(region == null) {
      messageReceivedEvent.getTextChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "statsTeamAnalysisRegionTagInvalid") 
          + " " + LanguageManager.getText(server.getLanguage(), STATS_TEAM_ANALYSIS_CANCEL_MESSAGE_ID)).queue();
      defineAccountSelectionWaiter();
      return;
    }

    Message messageLoading = messageReceivedEvent.getTextChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "loadingSummoner")).complete();
    Summoner summoner;
    try {
      summoner = Zoe.getRiotApi().getSummonerByName(region, summonerName);
      
    }catch(APIResponseException e) {
      if(e.getReason() == APIHTTPErrorReason.ERROR_404) {
        messageLoading.editMessage(LanguageManager.getText(server.getLanguage(), "statsTeamAnalysisSummonerNotFound") 
            + " " + LanguageManager.getText(server.getLanguage(), STATS_TEAM_ANALYSIS_CANCEL_MESSAGE_ID)).queue();
      }else {
        messageLoading.editMessage(LanguageManager.getText(server.getLanguage(), "statsTeamAnalysisRiotApi") 
            + " " + LanguageManager.getText(server.getLanguage(), STATS_TEAM_ANALYSIS_CANCEL_MESSAGE_ID)).queue();
      }
      defineAccountSelectionWaiter();
      return;
    }
    
    if(isSummonerAlreadySelected(summoner, region)) {
      messageLoading.editMessage(LanguageManager.getText(server.getLanguage(), "statsTeamAnalysisAlreadySelectedAccount")).queue();
      defineAccountSelectionWaiter();
      return;
    }
    
    defineRoleSelectionWaiter(region, summoner, messageLoading);
  }
  
  private void defineRoleSelectionWaiter(ZoePlatform platform, Summoner summoner, Message messageLoading) {
    
    SelectionDialog.Builder selectRoleBuilder = new SelectionDialog.Builder()
        .addUsers(author.getUser())
        .setEventWaiter(waiter)
        .useLooping(true)
        .setColor(Color.GREEN)
        .setSelectedEnds("**", "**")
        .setCanceled(getSelectionCancelAction(server.getLanguage(), accountsSelected.size()))
        .setText(LanguageManager.getText(server.getLanguage(), "statsTeamAnalysisAskRole"))
        .setTimeout(2, TimeUnit.MINUTES);
    
    List<ClashPosition> rolesOrder = new ArrayList<>();
    
    selectRoleBuilder.addChoices(LanguageManager.getText(server.getLanguage(), TeamUtil.getTeamPositionId(ClashPosition.FILL))
    + " " + LanguageManager.getText(server.getLanguage(), "statsTeamAnalysisRoleSelectionFillInfo"));
    rolesOrder.add(ClashPosition.FILL);
    
    for(ClashPosition roleToSelect : ClashPosition.values()) {
      if(roleToSelect != ClashPosition.UNSELECTED && roleToSelect != ClashPosition.FILL && !isRoleAlreadySelected(roleToSelect)) {
        selectRoleBuilder.addChoices(LanguageManager.getText(server.getLanguage(), TeamUtil.getTeamPositionId(roleToSelect)));
        rolesOrder.add(roleToSelect);
      }
    }
    
    selectRoleBuilder.setSelectionConsumer(roleSelectedDoneAction(rolesOrder, platform, summoner));
    
    selectRoleBuilder.build().display(messageLoading);
  }
  
  private BiConsumer<Message, Integer> roleSelectedDoneAction(List<ClashPosition> rolesOrder, ZoePlatform platform, Summoner summoner) {
    return new BiConsumer<Message, Integer>() {
      
      @Override
      public void accept(Message baseMessage, Integer seletedAnswerId) {
        baseMessage.getTextChannel().sendTyping().queue();
        baseMessage.clearReactions().queue();
        ClashPosition selected = rolesOrder.get(seletedAnswerId - 1);
        
        String summonerName = "*" + platform.getShowableName() + "* " + summoner.getName();
        
        baseMessage.getTextChannel().sendMessage(String.format(LanguageManager.getText(server.getLanguage(), "statsTeamAnalysisAccountCorrectlySelected"), summonerName,
            LanguageManager.getText(server.getLanguage(), TeamUtil.getTeamPositionId(selected)))).queue();
        
        accountsSelected.add(new AccountDataWithRole(summoner, platform, selected));
        manageAccountNeededOrNot();
      }
    };
  }
  
  private void manageAccountNeededOrNot() {
    if(accountsSelected.size() == 5) {
      channel.sendMessage("*" + LanguageManager.getText(server.getLanguage(), "clashAnalyzeLoadStarted") + "*").queue();
      dataManager.feedWithData(accountsSelected);
      whereToExecute.execute(dataManager);
    }else {
      askSelectionAccount();
    }
  }

  private Consumer<Message> getSelectionCancelAction(String language, int accountSelected){
    return new Consumer<Message>() {
      @Override
      public void accept(Message message) {
        if(accountSelected == accountsSelected.size()) {
          message.clearReactions().queue();
          message.editMessage(LanguageManager.getText(language, "statsTeamAnalysisRoleSelectionCancel")).queue();
        }
      }
    };
  }
  
  private boolean isSummonerAlreadySelected(Summoner summoner, ZoePlatform platform) {
    for(AccountDataWithRole toCheck : accountsSelected) {
      if(toCheck.getSummoner().getSummonerId().equals(summoner.getSummonerId()) && platform == toCheck.getPlatform()) {
        return true;
      }
    }
    return false;
  }
  
  private boolean isRoleAlreadySelected(ClashPosition positionToCheck) {
    for(AccountDataWithRole toCheck : accountsSelected) {
      if(toCheck.getPosition() == positionToCheck) {
        return true;
      }
    }
    return false;
  }

  private void cancelSelectionOfAnAccount() {
    channel.sendMessage(LanguageManager.getText(server.getLanguage(), "statsTeamAnalysisSelectionTimeOut")).queue();
  }
  
}
