package ch.kalunight.zoe.model.dangerosityreport;

import java.util.List;

import ch.kalunight.zoe.translation.LanguageManager;

public class DangerosityReportLittleChampionPool extends DangerosityReport {

  private List<Integer> championPoolIds;
  
  public DangerosityReportLittleChampionPool(List<Integer> championPoolIds) {
    super(DangerosityReportType.LITTLE_CHAMPION_POOL);
    this.championPoolIds = championPoolIds;
  }

  @Override
  protected String getInfoToShow(String lang) {
    return String.format(LanguageManager.getText(lang, "dangerosityReportLittleChampionPoolInfo"), championPoolIds.size());
  }

  public List<Integer> getChampionPoolIds() {
    return championPoolIds;
  }
  
}
