package ch.kalunight.zoe;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.GuildController;
import net.rithms.riot.api.RiotApiException;

public class EventListener extends ListenerAdapter {

  private static Logger logger = LoggerFactory.getLogger(EventListener.class);

  @Override
  public void onReady(ReadyEvent event) {
    ZoeMain.getJda().getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
    ZoeMain.getJda().getPresence().setGame(Game.playing("Booting ..."));
    logger.info("Chargements des champions ...");
    try {
      ZoeMain.loadChampions();
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
      ZoeMain.loadDataTxt();
    } catch(IOException e) {
      logger.error(e.getMessage());
      logger.info("Une erreur est survenu lors du chargement des sauvegardes détaillés !");
    } catch(RiotApiException e) {
      logger.error(e.getMessage());
      logger.info("Une erreur venant de l'api Riot est survenu lors du chargement des sauvegardes détaillés !");
    }

    logger.info("Chargement des sauvegardes détaillés terminé !");
    logger.info("Démarrage des tâches continue...");

    setupContinousRefreshThread();

    logger.info("Démarrage des tâches continues terminés !");

    ZoeMain.getJda().getPresence().setStatus(OnlineStatus.ONLINE);
    ZoeMain.getJda().getPresence().setGame(Game.playing("For help type \">help\""));
    logger.info("Démarrage terminés !");
  }

  private void setupContinousRefreshThread() {
    Runnable gameChecker = new GameChecker();
    Thread thread = new Thread(gameChecker, "Game-Checker-Thread");
    thread.start();
  }

  @Override
  public void onGuildJoin(GuildJoinEvent event) {
    
    if(!event.getGuild().getOwner().getUser().getId().equals(ZoeMain.getJda().getSelfUser().getId())) {
      ServerData.getServers().put(event.getGuild().getId(), new Server(event.getGuild(), SpellingLangage.EN));
      return;
    }

    List<CustomEmote> customeEmotesList = ZoeMain.getEmotesNeedToBeUploaded().poll();

    if(customeEmotesList == null) {
      logger.error("Pas d'emote à envoyer ! Suppression de la guild ...");

      if(event.getGuild().getOwner().getUser().equals(ZoeMain.getJda().getSelfUser())) {
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

      try {
        updateFileSave(event.getGuild());
      } catch(IOException e) {
        logger.warn("Impossible to save the new Guild ! Guild will be deleted");
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
  public void onGuildLeave(GuildLeaveEvent event) {
    ServerData.getServers().put(event.getGuild().getId(), null);
  }

  private synchronized void updateFileSave(Guild guildToAdd) throws IOException {
    List<Guild> emotesGuild = new ArrayList<>();

    try(BufferedReader reader = new BufferedReader(new FileReader(Ressources.GUILD_EMOTES_FILE));){
      int numberOfGuild;
      numberOfGuild = Integer.parseInt(reader.readLine());

      for(int i = 0; i < numberOfGuild; i++) {
        emotesGuild.add(ZoeMain.getJda().getGuildById(reader.readLine()));
      }
    }

    emotesGuild.add(guildToAdd);

    StringBuilder saveString = new StringBuilder();
    saveString.append(Integer.toString(emotesGuild.size()) + "\n");

    for(Guild guild : emotesGuild) {
      saveString.append(guild.getId() + "\n");
    }

    try(PrintWriter writer = new PrintWriter(Ressources.GUILD_EMOTES_FILE, "UTF-8");){
      writer.write(saveString.toString());
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
