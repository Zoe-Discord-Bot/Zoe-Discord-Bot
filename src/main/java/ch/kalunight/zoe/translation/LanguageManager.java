package ch.kalunight.zoe.translation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ch.kalunight.zoe.model.static_data.SpellingLanguage;

public class LanguageManager {

  private static final File LANGUAGE_FOLDER = new File("ressources/languages");

  private static final ConcurrentHashMap<TranslationKey, String> translations = new ConcurrentHashMap<>();

  private static final Gson gson = new Gson();

  private LanguageManager() {
    //hide public constructor
  }

  public static void loadTranslations() throws IOException {
    synchronized(translations) {
      translations.clear();

      for(File file : LANGUAGE_FOLDER.listFiles()) {

        SpellingLanguage language = SpellingLanguage.valueOf(file.getName().split("\\.")[0]);

        try(final BufferedReader reader = new BufferedReader(new FileReader(file));) {
          JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);

          for(Entry<String, JsonElement> entryJson : jsonObject.entrySet()) {
            if(!entryJson.getValue().getAsString().equals("")) {
              translations.put(new TranslationKey(language, entryJson.getKey()), entryJson.getValue().getAsString());
            }
          }
        }
      }
    }
  }

  public static String getText(SpellingLanguage spellingLangage, String key) {
    String text = translations.get(new TranslationKey(spellingLangage, key));
    if(text == null) {
      text = translations.get(new TranslationKey(SpellingLanguage.EN, key));
    }

    if(text == null) {
      return "Translation error";
    }
    return text;
  }

}
