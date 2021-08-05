package ch.kalunight.zoe.command.definition;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.command.SubscriptionCommandRunnable;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

public class SubscriptionCommandClassicDefinition extends ZoeCommand {

  public SubscriptionCommandClassicDefinition() {
    this.name = "subscription";
    this.arguments = "@user (not mandatory)";
    String[] alias = {"sub", "subs", "supporter"};
    this.aliases = alias;
    this.help = "subscriptionCommandHelp";
    this.hidden = false;
    this.ownerCommand = false;
    this.guildOnly = false;
    Permission[] permsNeeded = {Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_EMBED_LINKS};
    this.botPermissions = permsNeeded;
    this.helpBiConsumer = CommandUtil.getHelpMethod(name, help);
  }
  
  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    
    Guild guild = null;
    String language;
    if(event.isFromType(ChannelType.TEXT)) {
      language = getServer(event.getGuild().getIdLong()).getLanguage();
      guild = event.getGuild();
    }else {
      language = LanguageManager.DEFAULT_LANGUAGE;
    }
    
    User user;
    if(event.getMessage().getMentionedUsers().isEmpty()) {
      user = event.getAuthor();
    }else {
      user = event.getMessage().getMentionedUsers().get(0);
    }
    
    event.getChannel().sendMessageEmbeds(SubscriptionCommandRunnable.executeCommand(
        language, user, guild)).queue();
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }

}
