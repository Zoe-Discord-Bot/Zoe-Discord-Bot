package ch.kalunight.zoe.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.player_data.Tier;
import ch.kalunight.zoe.model.static_data.Champion;
import ch.kalunight.zoe.model.static_data.CustomEmote;
import ch.kalunight.zoe.model.static_data.Mastery;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;

public class CustomEmoteUtil {

  private CustomEmoteUtil() {
    // Hide default public constructor
  }

  private static final int MAX_EMOTE_BY_GUILD = 50;

  public static List<CustomEmote> loadPicturesInFile() {
    List<CustomEmote> emotes = new ArrayList<>();

    File folder = new File(Ressources.FOLDER_TO_EMOTES);
    File[] listOfFiles = folder.listFiles();

    for(int i = 0; i < listOfFiles.length; i++) {
      String name = listOfFiles[i].getName();
      if(name.endsWith(".png") || name.endsWith(".jpg")) {
        name = name.substring(0, name.length() - 4);
        emotes.add(new CustomEmote(name, listOfFiles[i]));
      }
    }
    return emotes;
  }

  public static List<CustomEmote> prepareUploadOfEmotes(List<CustomEmote> customEmotes, JDA jda) throws IOException {

    List<Guild> emoteGuilds = getEmoteGuilds(jda);

    List<CustomEmote> uploadedEmote = uploadEmoteInGuildAlreadyExist(customEmotes, emoteGuilds);
    if(!uploadedEmote.isEmpty()) {
      Ressources.getCustomEmotes().addAll(uploadedEmote);
    }

    createNewGuildsWithAssignedEmotes(customEmotes, emoteGuilds, jda);

    return customEmotes;
  }

  private static void createNewGuildsWithAssignedEmotes(List<CustomEmote> customEmotes, List<Guild> emoteGuilds, JDA jda) {
    int j = 0;

    while(!customEmotes.isEmpty()) {
      jda.createGuild("Zoe Emotes Guild " + (emoteGuilds.size() + j)).complete();
      j++;

      List<CustomEmote> listEmoteForCreatedGuild = new ArrayList<>();

      int numberOfEmoteForNewGuild = 0;

      while(numberOfEmoteForNewGuild < MAX_EMOTE_BY_GUILD && !customEmotes.isEmpty()) {
        listEmoteForCreatedGuild.add(customEmotes.get(0));
        customEmotes.remove(0);
        numberOfEmoteForNewGuild++;
      }

      Zoe.getEmotesNeedToBeUploaded().add(listEmoteForCreatedGuild);
    }
  }

  private static List<CustomEmote> uploadEmoteInGuildAlreadyExist(List<CustomEmote> customEmotes, List<Guild> emoteGuilds)
      throws IOException {
    List<CustomEmote> emotesUploaded = new ArrayList<>();

    for(Guild guild : emoteGuilds) {
      List<Emote> emotes = getNonAnimatedEmoteOfTheGuild(guild);

      int emotesSize = emotes.size();

      while(emotesSize < MAX_EMOTE_BY_GUILD && !customEmotes.isEmpty()) {
        CustomEmote customEmote = customEmotes.get(0);
        Icon icon = Icon.from(customEmote.getFile());
        Emote emote = guild.createEmote(customEmote.getName(), icon, guild.getPublicRole()).complete();

        emotesSize++;

        customEmote.setEmote(emote);
        emotesUploaded.add(customEmote);
        customEmotes.remove(0);
      }
    }
    return emotesUploaded;
  }

  private static List<Emote> getNonAnimatedEmoteOfTheGuild(Guild guild) {
    List<Emote> emotes = guild.getEmotes();

    List<Emote> emotesNonAnimated = new ArrayList<>();
    for(Emote emote : emotes) {
      if(!emote.isAnimated()) {
        emotesNonAnimated.add(emote);
      }
    }
    return emotes;
  }

  private static List<Guild> getEmoteGuilds(JDA jda) throws IOException {
    List<Guild> emoteGuild = new ArrayList<>();

    for(Guild guild : jda.getGuilds()) {
      if(guild.getOwnerId().equals(jda.getSelfUser().getId())) {
        emoteGuild.add(guild);
      }
    }
    return emoteGuild;
  }

  public static void addToMasteryIfIsSame(CustomEmote emote) {
    for(Mastery mastery : Mastery.values()) {
      if(mastery.getName().equalsIgnoreCase(emote.getName())) {
        Ressources.getMasteryEmote().put(mastery, emote);
      }
    }
  }

  public static void addToTierIfisSame(CustomEmote emote) {
    for(Tier tier : Tier.values()) {
      if(tier.getTranslationTag().equalsIgnoreCase(emote.getName())) {
        Ressources.getTierEmote().put(tier, emote);
      }
    }
  }

  public static void addToChampionIfIsSame(CustomEmote emote) {
    for(Champion champion : Ressources.getChampions()) {
      if(champion.getId().equals(emote.getName())) {
        champion.setEmote(emote.getEmote());
      }
    }
  }

  public static void addInfoIconIfSame(CustomEmote emote) {
    if(emote.getEmote() == null) {
      return;
    }
    
    if(emote.getEmote().getName().equalsIgnoreCase("smallGreenTriangle")) {
      Ressources.setGreenTriangleEmote(emote);
      return;
    }
    
    if(emote.getEmote().getName().equalsIgnoreCase("ZoeMatchInWait")) {
      Ressources.setGameToDo(emote);
      return;
    }
    
    if(emote.getEmote().getName().equalsIgnoreCase("ZoeMatchLose")) {
      Ressources.setGameLost(emote);
    }
  }
}
