package ch.kalunight.zoe.model.clash;

public abstract class DangerosityReport {

  private DangerosityReportType reportType;
  
  public DangerosityReport(DangerosityReportType reportType) {
    this.reportType = reportType;
  }
  
  protected abstract String getInfoToShow(String lang);

  public DangerosityReportType getReportType() {
    return reportType;
  }
  
}
