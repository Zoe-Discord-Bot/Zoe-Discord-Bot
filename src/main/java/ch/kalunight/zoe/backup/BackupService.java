package ch.kalunight.zoe.backup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.kalunight.zoe.Zoe;

public class BackupService implements Runnable {

  private static final File BACKUP_FOLDER = new File("ressources/backup");

  private static final Logger logger = LoggerFactory.getLogger(BackupService.class);
  
  @Override
  public void run() {

    if(!BACKUP_FOLDER.exists()) {
      BACKUP_FOLDER.mkdir();
    }

    DateTime currentTime = DateTime.now();
    int day = currentTime.getDayOfMonth();
    int month = currentTime.getMonthOfYear();
    int year = currentTime.getYear();
    int hour = currentTime.getHourOfDay();
    int minute = currentTime.getMinuteOfHour();
    
    File file = new File("ressources/backup/Backup-" + day + "-" + month + "-" + year + "T" + hour + ":" + minute  + ".zip");
    try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));){

      ZipEntry save = new ZipEntry("save.txt");
      out.putNextEntry(save);

      StringBuilder saveTxt = new StringBuilder();
      try(final BufferedReader reader = new BufferedReader(new FileReader(Zoe.SAVE_TXT_FILE));) {
        String line;

        while((line = reader.readLine()) != null) {
          saveTxt.append(line + "\n");
        }
      }
      byte[] saveTxtData = saveTxt.toString().getBytes();

      out.write(saveTxtData, 0, saveTxtData.length);
      out.closeEntry();

      for(File configFile : Zoe.SAVE_CONFIG_FOLDER.listFiles()) {
        ZipEntry saveConfig = new ZipEntry(Zoe.SAVE_CONFIG_FOLDER.getName() + "/" + configFile.getName());
        out.putNextEntry(saveConfig);

        StringBuilder configTxt = new StringBuilder();
        try(final BufferedReader reader = new BufferedReader(new FileReader(configFile));) {
          String line;

          while((line = reader.readLine()) != null) {
            configTxt.append(line + "\n");
          }
        }
        byte[] configData = configTxt.toString().getBytes();

        out.write(configData, 0, configData.length);
        out.closeEntry();
      }

    } catch (IOException e) {
      logger.warn("Error when saving backup !", e);
    }finally {
      logger.info("Backup ended !");
    }
  }
}
