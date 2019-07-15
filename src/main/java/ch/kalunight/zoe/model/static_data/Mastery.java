package ch.kalunight.zoe.model.static_data;

public enum Mastery {
  MASTERY1("mastery1"), MASTERY2("mastery2"), MASTERY3("mastery3"), MASTERY4("mastery4"), MASTERY5("mastery5"), MASTERY6(
      "mastery6"), MASTERY7("mastery7");

  private String name;

  private Mastery(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public static Mastery getEnum(int mastery) {
    switch(mastery) {
      case 1:
        return MASTERY1;
      case 2:
        return MASTERY2;
      case 3:
        return MASTERY3;
      case 4:
        return MASTERY4;
      case 5:
        return MASTERY5;
      case 6:
        return MASTERY6;
      case 7:
        return MASTERY7;
      default:
        throw new IllegalArgumentException();
    }
  }
}
