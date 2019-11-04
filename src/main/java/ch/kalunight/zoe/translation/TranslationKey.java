package ch.kalunight.zoe.translation;

import ch.kalunight.zoe.model.static_data.SpellingLangage;

public class TranslationKey {

  private SpellingLangage language;
  private String key;
  
  public TranslationKey(SpellingLangage language, String translationKey) {
    this.language = language;
    this.key = translationKey;
  }

  public SpellingLangage getLanguage() {
    return language;
  }

  public void setLanguage(SpellingLangage language) {
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
    if(this == obj)
      return true;
    if(obj == null)
      return false;
    if(getClass() != obj.getClass())
      return false;
    TranslationKey other = (TranslationKey) obj;
    if(key == null) {
      if(other.key != null)
        return false;
    } else if(!key.equals(other.key)) {
      return false; 
    }
    if(language != other.language) {
      return false;
    }
    return true;
  }
  
}
