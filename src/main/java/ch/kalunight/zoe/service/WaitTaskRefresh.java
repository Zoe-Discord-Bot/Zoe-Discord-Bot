package ch.kalunight.zoe.service;

public class WaitTaskRefresh implements Runnable {

  private TreatServerService service;
  
  public WaitTaskRefresh(TreatServerService treatServerService) {
    service = treatServerService;
  }
  
  @Override
  public void run() {
    service.taskEnded(null);
  }

}
