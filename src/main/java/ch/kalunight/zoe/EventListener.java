package ch.kalunight.zoe;

import java.io.IOException;
import java.util.List;
import java.util.TimerTask;

import org.discordbots.api.client.DiscordBotListAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.command.CommandUtil;
import ch.kalunight.zoe.model.ControlPannel;
import ch.kalunight.zoe.model.CustomEmote;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.SpellingLangage;
import ch.kalunight.zoe.service.GameChecker;
import ch.kalunight.zoe.util.EventListenerUtil;
import ch.kalunight.zoe.util.Ressources;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Icon;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.GuildController;
import net.rithms.riot.api.RiotApiException;

public class EventListener extends ListenerAdapter {

  private static final int WAIT_TIME_BETWEEN_EACH_REFRESH_IN_MS = 10000;
  
  private static final String WELCOME_MESSAGE = "Hi! Thank you for adding me! To get help on my configuration type the command `>setup`. "
      + "If you want to see all commands i have, type >`help`";
  
  private static Logger logger = LoggerFactory.getLogger(EventListener.class);

  @Override
  public void onReady(ReadyEvent event) {
    Zoe.getJda().getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
    Zoe.getJda().getPresence().setGame(Game.playing("Booting ..."));
    
    setupNonInitializedGuild();
    
    logger.info("Chargements des champions ...");
    try {
      Zoe.loadChampions();
    } catch(IOException e1) {
      logger.error("Erreur lors du chargement des champions !");
      System.exit(1);
    }
    
    logger.info("Chargements des champions terminé !");
    
    logger.info("Chargements des emotes ...");
    try {
      EventListenerUtil.loadCustomEmotes();
      logger.info("Chargements des emotes terminé !");
    } catch(IOException e) {
      logger.error("Erreur lors du chargment des emotes : {}", e.getMessage());
    }
    
    logger.info("Chargement des sauvegardes détaillés ...");
    try {
      Zoe.loadDataTxt();
    } catch(IOException e) {
      logger.error(e.getMessage());
      logger.info("Une erreur est survenu lors du chargement des sauvegardes détaillés !");
    } catch(RiotApiException e) {
      logger.error(e.getMessage());
      logger.info("Une erreur venant de l'api Riot est survenu lors du chargement des sauvegardes détaillés !");
    }

    logger.info("Chargement des sauvegardes détaillés terminé !");
    
    logger.info("Loading of DiscordBotList API ...");
    
    try {
    Zoe.setBotListApi(new DiscordBotListAPI.Builder()
        .botId(Zoe.getJda().getSelfUser().getId())
        .token("Tocken") //SET TOCKEN
        .build());
    }catch(Exception e) {
      //Tocken not implement
    }
    
    logger.info("Loading of DiscordBotList API finished !");
    
    logger.info("Démarrage des tâches continue...");

    setupContinousRefreshThread();

    logger.info("Démarrage des tâches continues terminés !");

    Zoe.getJda().getPresence().setStatus(OnlineStatus.ONLINE);
    Zoe.getJda().getPresence().setGame(Game.playing("type \">help\""));
    logger.info("Démarrage terminés !");
  }

  private void setupNonInitializedGuild() {
    for(Guild guild : Zoe.getJda().getGuilds()) {
      if(!guild.getOwnerId().equals(Zoe.getJda().getSelfUser().getId())) {
        Server server = ServerData.getServers().get(guild.getId());
        
        if(server == null) {
          ServerData.getServers().put(guild.getId(), new Server(guild, SpellingLangage.EN));
        }
      }
    }
  }

  private void setupContinousRefreshThread() {
    TimerTask mainThread = new GameChecker();
    ServerData.getMainThreadTimer().schedule(mainThread, 0, WAIT_TIME_BETWEEN_EACH_REFRESH_IN_MS);
    
    Runnable gameChecker = new GameChecker();
    Thread thread = new Thread(gameChecker, "Game-Checker-Thread");
    thread.start();
  }

  @Override
  public void onGuildJoin(GuildJoinEvent event) {
    
    if(!event.getGuild().getOwner().getUser().getId().equals(Zoe.getJda().getSelfUser().getId())) {
      ServerData.getServers().put(event.getGuild().getId(), new Server(event.getGuild(), SpellingLangage.EN));
      ServerData.getServersIsInTreatment().put(event.getGuild().getId(), false);
      CommandUtil.sendMessageInGuildOrAtOwner(event.getGuild(), WELCOME_MESSAGE);
      return;
    }

    List<CustomEmote> customeEmotesList = Zoe.getEmotesNeedToBeUploaded().poll();

    if(customeEmotesList == null) {
      logger.error("Pas d'emote à envoyer ! Suppression de la guild ...");

      if(event.getGuild().getOwner().getUser().equals(Zoe.getJda().getSelfUser())) {
        event.getGuild().delete().queue();
      }

    }else {

      try {
        sendAllEmotesInGuild(event, customeEmotesList);
      }catch(Exception e) {
        logger.warn("Error with emotes sending ! Guild will be deleted");
        logger.warn("Error : {}", e.getMessage());
        logger.info("Some of emotes will be probably disable");
        event.getGuild().delete().queue();
        return;
      }

      Ressources.getCustomEmotes().addAll(customeEmotesList);
      
      EventListenerUtil.assigneCustomEmotesToData();
      
      logger.info("New emote Guild \"{}\" initialized !", event.getGuild().getName());
    }
  }
  
  @Override
  public void onTextChannelDelete(TextChannelDeleteEvent event) {
    Server server = ServerData.getServers().get(event.getGuild().getId());
    if(server.getInfoChannel() != null && server.getInfoChannel().getId().equals(event.getChannel().getId())) {
      server.setControlePannel(new ControlPannel());
      server.setInfoChannel(null);
    }
  }

  private void sendAllEmotesInGuild(GuildJoinEvent event, List<CustomEmote> customeEmotesList) {
    GuildController guildController = event.getGuild().getController();

    for(CustomEmote customEmote : customeEmotesList) {
      try {
        Icon icon;
        icon = Icon.from(customEmote.getFile());

        Emote emote = guildController.createEmote(customEmote.getName(), icon, event.getGuild().getPublicRole()).complete();

        customEmote.setEmote(emote);
      } catch (IOException e) {
        logger.warn("Impossible de charger l'image !");
      }
    }
  }
}
