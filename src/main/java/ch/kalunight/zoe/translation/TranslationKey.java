package ch.kalunight.zoe.translation;

public class TranslationKey {

  private String language;
  private String key;

  public TranslationKey(String language, String translationKey) {
    this.language = language;
    this.key = translationKey;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String translationKey) {
    this.key = translationKey;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((key == null) ? 0 : key.hashCode());
    result = prime * result + ((language == null) ? 0 : language.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TranslationKey other = (TranslationKey) obj;
    if (key == null) {
      if (other.key != null)
        return false;
    } else if (!key.equals(other.key)) {
      return false;
    }
    if (language == null) {
      if (other.language != null) {
        return false;
      }
    } else if (!language.equals(other.language)) {
      return false;
    }
    return true;
  }
}
