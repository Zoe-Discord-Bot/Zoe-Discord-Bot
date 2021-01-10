package ch.kalunight.zoe.model.dangerosityreport;

import ch.kalunight.zoe.model.dto.SavedSimpleMastery;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.LanguageUtil;

public class DangerosityReportHighMastery extends DangerosityReport {

  private static final int HIGH_MASTERY_LOW_VALUE = 10;

  private static final long HIGH_MASTERY_LOW_MASTERY_NEEDED = 50000;

  private static final int HIGH_MASTERY_MEDIUM_VALUE = 20;

  private static final long HIGH_MASTERY_MEDIUM_MASTERY_NEEDED = 100000;

  private static final int HIGH_MASTERY_HIGH_VALUE = 30;

  private static final long HIGH_MASTERY_HIGH_MASTERY_NEEDED = 200000;

  private SavedSimpleMastery mastery;

  public DangerosityReportHighMastery(SavedSimpleMastery mastery) {
    super(DangerosityReportType.HIGH_MASTERIES);
    this.mastery = mastery;
  }

  @Override
  protected String getInfoToShow(String lang) {
    return String.format(LanguageManager.getText(lang, "dangerosityReportHighMasteryInfo"), LanguageUtil.convertMasteryToReadableText(mastery));
  }

  @Override
  protected int getReportValue() {

    if(mastery.getChampionPoints() > HIGH_MASTERY_HIGH_MASTERY_NEEDED) {
      return HIGH_MASTERY_HIGH_VALUE;
    }
    
    if(mastery.getChampionPoints() > HIGH_MASTERY_MEDIUM_MASTERY_NEEDED) {
      return HIGH_MASTERY_MEDIUM_VALUE;
    }
    
    if(mastery.getChampionPoints() > HIGH_MASTERY_LOW_MASTERY_NEEDED) {
      return HIGH_MASTERY_LOW_VALUE;
    }

    return BASE_SCORE;
  }

  public long getMasteryPoints() {
    return mastery.getChampionPoints();
  }

}
