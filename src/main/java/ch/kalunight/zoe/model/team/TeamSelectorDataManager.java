package ch.kalunight.zoe.model.team;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;

public abstract class TeamSelectorDataManager implements Runnable {

  protected static final Logger logger = LoggerFactory.getLogger(TeamSelectorDataManager.class);

  protected List<AccountDataWithRole> accountsToTreat;

  protected CommandEvent baseEvent;

  protected Server server;
  
  public TeamSelectorDataManager(CommandEvent event, Server server) {
    this.accountsToTreat = null;
    this.baseEvent = event;
    this.server = server;
  }

  @Override
  public void run() {
    try {
      if(accountsToTreat != null) {
        treatData();
      }else {
        logger.error("Accounts data missing ! We can't process them.");
        baseEvent.reply(LanguageManager.getText(server.getLanguage(), "statsProfileUnexpectedError"));
      }
    }catch (Exception e) {
      logger.error("Unexpected error in TeamSelectorDataManager !", e);
    }
  }
  
  public abstract void treatData();

  public void feedWithData(List<AccountDataWithRole> accountsToTreat) {
    this.accountsToTreat = accountsToTreat;
  }

}
