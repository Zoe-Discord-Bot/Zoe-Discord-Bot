package ch.kalunight.zoe.util;

public class EmojiUtil {
  
  public static String getEmojiNumberKeycapString(int number) {
    switch (number) {
    case 1:
      return "1️⃣";
    case 2:
      return "2️⃣";
    case 3:
      return "3️⃣";
    case 4:
      return "4️⃣";
    case 5:
      return "5️⃣";
    default:
      return "0";
    }
  }
}
