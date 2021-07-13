package ch.kalunight.zoe.command;

import java.awt.Color;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class AboutCommandRunnable {
  
  private static final Color COLOR_EMBED_MESSAGE = Color.CYAN;
  
  public static MessageEmbed executeCommand(JDA jda, String langage) {

    EmbedBuilder builder = new EmbedBuilder();

    builder.setAuthor("Hi guys ! I'm " + jda.getSelfUser().getName() + " !", null, jda.getSelfUser().getAvatarUrl());

    ApplicationInfo info = jda.retrieveApplicationInfo().complete();
    String inviteLink =
        info.getInviteUrl(0L, Permission.MANAGE_CHANNEL, Permission.MESSAGE_MANAGE, Permission.MESSAGE_READ,
            Permission.MESSAGE_WRITE, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_ADD_REACTION,
            Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_HISTORY,
            Permission.MANAGE_EMOTES, Permission.MANAGE_ROLES, Permission.USE_SLASH_COMMANDS);
    inviteLink += "&response_type=code&redirect_uri=https%3A%2F%2Fzoe-discord-bot.ch%2FThanksYou.html";

    String desc = String.format(LanguageManager.getText(langage, "aboutMessage"),
        "https://github.com/KaluNight/Zoe-Discord-Bot", "<https://discord.gg/whc5PrC>", inviteLink);

    builder.setDescription(desc);
    builder.addField("Stats", Zoe.getNumberOfGuilds() + " Servers\nShard " + (jda.getShardInfo().getShardId() + 1)
        + "/" + jda.getShardInfo().getShardTotal(), true);
    builder.addField("This shard", jda.getUsers().size() + " Users\n" + jda.getGuilds().size() + " Servers", true);
    builder.addField("Channels",
        jda.getTextChannels().size() + " Text Channels\n" + jda.getVoiceChannels().size() + " Voice Channels",
        true);


    builder.setFooter("Last restart", null);
    builder.setTimestamp(Zoe.BOOT_TIME);
    builder.setColor(COLOR_EMBED_MESSAGE);

    return builder.build();
  }
  
}
