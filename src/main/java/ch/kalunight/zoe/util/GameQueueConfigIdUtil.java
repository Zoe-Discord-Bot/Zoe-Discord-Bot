package ch.kalunight.zoe.util;

import java.util.ArrayList;
import java.util.List;

import no.stelar7.api.r4j.basic.constants.types.lol.GameQueueType;

public class GameQueueConfigIdUtil {

  private static final List<GameQueueType> ALL_SOLOQ_TYPE = new ArrayList<>();
  private static final List<GameQueueType> ALL_FLEX_TYPE = new ArrayList<>();
  private static final List<GameQueueType> ALL_TFT_TYPE = new ArrayList<>();
  
  private GameQueueConfigIdUtil() {
    // hide default public constructor
  }
  
  static {
    ALL_SOLOQ_TYPE.add(GameQueueType.RANKED_SOLO_5X5);
    ALL_SOLOQ_TYPE.add(GameQueueType.TEAM_BUILDER_RANKED_SOLO);
    
    ALL_FLEX_TYPE.add(GameQueueType.TEAM_BUILDER_DRAFT_RANKED_5X5);
    ALL_FLEX_TYPE.add(GameQueueType.RANKED_FLEX_SR);
    
    ALL_TFT_TYPE.add(GameQueueType.TEAMFIGHT_TACTICS_RANKED);
  }

  public static List<GameQueueType> getAllSoloqType() {
    return ALL_SOLOQ_TYPE;
  }

  public static List<GameQueueType> getAllFlexType() {
    return ALL_FLEX_TYPE;
  }

  public static List<GameQueueType> getAllTftType() {
    return ALL_TFT_TYPE;
  }
}
