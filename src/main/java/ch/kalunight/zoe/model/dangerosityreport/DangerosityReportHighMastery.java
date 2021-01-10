package ch.kalunight.zoe.model.dangerosityreport;

import ch.kalunight.zoe.model.dto.SavedSimpleMastery;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.LanguageUtil;

public class DangerosityReportHighMastery extends DangerosityReport {

  private SavedSimpleMastery mastery;
  
  public DangerosityReportHighMastery(SavedSimpleMastery mastery) {
    super(DangerosityReportType.HIGH_MASTERIES);
    this.mastery = mastery;
  }

  @Override
  protected String getInfoToShow(String lang) {
    return String.format(LanguageManager.getText(lang, "dangerosityReportHighMasteryInfo"), LanguageUtil.convertMasteryToReadableText(mastery));
  }

  public long getMasteryPoints() {
    return mastery.getChampionPoints();
  }

}
