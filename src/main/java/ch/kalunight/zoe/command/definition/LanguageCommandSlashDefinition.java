package ch.kalunight.zoe.command.definition;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ch.kalunight.zoe.command.LanguageCommandRunnable;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.command.ZoeSlashCommand;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.RepoRessources;
import ch.kalunight.zoe.repositories.ServerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class LanguageCommandSlashDefinition extends ZoeSlashCommand {

  public LanguageCommandSlashDefinition(String serverId) {
    this.name = "language";
    this.help = LanguageManager.getText(LanguageManager.DEFAULT_LANGUAGE, "languageCommandHelp");
    this.hidden = false;
    this.ownerCommand = false;
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL, Permission.MESSAGE_ADD_REACTION};
    this.userPermissions = permissionRequired;
    Permission[] permissionBot = {Permission.MESSAGE_ADD_REACTION, Permission.MANAGE_EMOTES, Permission.MESSAGE_EMBED_LINKS};
    this.botPermissions = permissionBot;

    List<OptionData> data = new ArrayList<>();
    OptionData languageOption = new OptionData(OptionType.STRING, "language", "The language you want to define");
    languageOption.setRequired(true);
    
    for(String langage : LanguageManager.getListlanguages()) {
      languageOption.addChoice(LanguageManager.getText(langage, LanguageCommandRunnable.NATIVE_LANGUAGE_TRANSLATION_ID), langage);
    }

    data.add(languageOption);
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
    String language = event.getOption("language").getAsString();

    if(!LanguageManager.getListlanguages().contains(language)) {
      event.getHook().editOriginal("languageSlashCommandOptionNotValid").queue();
      return;
    }
    
    Server server = ZoeCommand.getServer(event.getGuild().getIdLong());

    try {
      ServerRepository.updateLanguage(server.serv_guildId, language);
      server.setLanguage(language);
    } catch (SQLException e) {
      RepoRessources.sqlErrorReportSlashResponse(event, server, e);
      return;
    }

    event.getHook().editOriginal(String.format(LanguageManager.getText(server.getLanguage(), "languageCommandSelected"),
        LanguageManager.getText(server.getLanguage(), LanguageCommandRunnable.NATIVE_LANGUAGE_TRANSLATION_ID))).queue();
  }

}
