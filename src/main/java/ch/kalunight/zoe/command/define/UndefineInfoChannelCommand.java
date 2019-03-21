package ch.kalunight.zoe.command.define;

import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.command.CommandUtil;
import ch.kalunight.zoe.model.ControlPannel;
import ch.kalunight.zoe.model.InfoCard;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.SpellingLangage;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;

public class UndefineInfoChannelCommand extends Command {

  public UndefineInfoChannelCommand() {
    this.name = "InfoChannel";
    this.arguments = "";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = "Undefine the actual InfoChannel. This will **NOT** delete the channel, "
        + "i will just stop to do my work in. Manage Channel permission needed.";
    this.helpBiConsumer = getHelpMethod();
  }
  
  @Override
  protected void execute(CommandEvent event) {
    event.getTextChannel().sendTyping().complete();
    Server server = ServerData.getServers().get(event.getGuild().getId());
    
    if(server == null) {
      server = new Server(event.getGuild(), SpellingLangage.EN);
      ServerData.getServers().put(event.getGuild().getId(), server);
    }
    
    if(server.getInfoChannel() == null) {
      event.reply("I have no registered info channel ! I can't undefine something who don't exist :p");
    }else {
      for(InfoCard infoCard : server.getControlePannel().getInfoCards()) {
        infoCard.getMessage().delete().queue();
        infoCard.getTitle().delete().queue();
      }
      for(Message message : server.getControlePannel().getInfoPanel()) {
        message.delete().queue();
      }
      server.setInfoChannel(null);
      server.setControlePannel(new ControlPannel());
      event.reply("I have undefine the info channel ! I have deleted all message related to my activity");
    }
  }
  
  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Undefine infoChannel command :\n");
        stringBuilder.append("--> `>undefine " + name + " " + arguments + "` : " + help);
        
        event.reply(stringBuilder.toString());
      }
    };
  }
}
