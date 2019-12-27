package ch.kalunight.zoe.command;

import java.awt.Color;
import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.entities.ChannelType;

public class AboutCommand extends ZoeCommand {

  private static final Color COLOR_EMBED_MESSAGE = Color.CYAN;

  public AboutCommand() {
    this.name = "about";
    this.help = "aboutCommandHelp";
    this.hidden = true;
    this.ownerCommand = false;
    this.guildOnly = false;
    this.helpBiConsumer = CommandUtil.getHelpMethod(name, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {

    CommandUtil.sendTypingInFonctionOfChannelType(event);

    String langage = LanguageManager.DEFAULT_LANGUAGE;
    if(event.getChannelType() == ChannelType.TEXT) {
      DTO.Server server = getServer(event.getGuild().getIdLong());
      langage = server.serv_language;
    }
    EmbedBuilder builder = new EmbedBuilder();

    builder.setAuthor("Hi guys ! I'm " + event.getSelfUser().getName() + " !", null, event.getSelfUser().getAvatarUrl());

    ApplicationInfo info = event.getJDA().retrieveApplicationInfo().complete();
    String inviteLink =
        info.getInviteUrl(0L, Permission.MANAGE_CHANNEL, Permission.MESSAGE_MANAGE, Permission.MESSAGE_READ,
            Permission.MESSAGE_WRITE, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_ADD_REACTION,
            Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_HISTORY,
            Permission.MANAGE_EMOTES, Permission.MANAGE_ROLES);
    inviteLink += "&response_type=code&redirect_uri=https%3A%2F%2Fzoe-discord-bot.ch%2FThanksYou.html";

    String desc = String.format(LanguageManager.getText(langage, "aboutMessage"),
        "https://github.com/KaluNight/Zoe-Discord-Bot", "<https://discord.gg/whc5PrC>", inviteLink);

    builder.setDescription(desc);
    builder.addField("Stats", (event.getClient()).getTotalGuilds() + " Servers\nShard " + (event.getJDA().getShardInfo().getShardId() + 1)
        + "/" + event.getJDA().getShardInfo().getShardTotal(), true);
    builder.addField("This shard", event.getJDA().getUsers().size() + " Users\n" + event.getJDA().getGuilds().size() + " Servers", true);
    builder.addField("Channels",
        event.getJDA().getTextChannels().size() + " Text Channels\n" + event.getJDA().getVoiceChannels().size() + " Voice Channels",
        true);


    builder.setFooter("Last restart", null);
    builder.setTimestamp(event.getClient().getStartTime());
    builder.setColor(COLOR_EMBED_MESSAGE);

    event.reply(builder.build());
  }

  @Override
  public String toString() {
    return name + "command : " + help;
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
