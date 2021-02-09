package ch.kalunight.zoe.command.define;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.RankHistoryChannelRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;

public class DefineRankChannelCommand extends ZoeCommand {

  public DefineRankChannelCommand() {
    this.name = "rankChannel";
    this.arguments = "#mentionOfTheChannel";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.help = "defineRankChannelHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(DefineCommand.USAGE_NAME, name, arguments, help);
  }
  
  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    DTO.Server server = getServer(event.getGuild().getIdLong());

    DTO.RankHistoryChannel rankChannel = RankHistoryChannelRepository.getRankHistoryChannel(server.serv_guildId);

    if(rankChannel != null) {
      TextChannel textChannel = event.getGuild().getTextChannelById(rankChannel.rhChannel_channelId);
      event.reply(String.format(LanguageManager.getText(server.getLanguage(), "defineInfoChannelAlreadySet"), //Translation for both channel
          textChannel.getAsMention()));
    } else {
      if(event.getMessage().getMentionedChannels().size() != 1) {
        event.reply(LanguageManager.getText(server.getLanguage(), "defineRankChannelMentionOfAChannelNeeded"));
      } else {
        TextChannel textChannel = event.getMessage().getMentionedChannels().get(0);

        if(textChannel.getGuild().getIdLong() != server.serv_guildId) {
          event.reply(LanguageManager.getText(server.getLanguage(), "defineInfoChannelMentionOfAChannel")); //Translation for both channel

        } else {
          if(!event.getMessage().getMentionedChannels().get(0).canTalk()) {
            event.reply(LanguageManager.getText(server.getLanguage(), "defineInfoChannelMissingSpeakPermission")); //Translation for both channel
          } else {
            ServerConfiguration config = ConfigRepository.getServerConfiguration(server.serv_guildId, event.getJDA());
            if(textChannel.equals(config.getCleanChannelOption().getCleanChannel())) {
              event.reply(LanguageManager.getText(server.getLanguage(), "defineRankChannelImpossibleToDefineCleanChannel"));
            }else {
              RankHistoryChannelRepository.createRankHistoryChannel(server.serv_id, textChannel.getIdLong());
              
              if(config.getZoeRoleOption().getRole() != null) {
                CommandUtil.giveRolePermission(event.getGuild(), textChannel, config);
              }
              
              event.reply(LanguageManager.getText(server.getLanguage(), "defineRankChannelDoneMessage"));
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
