package ch.kalunight.zoe.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.CustomEmote;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Guild;

public class EventListenerUtil {

  public static void loadCustomEmotes() throws IOException {
    List<Emote> uploadedEmotes = getAllGuildCustomEmotes();
    List<CustomEmote> picturesInFile = CustomEmoteUtil.loadPicturesInFile();

    assigneAlreadyUploadedEmoteToPicturesInFile(uploadedEmotes, picturesInFile);

    List<CustomEmote> emotesNeedToBeUploaded = getEmoteNeedToBeUploaded(picturesInFile);

    CustomEmoteUtil.prepareUploadOfEmotes(emotesNeedToBeUploaded);

    List<CustomEmote> emoteAlreadyUploded = getEmoteAlreadyUploaded(picturesInFile);

    Ressources.setCustomEmotes(emoteAlreadyUploded);

    assigneCustomEmotesToData();
  }

  private static List<CustomEmote> getEmoteAlreadyUploaded(List<CustomEmote> picturesInFile) {
    List<CustomEmote> emoteAlreadyUploded = new ArrayList<>();

    for(CustomEmote customEmote : picturesInFile) {
      if(customEmote.getEmote() != null) {
        emoteAlreadyUploded.add(customEmote);
      }
    }
    return emoteAlreadyUploded;
  }

  private static List<CustomEmote> getEmoteNeedToBeUploaded(List<CustomEmote> picturesInFile) {
    List<CustomEmote> emotesNeedToBeUploaded = new ArrayList<>();

    for(CustomEmote customEmote : picturesInFile) {
      if(customEmote.getEmote() == null) {
        emotesNeedToBeUploaded.add(customEmote);
      }
    }
    return emotesNeedToBeUploaded;
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

  private static List<Emote> getAllGuildCustomEmotes() {
    List<Emote> uploadedEmotes = new ArrayList<>();
    List<Guild> listGuild = Zoe.getJda().getGuilds();

    for(Guild guild : listGuild) {

      if(guild.getOwnerId().equals(Zoe.getJda().getSelfUser().getId())) {
        uploadedEmotes.addAll(guild.getEmotes());
      }
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
