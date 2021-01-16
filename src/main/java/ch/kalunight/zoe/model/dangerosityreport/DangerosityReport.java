package ch.kalunight.zoe.model.dangerosityreport;

import java.text.DecimalFormat;

public abstract class DangerosityReport {

  public static final int BASE_SCORE = 0;
  
  public static final DecimalFormat POURCENTAGE_FORMAT = new DecimalFormat("###.#");

  public static final DecimalFormat SMALL_NUMBER_FORMAT = new DecimalFormat("###.##");
  
  private DangerosityReportType reportType;
  
  private DangerosityReportSource reportSource;
  
  public DangerosityReport(DangerosityReportType reportType, DangerosityReportSource reportSource) {
    this.reportType = reportType;
    this.reportSource = reportSource;
  }
  
  public abstract String getInfoToShow(String lang);

  public abstract int getReportValue();
  
  public DangerosityReportType getReportType() {
    return reportType;
  }
  
  public DangerosityReportSource getReportSource() {
    return reportSource;
  }
  
}
