package ch.kalunight.zoe.exception;

@SuppressWarnings("serial")
public class PlayerNotFoundException extends RuntimeException {

  public PlayerNotFoundException(String message) {
    super(message);
  }
}
