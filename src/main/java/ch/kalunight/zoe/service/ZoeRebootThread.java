package ch.kalunight.zoe.service;

import java.util.TimerTask;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.EventListener;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDA.Status;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;

public class ZoeRebootThread extends TimerTask {

  private static final Logger logger = LoggerFactory.getLogger(ZoeRebootThread.class);

  @Override
  public void run() {
    JDA jda = null;
    try {
      ServerData.clearAllTask();

      CommandClientBuilder client = new CommandClientBuilder();

      client.setOwnerId(Zoe.getClientOwnerID());

      client.setPrefix(Zoe.BOT_PREFIX);

      Zoe.setEventWaiter(new EventWaiter(ServerData.getResponseWaiter(), false));

      Zoe.setMainCommands(null);

      for(Command command : Zoe.getMainCommands(Zoe.getEventWaiter())) {
        client.addCommand(command);
      }

      Consumer<CommandEvent> helpCommand = CommandUtil.getHelpCommand();

      client.setHelpConsumer(helpCommand);

      CommandClient commandClient = client.build();

      EventListener eventListener = new EventListener();

      Zoe.getEventlistenerlist().clear();

      Zoe.getEventlistenerlist().add(commandClient);
      Zoe.getEventlistenerlist().add(Zoe.getEventWaiter());
      Zoe.getEventlistenerlist().add(eventListener);

      jda = new JDABuilder(AccountType.BOT)//
          .setToken(Zoe.getDiscordTocken())//
          .setStatus(OnlineStatus.ONLINE)//
          .addEventListeners(commandClient)//
          .addEventListeners(Zoe.getEventWaiter())//
          .addEventListeners(eventListener)
          .setAutoReconnect(false).build();//
      Zoe.setJda(jda);
      jda.awaitReady();

    } catch (Exception e) {
      logger.error("Catch unexpected exception when rebooting !", e);
    } finally {
      if (jda == null || !jda.getStatus().equals(Status.CONNECTED)){
        logger.info("Reboot has failed ! Try again in 10 secs ...");
        if(jda != null) {
          jda.shutdownNow();
        }
        TimerTask rebootTask = new ZoeRebootThread();
        ServerData.getServerCheckerThreadTimer().schedule(rebootTask, 10000);
      }else {
        logger.info("Zoe has rebooted correctly !");
      }
    }
  }
}
