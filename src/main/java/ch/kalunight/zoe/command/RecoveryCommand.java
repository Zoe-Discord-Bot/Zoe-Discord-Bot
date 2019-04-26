package ch.kalunight.zoe.command;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.impl.CommandClientImpl;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.create.CreateCommand;
import ch.kalunight.zoe.command.create.CreatePlayerCommand;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class RecoveryCommand extends Command {

  private final EventWaiter waiter;

  public RecoveryCommand(EventWaiter waiter) {
    this.name = "recoverPlayer";
    this.help = "Remake all command of a channel.";
    this.arguments = "@mentionOfSender";
    this.hidden = false;
    this.ownerCommand = false;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.guildOnly = true;
    this.waiter = waiter;
    this.cooldown = 120;
  }

  @Override
  protected void execute(CommandEvent event) {
    CommandUtil.sendTypingInFonctionOfChannelType(event);

    if(event.getMessage().getMentionedMembers().size() == 1 
        && event.getMessage().getMentionedMembers().get(0).getPermissions().contains(Permission.MANAGE_CHANNEL)) {

      event.reply("**WARNING**: This command will relauch all create players commands in this channel (and spam a lot) ! "
          + "Your actual configuration will be drastically modified! "
          + "It's only recommanded to use it if Zoe have forgot your save.\n\n Say **YES** if you want to do that.");

      waiter.waitForEvent(MessageReceivedEvent.class,
          e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel()),
          e -> recover(e, event.getMessage().getMentionedMembers().get(0).getUser()), 1,
          TimeUnit.MINUTES,
          () -> cancelRecovery(event.getEvent()));
    }else {
      event.reply("Please mention one people who has the Permission to manage channel");
    }
  }

  private void recover(MessageReceivedEvent e, User user) {
    if(e.getMessage().getContentRaw().equals("YES")) {

      e.getTextChannel().sendMessage("Alright, i start now ...").queue();
      e.getTextChannel().sendMessage("By the power of chocolate moon cake ! Restore all theses lost players !").queueAfter(1, TimeUnit.SECONDS);

      List<Message> messages = e.getTextChannel().getIterableHistory().stream()
          .filter(m-> m.getAuthor().equals(user))
          .limit(1000)
          .collect(Collectors.toList());


      for(Message potentialCreatePlayer : messages) {
        String message = potentialCreatePlayer.getContentRaw();
        if(message.startsWith(Zoe.BOT_PREFIX) && isCreatePlayerCommand(message)) {

          User playerUser = CreatePlayerCommand.getMentionedUser(potentialCreatePlayer.getMentionedMembers());
          if(playerUser == null) {
            
          }
          
        }
      }

    }else {
      e.getTextChannel().sendMessage("Right, so i do nothing.").queue();
    }
  }

  private boolean isCreatePlayerCommand(String command) {
    if(command.substring(1).split(" ").length < 4) {
      return false;
    }

    String messageInTreatment = command.substring(1).split(" ")[0];

    return messageInTreatment.equalsIgnoreCase(CreateCommand.USAGE_NAME)
        && command.substring(1).split(" ")[1].equals(CreatePlayerCommand.USAGE_NAME);
  }

  private void cancelRecovery(MessageReceivedEvent event) {
    event.getTextChannel().sendMessage("Recovery has been canceled.");
  }


}
