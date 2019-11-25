package ch.kalunight.zoe.util;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import com.jagrosh.jdautilities.menu.SelectionDialog;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.LanguageCommand;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.static_data.CustomEmote;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.RichPresence;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.rithms.riot.constant.Platform;

public class EventListenerUtil {
  
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
        listGuild.add(Zoe.getJda().getGuildById(line));
      }
    }

    for(Guild guild : listGuild) {
      uploadedEmotes.addAll(guild.getEmotes());
    }
    return uploadedEmotes;
  }

  public static void assigneCustomEmotesToData() {
    for(CustomEmote emote : Ressources.getCustomEmotes()) {
      CustomEmoteUtil.addToChampionIfIsSame(emote);
      CustomEmoteUtil.addToTierIfisSame(emote);
      CustomEmoteUtil.addToMasteryIfIsSame(emote);
    }
  }
  
  public static BiConsumer<Message, Integer> getSelectionDoneAction(List<String> languageList, Server server, MessageChannel channel) {
    return new BiConsumer<Message, Integer>() {
      @Override
      public void accept(Message selectionMessage, Integer selectionOfLanguage) {

        try {
        selectionMessage.clearReactions().queue();
        }catch (IllegalStateException | InsufficientPermissionException e){
          //Exception Ok, appear in private message or when missing permissions.
        }
        server.setLangage(languageList.get(selectionOfLanguage - 1));
        
        selectionMessage.getChannel().sendMessage(String.format(LanguageManager.getText(server.getLangage(), "addingSystemLanguageSelected"),
            LanguageManager.getText(server.getLangage(), LanguageCommand.NATIVE_LANGUAGE_TRANSLATION_ID))).queue();
        
        
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
          String actualChoice = String.format(LanguageManager.getText(server.getLangage(), "regionOptionRegionChoice"),
              regionMember.getName().toUpperCase());
          
          regionChoices.add(actualChoice);
          selectAccountBuilder.addChoices(actualChoice);
          regionsList.add(regionMember);
        }
        
        String anyChoice = LanguageManager.getText(server.getLangage(), "regionOptionDisableChoice");
        regionChoices.add(anyChoice);
        selectAccountBuilder.addChoices(anyChoice);

        selectAccountBuilder.setText(getUpdateMessageAfterChangeSelectAction(server.getLangage(), regionChoices));
        selectAccountBuilder.setSelectionConsumer(getSelectionDoneAction(server.getLangage(), regionsList, server));

        SelectionDialog dialog = selectAccountBuilder.build();
        dialog.display(channel);
      }
    };
  }
  
  private static Function<Integer, String> getUpdateMessageAfterChangeSelectAction(String language, List<String> choices) {
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
  
  private static BiConsumer<Message, Integer> getSelectionDoneAction(String language, List<Platform> regionsList, Server server) {
    return new BiConsumer<Message, Integer>() {
      @Override
      public void accept(Message selectionMessage, Integer selectionOfRegion) {

        try {
        selectionMessage.clearReactions().queue();
        }catch (IllegalStateException | InsufficientPermissionException e){
          //Exception Ok, appear in private message or when missing permissions.
        }
        
        String strRegion;
        if(regionsList.size() == selectionOfRegion - 1) {
          strRegion = LanguageManager.getText(language, "regionOptionAnyRegion");
          server.getConfig().getDefaultRegion().setRegion(null);
        } else {
          strRegion = regionsList.get(selectionOfRegion - 1).getName().toUpperCase();
          server.getConfig().getDefaultRegion().setRegion(regionsList.get(selectionOfRegion - 1));
        }

        selectionMessage.getChannel().sendMessage(String.format(LanguageManager.getText(language, "addingSystemRegionSelected")
            + "\n\n" + LanguageManager.getText(language, "setupMessage")
            + "\n\n" + LanguageManager.getText(language, "addingSystemEndMessage"),
            strRegion)).queue();
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
