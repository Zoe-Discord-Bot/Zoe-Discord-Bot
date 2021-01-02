package ch.kalunight.zoe.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefreshStatus {

  private static final int NUMBER_OF_CYCLE_NEEDED_FOR_ACTION = 6; // 1 cycle happens every 10 seconds

  private static final int MINIMAL_REFRESH_RATE_IN_MINUTES = 5;

  private static final int START_DELAY_BETWEEN_EACH_REFRESH_IN_MINUTES = 5;

  private static final int EVALUTATION_INCREASE_DELAY_VALUE_IN_MINUTES = 5;

  private static final int MAX_REFRESH_RATE_IN_MINUTES = 60;

  private static final int SMART_MOD_TIME_IN_MINUTES = 60;

  private static final Logger logger = LoggerFactory.getLogger(RefreshStatus.class);

  private AtomicInteger refreshRateInMinute;

  private LocalDateTime evaluationEnd;

  private RefreshPhase refreshPhase;

  private LocalDateTime smartModEnd;

  private AtomicInteger numberOfServerManaged;

  private List<RefreshLoadStatus> refreshLoadsHistory;

  public RefreshStatus() {
    this.refreshRateInMinute = new AtomicInteger();
    this.evaluationEnd = null;
    this.refreshPhase = RefreshPhase.NEED_TO_INIT;
    this.smartModEnd = LocalDateTime.now();
    this.refreshLoadsHistory = Collections.synchronizedList(new ArrayList<RefreshLoadStatus>());
  }

  public void init(int numberOfServerCurrentlyManaged) {
    synchronized (this) {
      if(refreshPhase == RefreshPhase.NEED_TO_INIT) {
        refreshRateInMinute.set(START_DELAY_BETWEEN_EACH_REFRESH_IN_MINUTES);
        evaluationEnd = LocalDateTime.now().plusMinutes(refreshRateInMinute.get());
        numberOfServerManaged = new AtomicInteger(numberOfServerCurrentlyManaged);
        refreshPhase = RefreshPhase.IN_EVALUATION_PHASE;
        logger.info("Refresh status initiated! Evaluation started.");
      }else {
        logger.warn("Refresh status already initiated!");
      }
    }
  }

  public void manageEvaluationPhase(boolean loadingEnded) {
    synchronized (this) {
      if(refreshPhase == RefreshPhase.IN_EVALUATION_PHASE) {
        if(loadingEnded) {
          refreshPhase = RefreshPhase.CLASSIC_MOD;
          logger.info("Evaluation period ended ! A refresh rate of {} as been defined.", refreshRateInMinute.get());
        }else if (evaluationEnd.isBefore(LocalDateTime.now())) {
          evaluationEnd = evaluationEnd.plusMinutes(EVALUTATION_INCREASE_DELAY_VALUE_IN_MINUTES);
          refreshRateInMinute.set(refreshRateInMinute.get() + EVALUTATION_INCREASE_DELAY_VALUE_IN_MINUTES);

          if(refreshRateInMinute.get() < MAX_REFRESH_RATE_IN_MINUTES) {
            logger.info("Evaluation objective not achieved ! {} minutes as been added to the refresh rate. We wait to see if we can achieve the new target ({} minutes)",
                EVALUTATION_INCREASE_DELAY_VALUE_IN_MINUTES, refreshRateInMinute.get());
          }else {
            refreshPhase = RefreshPhase.SMART_MOD;
            smartModEnd = LocalDateTime.now().plusMinutes(SMART_MOD_TIME_IN_MINUTES);
            logger.info("Evaluation objective not achieved an max value reached! Smart mod is now enabled.");
          } 
        }
      }else {
        logger.warn("Refresh status not in evaluation phase!");
      }
    }
  }

  public void manageClassicMod(int numberServersManaged, long queueSize) {
    synchronized (this) {
      if(refreshPhase == RefreshPhase.CLASSIC_MOD) {
        numberOfServerManaged.set(numberServersManaged);

        if(queueSize < getNumberOfTaskUnderused()) {
          manageClassicModUnderUsed();
        }else if(queueSize > getNumberOfTaskForceSmartMod()) {
          manageClassicModForceSmartMod(queueSize);
        }else if(queueSize > getNumberOfTaskForceMoreDelay()) {
          manageClassicModForceMoreDelay(queueSize);
        }else if(queueSize > getNumberOfTaskAllowed()) {
          manageClassicModOverload(queueSize);
        }else {
          addRefreshLoadStatus(RefreshLoadStatus.NORMAL_STATE);
        }
      }else {
        logger.warn("Refresh status not in classic mod!");
      }
    }
  }

  private void manageClassicModOverload(long queueSize) {
    addRefreshLoadStatus(RefreshLoadStatus.OVER_USED);
    if(isStatusRegular(RefreshLoadStatus.OVER_USED)) {
      int newRefreshRate = refreshRateInMinute.get() + EVALUTATION_INCREASE_DELAY_VALUE_IN_MINUTES;
      if(newRefreshRate >= MAX_REFRESH_RATE_IN_MINUTES) {
        logger.info("Zoe is a bit overloaded for 1 minutes straight and the refresh rate is to high ! {} are currently in queue. The smart mod has been enabled.", queueSize);
        refreshPhase = RefreshPhase.SMART_MOD;
        smartModEnd = LocalDateTime.now().plusMinutes(SMART_MOD_TIME_IN_MINUTES);

      }else {
        logger.warn("Zoe is a bit overloaded ! {} are currently in queue. {} minutes added to the refresh cycle. Refresh rate is currently of {}",
            queueSize, EVALUTATION_INCREASE_DELAY_VALUE_IN_MINUTES, refreshRateInMinute.get());
        refreshRateInMinute.set(newRefreshRate);
      }
      refreshLoadsHistory.clear();
    }else {
      addRefreshLoadStatus(RefreshLoadStatus.OVER_USED);
    }
  }

  private void manageClassicModForceMoreDelay(long queueSize) {
    int newRefreshRate = refreshRateInMinute.get() + EVALUTATION_INCREASE_DELAY_VALUE_IN_MINUTES;

    if(newRefreshRate >= MAX_REFRESH_RATE_IN_MINUTES) {
      logger.info("Zoe is a bit overloaded and the refresh rate is to high ! {} are currently in queue. The smart mod has been enabled.", queueSize);
      refreshPhase = RefreshPhase.SMART_MOD;
      smartModEnd = LocalDateTime.now().plusMinutes(SMART_MOD_TIME_IN_MINUTES);
    }else {
      logger.warn("Zoe is a bit overloaded ! {} are currently in queue. {} minutes added to the refresh cycle. Refresh rate is currently of {}",
          queueSize, EVALUTATION_INCREASE_DELAY_VALUE_IN_MINUTES, refreshRateInMinute.get());
      refreshRateInMinute.set(newRefreshRate);
    }
    refreshLoadsHistory.clear();
  }

  private void manageClassicModForceSmartMod(long queueSize) {
    logger.warn("Zoe is overloaded ! {} are currently in queue. The smart mod is now enabled.", queueSize);
    refreshPhase = RefreshPhase.SMART_MOD;
    smartModEnd = LocalDateTime.now().plusMinutes(SMART_MOD_TIME_IN_MINUTES);
    refreshLoadsHistory.clear();
  }

  private void manageClassicModUnderUsed() {
    addRefreshLoadStatus(RefreshLoadStatus.UNDER_USED);
    if(isStatusRegular(RefreshLoadStatus.UNDER_USED)) {
      int newRefreshRate = refreshRateInMinute.get() - EVALUTATION_INCREASE_DELAY_VALUE_IN_MINUTES;
      if(newRefreshRate >= MINIMAL_REFRESH_RATE_IN_MINUTES) {
        logger.info("Zoe has been underused for 1 minutes straight, we lower the refresh rate by {}. The refresh rate is now of {} minutes.",
            EVALUTATION_INCREASE_DELAY_VALUE_IN_MINUTES, refreshRateInMinute.get());
        refreshRateInMinute.set(newRefreshRate);
        refreshLoadsHistory.clear();
      }
    }
  }

  public void manageSmartMod(int numberOfServerCurrentlyManaged) {
    synchronized (this) {
      if(refreshPhase == RefreshPhase.SMART_MOD) {
        if(smartModEnd.isBefore(LocalDateTime.now())) {
          refreshRateInMinute.set(START_DELAY_BETWEEN_EACH_REFRESH_IN_MINUTES);
          evaluationEnd = LocalDateTime.now().plusMinutes(refreshRateInMinute.get());
          numberOfServerManaged.set(numberOfServerCurrentlyManaged);
          refreshPhase = RefreshPhase.IN_EVALUATION_PHASE;
          logger.info("Smart mod ended! Evaluation of performance started.");
        }
      } else {
        logger.warn("Refresh status not in smart mod!");
      }
    }
  }

  private void addRefreshLoadStatus(RefreshLoadStatus refreshLoad) {
    refreshLoadsHistory.add(refreshLoad);

    if(refreshLoadsHistory.size() == NUMBER_OF_CYCLE_NEEDED_FOR_ACTION + 1) {
      refreshLoadsHistory.remove(0);
    }
  }

  private boolean isStatusRegular(RefreshLoadStatus refreshLoad) {
    boolean statusRegular = false;
    if(refreshLoadsHistory.size() == NUMBER_OF_CYCLE_NEEDED_FOR_ACTION) {
      statusRegular = true;

      for(RefreshLoadStatus refreshLoadStatus : refreshLoadsHistory) {
        if(!refreshLoadStatus.equals(refreshLoad)) {
          statusRegular = false;
          break;
        }
      }
    }
    return statusRegular;
  }

  private double getNumberOfTaskHandledEvery10Seconds() {
    return ((double) numberOfServerManaged.get() * 10) / (refreshRateInMinute.get() * 60);
  }

  private double getNumberOfTaskUnderused() {
    return getNumberOfTaskHandledEvery10Seconds() * 70 / 100; //70% of handled usage
  }

  private double getNumberOfTaskAllowed() {
    return getNumberOfTaskHandledEvery10Seconds() * 150 / 100; //150% of handled usage
  }

  private double getNumberOfTaskForceMoreDelay() {
    return getNumberOfTaskHandledEvery10Seconds() * 300 / 100; //300% of handled usage
  }

  private double getNumberOfTaskForceSmartMod() {
    return getNumberOfTaskHandledEvery10Seconds() * 500 / 100; //500% of handled usage
  }

  public RefreshPhase getRefreshPhase() {
    return refreshPhase;
  }

  public void setRefreshPhase(RefreshPhase refreshPhase) {
    this.refreshPhase = refreshPhase;
  }

  public LocalDateTime getEvaluationEnd() {
    return evaluationEnd;
  }

  public void setEvaluationEnd(LocalDateTime evaluationEnd) {
    this.evaluationEnd = evaluationEnd;
  }

  public LocalDateTime getSmartModEnd() {
    return smartModEnd;
  }

  public void setSmartModEnd(LocalDateTime smartModEnd) {
    this.smartModEnd = smartModEnd;
  }

  public List<RefreshLoadStatus> getRefreshLoadsHistory() {
    return refreshLoadsHistory;
  }

  public AtomicInteger getNumberOfServerManaged() {
    return numberOfServerManaged;
  }

  public AtomicInteger getRefresRatehInMinute() {
    return refreshRateInMinute;
  }

}
