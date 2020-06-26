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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class LanguageManager {
  
  public static final String DEFAULT_LANGUAGE = "EN.json";
  
  private static final Logger logger = LoggerFactory.getLogger(LanguageManager.class);
  
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

      File englishFile = new File(LANGUAGE_FOLDER.getAbsolutePath() + "/" + DEFAULT_LANGUAGE);
      
      try(final BufferedReader reader = new BufferedReader(new FileReader(englishFile));) {
        JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
        
        for(Entry<String, JsonElement> entryJson : jsonObject.entrySet()) {
          if(!entryJson.getValue().getAsString().equals("")) {
            translations.put(new TranslationKey(englishFile.getName(), entryJson.getKey()), entryJson.getValue().getAsString().replaceAll("#.*?#", ""));
          }
        }
      }
      
      for(File file : LANGUAGE_FOLDER.listFiles()) {     
        try(final BufferedReader reader = new BufferedReader(new FileReader(file));) {
          JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);

          if(jsonObject.get("nativeActualLanguage") == null || jsonObject.get("nativeActualLanguage").getAsString().equals("") || file.getName().equals(DEFAULT_LANGUAGE)) {
            continue;
          }
          
          for(Entry<String, JsonElement> entryJson : jsonObject.entrySet()) {
            if(!entryJson.getValue().getAsString().equals("") && isTranslationValid(entryJson.getKey(), entryJson.getValue().getAsString(), file.getName())) {
              translations.put(new TranslationKey(file.getName(), entryJson.getKey()), entryJson.getValue().getAsString().replaceAll("#.*?#", ""));
            }
          }
        }
        listLanguages.add(file.getName());
      }
    }
  }
  
  private static boolean isTranslationValid(String key, String translation, String translatedLanguage) {
    String englishBase = getText(DEFAULT_LANGUAGE, key);
    
    if((StringUtils.countMatches(englishBase, "%s") == StringUtils.countMatches(translation, "%s")) && (StringUtils.countMatches(englishBase, "%d") == StringUtils.countMatches(translation, "%d"))) {
      return true;
    }
    
    logger.warn("Error when inserting param with the translation {} from the language {} ! This translation will not be used.", key, translatedLanguage);
    return false;
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
