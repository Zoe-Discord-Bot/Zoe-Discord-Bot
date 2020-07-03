package ch.kalunight.zoe.model;

import java.util.concurrent.atomic.AtomicInteger;

public class KDAReceiver {
  public final AtomicInteger numberOfMatchs = new AtomicInteger(0);
  public final AtomicInteger kills = new AtomicInteger(0);
  public final AtomicInteger deaths = new AtomicInteger(0);
  public final AtomicInteger assists = new AtomicInteger(0);
}
