package ch.kalunight.zoe.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.bson.codecs.pojo.annotations.BsonIgnore;

public class SavedClashTournamentPhaseUtil {

  private SavedClashTournamentPhaseUtil() {
    // hide default public constructor
  }
  
  @BsonIgnore
  public static ZonedDateTime convertTimestampToZone(long timestamp) {
    return ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC);
  }
}
