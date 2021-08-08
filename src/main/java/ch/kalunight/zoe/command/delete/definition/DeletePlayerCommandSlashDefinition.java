package ch.kalunight.zoe.command.delete.definition;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.command.delete.DeletePlayerRunnable;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class DeletePlayerCommandSlashDefinition extends ZoeSlashCommand {

  public DeletePlayerCommandSlashDefinition(String serverId) {
    this.name = DeletePlayerRunnable.USAGE_NAME;
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "deletePlayerHelpMessageSlashCommand");

    List<OptionData> data = new ArrayList<>();
    
    OptionData discordAccountToLink = new OptionData(OptionType.USER, ZoeSlashCommand.USER_OPTION_ID, "The discord account you want to delete.");
    discordAccountToLink.setRequired(true);
    
    data.add(discordAccountToLink);
    
    this.options = data;
    
    if(serverId == null) {
      this.guildOnly = true;
    }else {
      this.guildOnly = true; //True for testing
      this.guildId = serverId; //Test server
    }
  }

  @Override
  protected void executeCommand(SlashCommandEvent event) throws SQLException {
    Member member = event.getOption(ZoeSlashCommand.USER_OPTION_ID).getAsMember();
    
    List<Member> mentionnedMembers = new ArrayList<>();
    
    mentionnedMembers.add(member);
    
    String message = DeletePlayerRunnable.executeCommand(ZoeCommand.getServer(event.getGuild().getIdLong()), event.getMember(), mentionnedMembers);
    
    event.getHook().editOriginal(message).queue();
  }
  
}
