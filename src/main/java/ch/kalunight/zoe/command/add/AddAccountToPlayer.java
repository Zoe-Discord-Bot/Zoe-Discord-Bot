package ch.kalunight.zoe.command.add;

import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.command.CommandUtil;
import net.dv8tion.jda.core.Permission;

public class AddAccountToPlayer extends Command {

  public static final String USAGE_NAME = "accountToPlayer";
  public static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\(([^)]+)\\)");
  
  public AddAccountToPlayer() {
    this.name = USAGE_NAME;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.arguments = "@MentionPlayer (accountName)";
    this.userPermissions = permissionRequired;
    this.help = "Add to the mentionned player the account in \"()\". Manage Channel permission needed.";
    this.helpBiConsumer = getHelpMethod();
  }
  
  @Override
  protected void execute(CommandEvent event) {
    // TODO Auto-generated method stub
    
  }

  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Add "+ name +" command :\n");
        stringBuilder.append("--> `>add " + name + " " + arguments + "` : " + help);

        event.reply(stringBuilder.toString());
      }
    };
  }
  
}
