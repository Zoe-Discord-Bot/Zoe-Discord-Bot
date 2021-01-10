package ch.kalunight.zoe.model.dangerosityreport;

import java.text.DecimalFormat;

public abstract class DangerosityReport {

  protected DecimalFormat pourcentageFormat = new DecimalFormat("###,#");

  private DangerosityReportType reportType;
  
  public DangerosityReport(DangerosityReportType reportType) {
    this.reportType = reportType;
  }
  
  protected abstract String getInfoToShow(String lang);

  public DangerosityReportType getReportType() {
    return reportType;
  }
  
}
