package ch.kalunight.zoe.command;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class SetupCommand extends Command {

  public SetupCommand() {
    this.name = "setup";
    this.help = "Give info about how setup me.";
    this.hidden = true;
    this.ownerCommand = false;
    this.guildOnly = false;
  }

  @Override
  protected void execute(CommandEvent event) {
    CommandUtil.sendTypingInFonctionOfChannelType(event);
    event.reply("Hi, you'll see, my commands are really not complicated. "
        + "First of all, know that you can see all my commands with a help with the command `>help`. "
        + "Type `>commandName help` to get help about one specific command. "
        + "Be also sure you have the right to manage the channels (Manage Channels permission). \n\n"
        + "1. To start, create an information channel with the command `>create infoChannel channelName`. "
        + "You can also define an existing one with the command `>define infoChannel #mentionOfAChannel`.\n"
        + "2. Then add the League of Legends players to the system using the command `>create player @MentionOfPlayer (ServerTag) (SummonerName)`. "
        + "You can add a secondary account to a player with the command `>add accountToPlayer @mentionOfPlayer (ServerTag) (SummonerName)\n"
        + "3. You can use the command `>config` to config your server like you want.\n\n"
        + "That's it, your information system has been well implemented. If you have questions,"
        + " features requests or just discuss feel free to come here : https://discord.gg/whc5PrC\n"
        + "*Note: The information panel is updated every 3 minutes, infocards sended in ~20 sec.*");
  }
}
