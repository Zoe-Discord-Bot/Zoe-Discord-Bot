package ch.kalunight.zoe.command;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.SpellingLangage;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class ResetCommand extends Command {
  
  private final EventWaiter waiter;

  public ResetCommand(EventWaiter waiter) {
    this.name = "reset";
    this.help = "Reset the actual config of the server.";
    this.hidden = false;
    this.ownerCommand = false;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.guildOnly = true;
    this.waiter = waiter;
    this.helpBiConsumer = getHelpMethod();
  }
  
  @Override
  protected void execute(CommandEvent event) {
    CommandUtil.sendTypingInFonctionOfChannelType(event);
    
    event.getEvent().getTextChannel().sendMessage("**WARNING**: This command will reset the configuration of this server ! "
        + "Are you sure to do that ?\n\nIf you want to do that say **YES**.").queue();
    
    waiter.waitForEvent(MessageReceivedEvent.class,
        e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel())
          && !e.getMessage().getId().equals(event.getMessage().getId()),
        e -> reset(e), 1, TimeUnit.MINUTES,
        () -> cancelReset(event.getEvent()));
  }

  private void reset(MessageReceivedEvent messageReceivedEvent) {
    if(messageReceivedEvent.getMessage().getContentRaw().equals("YES")) {
      
      messageReceivedEvent.getTextChannel().sendMessage("Okay, let's go ! Let's forgot everything !").queue();
      
      Server server = ServerData.getServers().get(messageReceivedEvent.getGuild().getId());
      
      SpellingLangage spellingLangage = SpellingLangage.EN;
      
      if(server != null) {
        spellingLangage = server.getLangage();
      }
      
      ServerData.getServers().put(messageReceivedEvent.getGuild().getId(), new Server(messageReceivedEvent.getGuild(), spellingLangage));
      
      messageReceivedEvent.getTextChannel().sendMessage("Done, i have been reset correctly").queue();
    }else {
      messageReceivedEvent.getTextChannel().sendMessage("Alright, so i do nothing.").queue();
    }
  }
  
  private void cancelReset(MessageReceivedEvent event) {
    event.getTextChannel().sendMessage("I've been waiting for more than a minute. I won't wait more. "
        + "Please resend the command `>reset` if you want to reset myself.").queue();
  }



  private BiConsumer<CommandEvent, Command> getHelpMethod() {
    return new BiConsumer<CommandEvent, Command>() {
      @Override
      public void accept(CommandEvent event, Command command) {
        CommandUtil.sendTypingInFonctionOfChannelType(event);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Reset command :\n");
        stringBuilder.append("--> `>" + name + " " + "` : " + help);

        event.reply(stringBuilder.toString());
      }
    };
  }
  
}
