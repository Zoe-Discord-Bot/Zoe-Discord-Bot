package ch.kalunight.zoe.command.stats;

import java.sql.SQLException;
import java.util.List;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.stats.definition.StatsProfileCommandClassicDefinition;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
  
public class StatsCommandRunnable {

  public static final String USAGE_NAME = "stats";
  
  private StatsCommandRunnable() {
    // hide default constructor
  }
  
  public static void executeCommand(Server server, List<Member> mentionnedMembers, String args, InteractionHook hook, TextChannel channel, CommandEvent event) throws SQLException {
    
    boolean playerGiven = false;
    if(!mentionnedMembers.isEmpty() ||
        args.split(" ").length >= 2) {
      playerGiven = true;
    }
    
    if(playerGiven && event != null) {
      for(Command command : Zoe.getMainCommands(null)) {
        for(Command commandChild : command.getChildren()) {
          if(commandChild instanceof StatsProfileCommandClassicDefinition) {
            ((StatsProfileCommandClassicDefinition) commandChild).executeCommandExternal(event);
            return;
          }
        }
      }
    }
    
    String message = LanguageManager.getText(server.getLanguage(), "mainStatsCommandHelpMessage");
    
    if(hook != null) {
      hook.editOriginal(message).queue();
    }else {
      channel.sendMessage(message).queue();
    }
  }


}
