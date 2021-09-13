package ch.kalunight.zoe.model.dto;

public enum ZoeRegion {
  EUROPE,
  ASIA,
  AMERICAS;
  
  public static ZoeRegion getZoeRegionByPlatform(ZoePlatform platform) {
    switch (platform) {
    case EUNE:
    case EUW:
    case TR:
    case RU:
      return EUROPE;
    case BR:
    case NA:
    case LAN:
    case LAS:
    case OCE:
      return AMERICAS;
    case JP:
    case KR:
      return ASIA;
    default:
      return null;
    }
  }

  public String getShowableName() {
    return this.name();
  }
}
