package ch.kalunight.zoe.model.static_data;

public enum SpellingLanguage {
  FR("Français"),
  EN("English");
  
  private String name;
  
  private SpellingLanguage(String name) {
    this.name = name;
  }

  public String nameInNativeLanguage() {
    return name;
  }
  
}
