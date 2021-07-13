package ch.kalunight.zoe.util;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.menu.SelectionDialog;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.LanguageCommandRunnable;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.static_data.CustomEmote;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.ServerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.RichPresence;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.rithms.riot.constant.Platform;

public class EventListenerUtil {

  private static final Logger logger = LoggerFactory.getLogger(EventListenerUtil.class);
  
  private EventListenerUtil() {
    //Hide public constructor
  }

  public static boolean checkIfIsGame(RichPresence richPresence) {

    boolean clientOpen = false;

    if(richPresence.getName() != null && richPresence.getName().equals("League of Legends")){
      clientOpen = true;
    }

    if(clientOpen) {
      return richPresence.getTimestamps() != null && (richPresence.getLargeImage() == null || richPresence.getLargeImage().getText() != null);
    }
    return false;
  }


  public static void loadCustomEmotes() throws IOException {
    Ressources.getCustomEmotes().clear();
    Ressources.getMasteryEmote().clear();
    Ressources.getTierEmote().clear();
    List<Emote> uploadedEmotes = getAllGuildCustomEmotes();
    List<CustomEmote> picturesInFile = CustomEmoteUtil.loadPicturesInFile();

    assigneAlreadyUploadedEmoteToPicturesInFile(uploadedEmotes, picturesInFile);

    Ressources.getCustomEmotes().addAll(picturesInFile);
    assigneCustomEmotesToData();
  }

  private static void assigneAlreadyUploadedEmoteToPicturesInFile(List<Emote> uploadedEmotes, List<CustomEmote> picturesInFile) {
    for(CustomEmote customeEmote : picturesInFile) {
      for(Emote emote : uploadedEmotes) {
        if(emote.getName().equalsIgnoreCase(customeEmote.getName())) {
          customeEmote.setEmote(emote);
        }
      }
    }
  }

  private static List<Emote> getAllGuildCustomEmotes() throws IOException {
    List<Emote> uploadedEmotes = new ArrayList<>();
    List<Guild> listGuild = new ArrayList<>();

    try(final BufferedReader reader = new BufferedReader(new FileReader(Ressources.GUILD_EMOTES_FILE));) {
      String line;
      while((line = reader.readLine()) != null) {
        listGuild.add(Zoe.getGuildById(line));
      }
    }

    for(Guild guild : listGuild) {
      uploadedEmotes.addAll(guild.retrieveEmotes().complete());
    }
    return uploadedEmotes;
  }

  public static void assigneCustomEmotesToData() {
    for(CustomEmote emote : Ressources.getCustomEmotes()) {
      CustomEmoteUtil.addToChampionIfIsSame(emote);
      CustomEmoteUtil.addToTierIfisSame(emote);
      CustomEmoteUtil.addToMasteryIfIsSame(emote);
      CustomEmoteUtil.addInfoIconIfSame(emote);
    }
  }

  public static BiConsumer<Message, Integer> getSelectionDoneActionLangueSelection(List<String> languageList,
      DTO.Server server, MessageChannel channel) {
    return new BiConsumer<Message, Integer>() {
      @Override
      public void accept(Message selectionMessage, Integer selectionOfLanguage) {

        try {
          selectionMessage.clearReactions().queue();
        }catch (IllegalStateException | InsufficientPermissionException e){
          //Exception Ok, appear in private message or when missing permissions.
        }
        server.setLanguage(languageList.get(selectionOfLanguage - 1));
        
        try {
          ServerRepository.updateLanguage(server.serv_guildId, server.getLanguage());
        } catch(SQLException e) {
          logger.error("SQL error when updating the language in guild joining setup");
          channel.sendMessage("Issue when updating the language."
              + " Please try with the command `>language`. I will now continue to talk in your language.").queue();
        }

        selectionMessage.getChannel().sendMessage(String.format(LanguageManager.getText(server.getLanguage(), "addingSystemLanguageSelected"),
            LanguageManager.getText(server.getLanguage(), LanguageCommandRunnable.NATIVE_LANGUAGE_TRANSLATION_ID))).queue();


        SelectionDialog.Builder selectAccountBuilder = new SelectionDialog.Builder()
            .setEventWaiter(Zoe.getEventWaiter())
            .useLooping(true)
            .setColor(Color.GREEN)
            .setSelectedEnds("**", "**")
            .setCanceled(getSelectionCancelAction())
            .setTimeout(15, TimeUnit.MINUTES);

        List<Platform> regionsList = new ArrayList<>();
        List<String> regionChoices = new ArrayList<>();
        for(Platform regionMember : Platform.values()) {
          String actualChoice = String.format(LanguageManager.getText(server.getLanguage(), "regionOptionRegionChoice"),
              regionMember.getName().toUpperCase());

          regionChoices.add(actualChoice);
          selectAccountBuilder.addChoices(actualChoice);
          regionsList.add(regionMember);
        }

        String anyChoice = LanguageManager.getText(server.getLanguage(), "regionOptionDisableChoice");
        regionChoices.add(anyChoice);
        selectAccountBuilder.addChoices(anyChoice);

        selectAccountBuilder.setText(getUpdateMessageRegionAfterChangeSelectAction(server.getLanguage(), regionChoices));
        selectAccountBuilder.setSelectionConsumer(getSelectionDoneActionRegionSelection(regionsList, server));

        SelectionDialog dialog = selectAccountBuilder.build();
        dialog.display(channel);
      }
    };
  }

  private static Function<Integer, String> getUpdateMessageRegionAfterChangeSelectAction(String language, List<String> choices) {
    return new Function<Integer, String>() {
      @Override
      public String apply(Integer index) {
        if(choices.size() == index) {
          return LanguageManager.getText(language, "regionOptionInSelectionAny");
        }

        return String.format(LanguageManager.getText(language, "regionOptionInSelectionRegion"), choices.get(index - 1));
      }
    };
  }

  private static BiConsumer<Message, Integer> getSelectionDoneActionRegionSelection(List<Platform> regionsList, DTO.Server server) {
    return new BiConsumer<Message, Integer>() {
      @Override
      public void accept(Message selectionMessage, Integer selectionOfRegion) {

        try {
          selectionMessage.clearReactions().queue();
        }catch (IllegalStateException | InsufficientPermissionException e){
          //Exception Ok, appear in private message or when missing permissions.
        }

        try {

          String strRegion;
          if(regionsList.size() == selectionOfRegion - 1) {
            strRegion = LanguageManager.getText(server.getLanguage(), "regionOptionAnyRegion");
            ConfigRepository.updateRegionOption(server.serv_guildId, null, selectionMessage.getJDA());
          } else {
            strRegion = regionsList.get(selectionOfRegion - 1).getName().toUpperCase();
            ConfigRepository.updateRegionOption(server.serv_guildId, regionsList.get(selectionOfRegion - 1), selectionMessage.getJDA());
          }

          selectionMessage.getChannel().sendMessage(String.format(
              LanguageManager.getText(server.getLanguage(), "addingSystemRegionSelected")
              + "\n\n" + LanguageManager.getText(server.getLanguage(), "setupMessage")
              + "\n\n" + LanguageManager.getText(server.getLanguage(), "addingSystemEndMessage"),
              strRegion)).queue();
        }catch(SQLException e) {
          logger.error("SQL error when updating region when joining guild !", e);
          selectionMessage.getChannel().sendMessage("I got a issue when updating the option. Please retry with the command `>config`")
          .queue();
        }
      }
    };
  }

  private static Consumer<Message> getSelectionCancelAction(){
    return new Consumer<Message>() {
      @Override
      public void accept(Message message) {
        message.clearReactions().queue();
      }
    };
  }
}
