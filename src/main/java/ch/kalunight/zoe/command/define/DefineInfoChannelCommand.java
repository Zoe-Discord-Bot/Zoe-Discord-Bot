package ch.kalunight.zoe.command.define;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.InfoChannelRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class DefineInfoChannelCommand extends ZoeCommand {

  public DefineInfoChannelCommand() {
    this.name = "infochannel";
    this.arguments = "#mentionOfTheChannel";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = "defineInfoChannelHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(DefineCommand.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    event.getTextChannel().sendTyping().complete();
    
    DTO.Server server = getServer(event.getGuild().getIdLong());

    DTO.InfoChannel infochannel = InfoChannelRepository.getInfoChannel(server.serv_guildId);

    if(infochannel != null) {
      TextChannel textChannel = event.getGuild().getTextChannelById(infochannel.infochannel_channelid);
      event.reply(String.format(LanguageManager.getText(server.serv_language, "defineInfoChannelAlreadySet"), 
          textChannel.getAsMention()));
    } else {
      if(event.getMessage().getMentionedChannels().size() != 1) {
        event.reply(LanguageManager.getText(server.serv_language, "defineInfoChannelMentionOfAChannelNeeded"));
      } else {
        TextChannel textChannel = event.getMessage().getMentionedChannels().get(0);

        if(textChannel.getGuild().getIdLong() != server.serv_guildId) {
          event.reply(LanguageManager.getText(server.serv_language, "defineInfoChannelMentionOfAChannel"));

        } else {
          if(!event.getMessage().getMentionedChannels().get(0).canTalk()) {
            event.reply(LanguageManager.getText(server.serv_language, "defineInfoChannelMissingSpeakPermission"));
          } else {
            ServerConfiguration config = ConfigRepository.getServerConfiguration(server.serv_guildId);
            if(textChannel.equals(config.getCleanChannelOption().getCleanChannel())) {
              event.reply(LanguageManager.getText(server.serv_language, "defineInfoChannelImpossibleToDefineCleanChannel"));
            }else {
              InfoChannelRepository.createInfoChannel(server.serv_id, textChannel.getIdLong());
              event.reply(LanguageManager.getText(server.serv_language, "defineInfoChannelDoneMessage"));

              Message message = textChannel.sendMessage(LanguageManager.getText(server.serv_language, "defineInfoChannelLoadingMessage"))
                  .complete();

              infochannel = InfoChannelRepository.getInfoChannel(server.serv_guildId);
              InfoChannelRepository.createInfoPanelMessage(infochannel.infoChannel_id, message.getIdLong());
              
              //InfoPanelRefresher infoPanelRefresher = new InfoPanelRefresher(server);
              //ServerData.getServerExecutor().submit(infoPanelRefresher);
            }
          }
        }
      }
    }
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
