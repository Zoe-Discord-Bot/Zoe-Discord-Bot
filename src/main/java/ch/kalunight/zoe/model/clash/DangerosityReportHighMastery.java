package ch.kalunight.zoe.model.clash;

import ch.kalunight.zoe.translation.LanguageManager;

public class DangerosityReportHighMastery extends DangerosityReport {

  private long mastery;
  
  public DangerosityReportHighMastery(long mastery) {
    super(DangerosityReportType.HIGH_MASTERIES);
    this.mastery = mastery;
  }

  @Override
  protected String getInfoToShow(String lang) {
    return String.format(LanguageManager.getText(lang, "dangerosityReportHighMasteryInfo"), mastery);
  }

  public long getMastery() {
    return mastery;
  }

}
