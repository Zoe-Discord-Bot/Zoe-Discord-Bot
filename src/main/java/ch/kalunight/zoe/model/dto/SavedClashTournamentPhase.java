package ch.kalunight.zoe.model.dto;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

import no.stelar7.api.r4j.pojo.lol.clash.ClashTournamentPhase;

public class SavedClashTournamentPhase {

  private int id;
  private long registrationTime;
  private long startTime;
  private boolean cancelled;
  
  public SavedClashTournamentPhase(ClashTournamentPhase phase) {
    id = phase.getId();
    registrationTime = phase.getRegistrationTime();
    startTime = phase.getStartTime();
    cancelled = phase.isCancelled();
  }
  
  public int getId() {
    return id;
  }

  public long getRegistrationTime() {
    return registrationTime;
  }

  public ZonedDateTime getRegistrationTimeAsDate() {
    return ZonedDateTime.ofInstant(Instant.ofEpochMilli(this.registrationTime), ZoneOffset.UTC);
  }

  public long getStartTime() {
    return startTime;
  }

  public ZonedDateTime getStartTimeAsDate() {
    return ZonedDateTime.ofInstant(Instant.ofEpochMilli(this.startTime), ZoneOffset.UTC);
  }

  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SavedClashTournamentPhase that = (SavedClashTournamentPhase) o;
    return id == that.id &&
        registrationTime == that.registrationTime &&
        startTime == that.startTime &&
        cancelled == that.cancelled;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, registrationTime, startTime, cancelled);
  }

  @Override
  public String toString() {
    return "ClashTournamentPhase{" +
        "id=" + id +
        ", registrationTime=" + registrationTime +
        ", startTime=" + startTime +
        ", cancelled=" + cancelled +
        '}';
  }
}
