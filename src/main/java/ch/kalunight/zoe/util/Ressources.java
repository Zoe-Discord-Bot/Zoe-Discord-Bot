package ch.kalunight.zoe.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import ch.kalunight.zoe.model.Champion;
import ch.kalunight.zoe.model.CustomEmote;
import ch.kalunight.zoe.model.Mastery;
import ch.kalunight.zoe.model.Tier;

public class Ressources {

  private Ressources() {
    // Hide default public constructor
  }

  public static final String FOLDER_TO_TIER_SAVE = "ressources/tierData/";

  public static final String FOLDER_TO_EMOTES = "ressources/images";

  public static final File GUILD_EMOTES_FILE = new File("ressources/guilds.txt");

  private static List<Champion> champions = new ArrayList<>();

  private static List<CustomEmote> customEmotes = new ArrayList<>();

  private static Map<Tier, CustomEmote> tierEmote = Collections.synchronizedMap(new EnumMap<Tier, CustomEmote>(Tier.class));

  private static Map<Mastery, CustomEmote> masteryEmote = Collections.synchronizedMap(new EnumMap<Mastery, CustomEmote>(Mastery.class));

  public static Champion getChampionDataById(int id) {
    for(Champion champion : champions) {
      if(champion.getKey() == id) {
        return champion;
      }
    }
    return null;
  }

  public static List<CustomEmote> getCustomEmotes() {
    return customEmotes;
  }

  public static void setCustomEmotes(List<CustomEmote> customEmotes) {
    Ressources.customEmotes = customEmotes;
  }

  public static List<Champion> getChampions() {
    return champions;
  }

  public static void setChampions(List<Champion> champions) {
    Ressources.champions = champions;
  }

  public static Map<Tier, CustomEmote> getTierEmote() {
    return tierEmote;
  }

  public static void setTierEmote(Map<Tier, CustomEmote> tierEmote) {
    Ressources.tierEmote = tierEmote;
  }

  public static Map<Mastery, CustomEmote> getMasteryEmote() {
    return masteryEmote;
  }

  public static void setMasteryEmote(Map<Mastery, CustomEmote> masteryEmote) {
    Ressources.masteryEmote = masteryEmote;
  }


}
