package ch.kalunight.zoe.translation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class LanguageManager {

  public static final String DEFAULT_LANGUAGE = "EN.json";
  
  private static final File LANGUAGE_FOLDER = new File("ressources/languages");

  private static final ConcurrentHashMap<TranslationKey, String> translations = new ConcurrentHashMap<>();
  
  private static final DecimalFormat df = new DecimalFormat("###.#");
  
  private static final List<String> listLanguages = Collections.synchronizedList(new ArrayList<>());

  private static final Gson gson = new Gson();

  private LanguageManager() {
    //hide public constructor
  }

  public static void loadTranslations() throws IOException {
    synchronized(translations) {
      translations.clear();
      listLanguages.clear();

      for(File file : LANGUAGE_FOLDER.listFiles()) {
        
        try(final BufferedReader reader = new BufferedReader(new FileReader(file));) {
          JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);

          if(jsonObject.get("nativeActualLanguage") == null || jsonObject.get("nativeActualLanguage").getAsString().equals("")) {
            continue;
          }
          
          for(Entry<String, JsonElement> entryJson : jsonObject.entrySet()) {
            if(!entryJson.getValue().getAsString().equals("")) {
              translations.put(new TranslationKey(file.getName(), entryJson.getKey()), entryJson.getValue().getAsString().replaceAll("#.*?#", ""));
            }
          }
        }
        listLanguages.add(file.getName());
      }
    }
  }
  
  public static String getPourcentageTranslated(String language) {
    int nbrTranslationEnglish = 0;
    int nbrTranslationOther = 0;
    
    Iterator<Entry<TranslationKey, String>> translationsList = translations.entrySet().iterator();
    
    while(translationsList.hasNext()) {
      Entry<TranslationKey, String> translation = translationsList.next();
      
      if(translation.getKey().getLanguage().equals(DEFAULT_LANGUAGE)) {
        nbrTranslationEnglish++;
        if(translation.getKey().getLanguage().equals(language)) {
          nbrTranslationOther++;
        }
      }else if(translation.getKey().getLanguage().equals(language) && !translation.getValue().equals("")) {
        nbrTranslationOther++;
      }
    }
    
    if(nbrTranslationEnglish == 0) {
      return "Error with English Language";
    }
    
    return "("+ df.format((nbrTranslationOther / (double) nbrTranslationEnglish) * 100) + "%)";
  }

  public static String getText(String spellingLangage, String key) {
    String text = translations.get(new TranslationKey(spellingLangage, key));
        
    if(text == null) {
      text = translations.get(new TranslationKey(DEFAULT_LANGUAGE, key));
    }
    
    if(text == null) {
      return "Translation error (Id: " + key + ")";
    }
    return text;
  }

  public static List<String> getListlanguages() {
    return listLanguages;
  }
  
}
