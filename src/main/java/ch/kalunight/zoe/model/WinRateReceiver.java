package ch.kalunight.zoe.model;

import java.util.concurrent.atomic.AtomicInteger;

public class WinRateReceiver {
  public final AtomicInteger win = new AtomicInteger(0);
  public final AtomicInteger loose = new AtomicInteger(0);
}
