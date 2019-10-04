package ch.kalunight.zoe.model.static_data;

import java.io.File;
import net.dv8tion.jda.api.entities.Emote;

public class CustomEmote {

  private String name;
  private File file;
  private Emote emote;

  public CustomEmote(String name, File file) {
    this.name = name;
    this.file = file;
  }
  
  public String getUsableEmote() {
    if(emote != null) {
      return emote.getAsMention();
    }
    return name;
  }

  public String getName() {
    return name.replaceAll(" ", "");
  }

  public void setName(String name) {
    this.name = name;
  }

  public File getFile() {
    return file;
  }

  public void setFile(File file) {
    this.file = file;
  }

  public Emote getEmote() {
    return emote;
  }

  public void setEmote(Emote emote) {
    this.emote = emote;
  }

}
