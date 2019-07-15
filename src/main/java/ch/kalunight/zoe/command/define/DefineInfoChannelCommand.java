package ch.kalunight.zoe.command.define;

import java.util.function.BiConsumer;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.command.CommandUtil;
import ch.kalunight.zoe.model.ControlPannel;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.static_data.SpellingLangage;
import ch.kalunight.zoe.service.InfoPanelRefresher;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.TextChannel;

public class DefineInfoChannelCommand extends Command {

  public DefineInfoChannelCommand() {
    this.name = "InfoChannel";
    this.arguments = "#mentionOfTheChannel";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = "Define a new InfoChannel where i can send info about players. Manage Channel permission needed.";
    this.helpBiConsumer = getHelpMethod();
  }

  @Override
  protected void execute(CommandEvent event) {
    event.getTextChannel().sendTyping().complete();
    Server server = ServerData.getServers().get(event.getGuild().getId());

    if(server == null) {
      server = new Server(event.getGuild(), SpellingLangage.EN, new ServerConfiguration());
      ServerData.getServers().put(event.getGuild().getId(), server);
    }

    if(server.getInfoChannel() != null) {
      event.reply("The channel " + server.getInfoChannel().getAsMention() + " is already set. "
          + "Please undefine or delete it first if you want to set another.");
    } else {
      if(event.getMessage().getMentionedChannels().size() != 1) {
        event.reply("You need to mention one channel like this : `>define infoChannel #the-best-text-channel`");
      } else {
        TextChannel textChannel = event.getMessage().getMentionedChannels().get(0);

        if(!textChannel.getGuild().equals(server.getGuild())) {
          event.reply("Please mention a channel from this server ! (I see you with your little magic trick :eyes:)");

        } else {
          if(!event.getMessage().getMentionedChannels().get(0).canTalk()) {
            event.reply("I can't talk in this channel ! Please give me the speak permission in this channel if you want to do that.");
          } else {
            server.setInfoChannel(textChannel);
            server.setControlePannel(new ControlPannel());
            event.reply("The channel has been defined ! It should be refreshed really quick.");
            InfoPanelRefresher infoPanelRefresher = new InfoPanelRefresher(server);
            ServerData.getTaskExecutor().submit(infoPanelRefresher);
          }
        }
      }
    }
  }

  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Define infoChannel command :\n");
        stringBuilder.append("--> `>define " + name + " " + arguments + "` : " + help);

        event.reply(stringBuilder.toString());
      }
    };
  }
}
