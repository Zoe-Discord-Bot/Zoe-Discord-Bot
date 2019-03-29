package ch.kalunight.zoe.util;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.kalunight.zoe.model.BooleanSynchronisable;
import net.rithms.riot.api.request.Request;
import net.rithms.riot.api.request.RequestResponse;
import net.rithms.riot.api.request.ratelimit.DefaultRateLimitHandler;
import net.rithms.riot.api.request.ratelimit.RespectedRateLimitException;

public class SleeperRateLimitHandler extends DefaultRateLimitHandler {

  private static BooleanSynchronisable isRateLimited = new BooleanSynchronisable(false);

  private static DateTime rateLimitStart = DateTime.now();

  private static int rateLimitTimeInSeconds = -1;

  private static final Logger logger = LoggerFactory.getLogger(SleeperRateLimitHandler.class);

  @Override
  public void onRequestAboutToFire(Request request) throws RespectedRateLimitException {

    if(isRateLimited.isValue()) {
      try {
        long msToSleep = (rateLimitTimeInSeconds * 1000) - (DateTime.now().getMillis() - rateLimitStart.getMillis());
        if(msToSleep > 0) {
          logger.info("Thread {} get blocked for {} ms for respect Rate limit", Thread.currentThread().getName(), msToSleep);
          Thread.sleep(msToSleep);
        }else {
          logger.info("All threads can now work normally");
          isRateLimited.setValue(false);
        }
      } catch(InterruptedException e) {
        logger.error("Thread got interupt in onRequestDone : {}", e.getMessage());
        Thread.currentThread().interrupt();
      }
    }

    logger.debug("Request send : {}", request.getObject().getUrl());
    super.onRequestAboutToFire(request);
  }

  @Override
  public void onRequestDone(Request request) {
    super.onRequestDone(request);

    synchronized(isRateLimited) {
      if(request.getResponse().getCode() == Request.CODE_ERROR_RATE_LIMITED) {
        RequestResponse response = request.getResponse();
        String retryAfterString = response.getHeaderField("Retry-After");

        if (retryAfterString != null) {
          isRateLimited.setValue(true);
          setRateLimitTimeInSeconds(Integer.parseInt(retryAfterString));
          setRateLimitStart(DateTime.now());
          logger.info("Got rate limit response ! Block all request for {} s ...", getRateLimitTimeInSeconds());
        }
      }
    }
  }

  public static int getRateLimitTimeInSeconds() {
    return rateLimitTimeInSeconds;
  }

  public static void setRateLimitTimeInSeconds(int rateLimitTimeInSeconds) {
    SleeperRateLimitHandler.rateLimitTimeInSeconds = rateLimitTimeInSeconds;
  }

  public static DateTime getRateLimitStart() {
    return rateLimitStart;
  }

  public static void setRateLimitStart(DateTime rateLimitStart) {
    SleeperRateLimitHandler.rateLimitStart = rateLimitStart;
  }
}
