package ch.kalunight.zoe.command.definition;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ch.kalunight.zoe.command.SubscriptionCommandRunnable;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class SubscriptionCommandSlashDefinition extends ZoeSlashCommand {

  public SubscriptionCommandSlashDefinition(String serverId) {
    this.name = "subscription";
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "subscriptionCommandSlashHelp");
    this.hidden = false;
    this.ownerCommand = false;
    Permission[] permsNeeded = {Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_EMBED_LINKS};
    this.botPermissions = permsNeeded;
    
    List<OptionData> data = new ArrayList<>();
    
    OptionData discordAccountToShow = new OptionData(OptionType.USER, ZoeSlashCommand.USER_OPTION_ID, "The discord account you want to see.");
    discordAccountToShow.setRequired(false);
    
    data.add(discordAccountToShow);
    
    this.options = data;
    
    if(serverId == null) {
      this.guildOnly = false;
    }else {
      this.guildOnly = true; //True for testing
      this.guildId = serverId; //Test server
    }
  }
  
  @Override
  protected void executeCommand(SlashCommandEvent event) throws SQLException {
    
    String language;
    if(event.getGuild() != null) {
      language = ZoeCommand.getServer(event.getGuild().getIdLong()).getLanguage();
    }else {
      language = LanguageManager.DEFAULT_LANGUAGE;
    }
    
    User user;
    if(event.getOption(ZoeSlashCommand.USER_OPTION_ID) != null) {
      user = event.getOption(ZoeSlashCommand.USER_OPTION_ID).getAsUser();
    }else {
      user = event.getUser();
    }
    
    /**
     * Since they are two type of perms (interaction perms and role perms)
     * we dodge here issue with missing permission for custom emote by sending a classic message
     * instead of answering directly to the interaction.
     * 
     * More info : https://github.com/discord/discord-api-docs/discussions/3307
     */
    if(event.getGuild() != null && 
        event.getGuild().getPublicRole().hasPermission(Permission.MESSAGE_EXT_EMOJI) 
        && event.getGuild().getPublicRole().hasPermission(Permission.MESSAGE_EMBED_LINKS)) {
    event.getHook().editOriginalEmbeds(
        SubscriptionCommandRunnable.executeCommand(language, user)).queue();
    }else {
      event.getHook().editOriginal(LanguageManager.getText(language, "loading")).queue();
      event.getChannel().sendMessageEmbeds(
          SubscriptionCommandRunnable.executeCommand(language, user)).queue();
    }
  }

  
}
