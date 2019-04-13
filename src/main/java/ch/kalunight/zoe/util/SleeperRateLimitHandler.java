package ch.kalunight.zoe.util;

import java.util.ArrayList;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.rithms.riot.api.request.Request;
import net.rithms.riot.api.request.RequestResponse;
import net.rithms.riot.api.request.ratelimit.DefaultRateLimitHandler;
import net.rithms.riot.api.request.ratelimit.RespectedRateLimitException;

public class SleeperRateLimitHandler extends DefaultRateLimitHandler {

  private static final int MAX_REQUEST_BY_SECONDS = 25; //Production api key got no seconds limits, Change the value with the rate of api dev key (10/50)

  private static final int MAX_REQUEST_BY_MINUTES = 1500;

  private static final ArrayList<DateTime> secondList = new ArrayList<>();

  private static final ArrayList<DateTime> minuteList = new ArrayList<>();

  private static final Logger logger = LoggerFactory.getLogger(SleeperRateLimitHandler.class);

  @Override
  public void onRequestAboutToFire(Request request) throws RespectedRateLimitException {

    synchronized(this) {
      DateTime actualTime = DateTime.now();

      secondList.add(actualTime);
      minuteList.add(actualTime);
      
      boolean fireableRequest = false;
      while(!fireableRequest) {

        fireableRequest = true;
        deleteOutOfRateLimiteTime(actualTime);

        if(minuteList.size() >= MAX_REQUEST_BY_MINUTES || secondList.size() >= MAX_REQUEST_BY_SECONDS) {
          fireableRequest = false;
          try {
            Thread.sleep(1000); //NOSONAR
          } catch(InterruptedException e) {
            logger.error(e.getMessage());
            Thread.currentThread().interrupt();
          }
        }
        actualTime = DateTime.now();
      }
    }

    logger.debug("Request Launch : {}", request.getObject().getUrl());

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
  
  @Override
  public void onRequestDone(Request request) {
    super.onRequestDone(request);
    
    if(request.getResponse().getCode() == Request.CODE_ERROR_RATE_LIMITED) {
      RequestResponse response = request.getResponse();
      String retryAfterString = response.getHeaderField("Retry-After");

      if (retryAfterString != null) {
        int retryAfter = Integer.parseInt(retryAfterString);
        logger.warn("Got rate limit error ! Wait ... : {} s", retryAfter);
        try {
          Thread.sleep(retryAfter * 1000l);
        } catch(InterruptedException e) {
          logger.error("Thread got interupt in onRequestDone : {}", e.getMessage());
          Thread.currentThread().interrupt();
        }
      }
    }
  }
}
