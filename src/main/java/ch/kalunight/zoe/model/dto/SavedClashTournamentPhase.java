package ch.kalunight.zoe.model.dto;

import java.util.Objects;

import no.stelar7.api.r4j.pojo.lol.clash.ClashTournamentPhase;

public class SavedClashTournamentPhase {

  protected int id;
  protected long registrationTime;
  protected long startTime;
  protected boolean cancelled;
  
  public SavedClashTournamentPhase() {}
  
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

  public long getStartTime() {
    return startTime;
  }

  public boolean isCancelled() {
    return cancelled;
  }

  public void setId(int id) {
    this.id = id;
  }

  public void setRegistrationTime(long registrationTime) {
    this.registrationTime = registrationTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
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
