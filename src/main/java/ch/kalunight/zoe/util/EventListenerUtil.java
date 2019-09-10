package ch.kalunight.zoe.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.static_data.CustomEmote;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.RichPresence;

public class EventListenerUtil {

  private static final List<String> inGameTranslationList = new ArrayList<>();
  
  static {
    inGameTranslationList.add("In Game");
    inGameTranslationList.add("En jeu");
    inGameTranslationList.add("Em partida");
    inGameTranslationList.add("Oyunda");
  }
  
  private EventListenerUtil() {
    //Hide public constructor
  }
  
  
  public static boolean checkIfRichPresenceIsInGame(RichPresence richPresence) {
    return inGameTranslationList.contains(richPresence.getState()) 
        || (richPresence.getLargeImage() != null && richPresence.getLargeImage().getKey() != null);
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

}
