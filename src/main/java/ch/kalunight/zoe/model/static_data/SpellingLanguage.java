package ch.kalunight.zoe.model.static_data;

public enum SpellingLanguage {
  FR("français"),
  DE("deutsch"),
  ES("española"),
  IT("italiano"),
  PL("polski"),
  PT("português"),
  RU("русский"),
  KO("한국의"),
  EN("english");

  private String name;

  private SpellingLanguage(String name) {
    this.name = name;
  }

  public String nameInNativeLanguage() {
    return name;
  }


  public static SpellingLanguage searchEnum(String search) {
    for (SpellingLanguage each : SpellingLanguage.values()) {
      if (each.name().equalsIgnoreCase(search)) {
        return each;
      }
    }
    return null;
  }
}