package ch.kalunight.zoe.service;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.kalunight.zoe.Zoe;

public class DataSaver implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(DataSaver.class);

  @Override
  public void run() {
      try {
        Zoe.saveDataTxt();
      } catch(FileNotFoundException | UnsupportedEncodingException e) {
        logger.error("Error : {}", e);
      }
  }
  
}
