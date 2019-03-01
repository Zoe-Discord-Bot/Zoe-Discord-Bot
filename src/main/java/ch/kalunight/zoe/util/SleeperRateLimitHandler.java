package ch.kalunight.zoe.util;

import net.rithms.riot.api.request.ratelimit.DefaultRateLimitHandler;
import net.rithms.riot.api.request.ratelimit.RespectedRateLimitException;
import java.util.ArrayList;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.rithms.riot.api.request.Request;

public class SleeperRateLimitHandler extends DefaultRateLimitHandler {

  private static final int MAX_REQUEST_BY_SECONDS = 20;

  private static final int MAX_REQUEST_BY_MINUTES = 50;

  private static final ArrayList<DateTime> secondList = new ArrayList<>();

  private static final ArrayList<DateTime> minuteList = new ArrayList<>();

  private static final Logger logger = LoggerFactory.getLogger(SleeperRateLimitHandler.class);

  @Override
  public void onRequestAboutToFire(Request request) throws RespectedRateLimitException {

    synchronized(secondList) {
      DateTime actualTime = DateTime.now();

      boolean fireableRequest = false;
      while(!fireableRequest) {

        fireableRequest = true;
        deleteOutOfRateLimiteTime(actualTime);

        if(minuteList.size() >= MAX_REQUEST_BY_MINUTES || secondList.size() >= MAX_REQUEST_BY_SECONDS) {
          fireableRequest = false;
          try {
            secondList.wait();
          } catch(InterruptedException e) {
            logger.error(e.getMessage());
            Thread.currentThread().interrupt();
          }
        }
        actualTime = DateTime.now();
      }
      secondList.add(actualTime);
      minuteList.add(actualTime);
    }

    logger.info("Request Launch : {}", request.getObject().getUrl());

    super.onRequestAboutToFire(request);
  }

  private void deleteOutOfRateLimiteTime(DateTime actualTime) {
    ArrayList<Integer> listToDelete = new ArrayList<>();

    for(int i = 0; i < secondList.size(); i++) {
      if(secondList.get(i).isBefore(actualTime.plusSeconds(-1))) {
        listToDelete.add(i);
      }
    }

    for(int i = listToDelete.size(); i > 0; i--) {
      secondList.remove((int) listToDelete.get(i - 1));
    }

    listToDelete = new ArrayList<>();

    for(int i = 0; i < minuteList.size(); i++) {
      if(minuteList.get(i).isBefore(actualTime.plusMinutes(-1))) {
        listToDelete.add(i);
      }
    }

    for(int i = listToDelete.size(); i > 0; i--) {
      minuteList.remove((int) listToDelete.get(i - 1));
    }
  }
}
