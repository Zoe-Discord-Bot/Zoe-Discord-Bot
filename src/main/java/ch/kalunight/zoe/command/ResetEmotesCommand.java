package ch.kalunight.zoe.command;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.Zoe;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;

public class ResetEmotesCommand extends Command {

  public ResetEmotesCommand() {
    this.name = "resetEmotes";
    this.help = "Reset emotes and Safely shutdown the bot";
    this.hidden = true;
    this.ownerCommand = true;
    this.guildOnly = false;
  }

  
  @Override
  protected void execute(CommandEvent event) {
    TextChannel textChannel = event.getTextChannel();
    
    textChannel.sendMessage("All custom Emotes managed by the bot will be deleted and the bot will shutdown after that."
        + " All emotes will be reupladed at my relaunch.").complete();
    
    for(Guild guild : Zoe.getJda().getGuilds()) {
      if(guild.getOwnerId().equals(Zoe.getJda().getSelfUser().getId())) {
        textChannel.sendMessage("The guild \""+ guild.getName() + "\" will be deleted...").complete();
        guild.delete().complete();
        textChannel.sendMessage("Deleted !").complete();
      }
    }
    
    textChannel.sendMessage("Emotes Reset ! Now will be shut down ...").complete();
    
    new ShutDownCommand().execute(event);
  }

}
