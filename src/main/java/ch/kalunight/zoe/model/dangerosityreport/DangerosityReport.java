package ch.kalunight.zoe.model.dangerosityreport;

import java.text.DecimalFormat;

public abstract class DangerosityReport {

  protected static final int BASE_SCORE = 0;
  
  protected DecimalFormat pourcentageFormat = new DecimalFormat("###,#");

  private DangerosityReportType reportType;
  
  public DangerosityReport(DangerosityReportType reportType) {
    this.reportType = reportType;
  }
  
  protected abstract String getInfoToShow(String lang);

  protected abstract int getReportValue();
  
  public DangerosityReportType getReportType() {
    return reportType;
  }
  
}
