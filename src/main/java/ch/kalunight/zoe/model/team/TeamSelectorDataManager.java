package ch.kalunight.zoe.model.team;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.TextChannel;

public abstract class TeamSelectorDataManager implements Runnable {

  protected static final Logger logger = LoggerFactory.getLogger(TeamSelectorDataManager.class);

  protected List<AccountDataWithRole> accountsToTreat;

  protected Server server;
  
  protected TextChannel channel;
  
  public TeamSelectorDataManager(Server server, TextChannel channel) {
    this.accountsToTreat = null;
    this.server = server;
    this.channel = channel;
  }

  @Override
  public void run() {
    try {
      if(accountsToTreat != null) {
        treatData();
      }else {
        logger.error("Accounts data missing ! We can't process them.");
        channel.sendMessage(LanguageManager.getText(server.getLanguage(), "statsProfileUnexpectedError")).queue();
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
