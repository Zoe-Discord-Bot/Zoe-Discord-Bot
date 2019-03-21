package ch.kalunight.zoe.command;

import java.awt.Color;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.core.EmbedBuilder;

public class AboutCommand extends Command {
  
  private static final Color COLOR_EMBED_MESSAGE = Color.CYAN;
  
  public AboutCommand() {
    this.name = "about";
    this.help = "Give info about the bot.";
    this.hidden = true;
    this.ownerCommand = false;
    this.guildOnly = false;
  }

  @Override
  protected void execute(CommandEvent event) {

    CommandUtil.sendTypingInFonctionOfChannelType(event);

    EmbedBuilder builder = new EmbedBuilder();
    
    builder.setAuthor("Hi guys ! I'm " + event.getSelfUser().getName() + " !", null, event.getSelfUser().getAvatarUrl());

    String desc = "Hi, I'm a League Of Legends bot written in Java by KaluNight#0001. "
        + "I offer the possibility to create an information panel that allows you to know if your friends are in "
        + "game directly in a Discord channel while giving various information about the current games. "
        + "You can contribute to my development on my Github [here](https://github.com/KaluNight/Zoe-Discord-Bot).\n" + 
        "Official Server Discord: <https://discord.gg/whc5PrC>\n\n"
        + "*I like butterflies, unicorns, and watching the end of finite realities!*";

    builder.setDescription(desc);
    if (event.getJDA().getShardInfo() == null) {
      builder.addField("Stats", event.getJDA().getGuilds().size() + " servers\n1 shard", true);
      builder.addField("Users", event.getJDA().getUsers().size() + " unique\n" + event.getJDA().getGuilds().stream().mapToInt(g -> g.getMembers().size()).sum() + " total", true);
      builder.addField("Channels", event.getJDA().getTextChannels().size() + " Text\n" + event.getJDA().getVoiceChannels().size() + " Voice", true);
    } else {
      builder.addField("Stats", (event.getClient()).getTotalGuilds() + " Servers\nShard " + (event.getJDA().getShardInfo().getShardId() + 1) 
          + "/" + event.getJDA().getShardInfo().getShardTotal(), true);
      builder.addField("This shard", event.getJDA().getUsers().size() + " Users\n" + event.getJDA().getGuilds().size() + " Servers", true);
      builder.addField("", event.getJDA().getTextChannels().size() + " Text Channels\n" + event.getJDA().getVoiceChannels().size() + " Voice Channels", true);
    }
    
    builder.setFooter("Last restart", null);
    builder.setTimestamp(event.getClient().getStartTime());
    builder.setColor(COLOR_EMBED_MESSAGE);

    event.reply(builder.build());
  }
}
