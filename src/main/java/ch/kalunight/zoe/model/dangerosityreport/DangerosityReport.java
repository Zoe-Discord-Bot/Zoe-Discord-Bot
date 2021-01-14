package ch.kalunight.zoe.model.dangerosityreport;

import java.text.DecimalFormat;

public abstract class DangerosityReport {

  public static final int BASE_SCORE = 0;
  
  public static final DecimalFormat POURCENTAGE_FORMAT = new DecimalFormat("###.#");

  public static final DecimalFormat SMALL_NUMBER_FORMAT = new DecimalFormat("###.##");
  
  private DangerosityReportType reportType;
  
  public DangerosityReport(DangerosityReportType reportType) {
    this.reportType = reportType;
  }
  
  public abstract String getInfoToShow(String lang);

  public abstract int getReportValue();
  
  public DangerosityReportType getReportType() {
    return reportType;
  }
  
}
