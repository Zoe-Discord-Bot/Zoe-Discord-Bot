package ch.kalunight.zoe.model.static_data;

public enum SpellingLangage {
  FR("Français"),
  EN("English");
  
  private String name;
  
  private SpellingLangage(String name) {
    this.name = name;
  }

  public String nameInNativeLanguage() {
    return name;
  }
  
}
