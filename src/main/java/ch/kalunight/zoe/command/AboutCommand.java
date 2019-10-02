package ch.kalunight.zoe.command;

import java.awt.Color;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ApplicationInfo;

public class AboutCommand extends Command {

  private static final Color COLOR_EMBED_MESSAGE = Color.CYAN;

  public AboutCommand() {
    this.name = "about";
    this.help = "aboutCommandHelp";
    this.hidden = true;
    this.ownerCommand = false;
    this.guildOnly = false;
  }

  @Override
  protected void execute(CommandEvent event) {

    CommandUtil.sendTypingInFonctionOfChannelType(event);
    
    Server server = ServerData.getServers().get(event.getGuild().getId());

    EmbedBuilder builder = new EmbedBuilder();

    builder.setAuthor("Hi guys ! I'm " + event.getSelfUser().getName() + " !", null, event.getSelfUser().getAvatarUrl());

    ApplicationInfo info = event.getJDA().retrieveApplicationInfo().complete();
    String inviteLink =
        info.getInviteUrl(0L, Permission.MANAGE_CHANNEL, Permission.MESSAGE_MANAGE, Permission.MESSAGE_READ,
            Permission.MESSAGE_WRITE, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_ADD_REACTION,
            Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_HISTORY,
            Permission.MANAGE_EMOTES, Permission.MANAGE_ROLES);
    inviteLink += "&response_type=code&redirect_uri=https%3A%2F%2Fzoe-discord-bot.ch%2FThanksYou.html";
    
    String desc = String.format(LanguageManager.getText(server.getLangage(), "aboutMessage"),
        "https://github.com/KaluNight/Zoe-Discord-Bot", "<https://discord.gg/whc5PrC>", inviteLink);

    builder.setDescription(desc);
    if(event.getJDA().getShardInfo() == null) {
      builder.addField("Stats", event.getJDA().getGuilds().size() + " servers\n1 shard", true);
      builder.addField("Users", event.getJDA().getUsers().size() + " unique\n"
          + event.getJDA().getGuilds().stream().mapToInt(g -> g.getMembers().size()).sum() + " total", true);
      builder.addField("Channels",
          event.getJDA().getTextChannels().size() + " Text\n" + event.getJDA().getVoiceChannels().size() + " Voice", true);
    } else {
      builder.addField("Stats", (event.getClient()).getTotalGuilds() + " Servers\nShard " + (event.getJDA().getShardInfo().getShardId() + 1)
          + "/" + event.getJDA().getShardInfo().getShardTotal(), true);
      builder.addField("This shard", event.getJDA().getUsers().size() + " Users\n" + event.getJDA().getGuilds().size() + " Servers", true);
      builder.addField("",
          event.getJDA().getTextChannels().size() + " Text Channels\n" + event.getJDA().getVoiceChannels().size() + " Voice Channels",
          true);
    }

    builder.setFooter("Last restart", null);
    builder.setTimestamp(event.getClient().getStartTime());
    builder.setColor(COLOR_EMBED_MESSAGE);

    event.reply(builder.build());
  }
  
  @Override
  public String toString() {
    return name + "command : " + help;
  }
}
