package ch.kalunight.zoe.command.create;

import java.sql.SQLException;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

public class CreateRankHistoryChannelCommand extends ZoeCommand {

  private static final Logger logger = LoggerFactory.getLogger(CreateRankHistoryChannelCommand.class);

  public CreateRankHistoryChannelCommand() {
    this.name = "rankChannel";
    this.arguments = "nameOfTheNewChannel";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    this.guildOnly = true;
    this.help = "createRankChannelHelpMessage";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(CreateCommand.USAGE_NAME, name, arguments, help);
  }


  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {

    DTO.Server server = getServer(event.getGuild().getIdLong());

    String nameChannel = event.getArgs();

    if(nameChannel == null || nameChannel.equals("")) {
      event.reply(LanguageManager.getText(server.serv_language, "nameOfInfochannelNeeded"));
      return;
    }

    if(nameChannel.length() > 100) {
      event.reply(LanguageManager.getText(server.serv_language, "nameOfTheInfoChannelNeedToBeLess100Characters"));
      return;
    }

    DTO.RankHistoryChannel rankChannelDb = RankHistoryChannelRepository.getRankHistoryChannel(event.getGuild().getIdLong());

    if(rankChannelDb != null && rankChannelDb.rhChannel_channelId != 0) {
      TextChannel rankChannel = event.getGuild().getTextChannelById(rankChannelDb.rhChannel_channelId);
      if(rankChannel == null) {
        RankHistoryChannelRepository.deleteRankHistoryChannel(rankChannelDb.rhChannel_id);
      }else {
        event.reply(String.format(LanguageManager.getText(server.serv_language, "rankChannelAlreadyExist"), rankChannel.getAsMention()));
        return;
      }
    }

    TextChannel rankChannel = event.getGuild().createTextChannel(nameChannel).complete();

    ServerConfiguration serverConfiguration = ConfigRepository.getServerConfiguration(event.getGuild().getIdLong());

    if(serverConfiguration.getZoeRoleOption().getRole() != null) {
      Role role = event.getGuild().getPublicRole();
      try {
        rankChannel.putPermissionOverride(role).deny(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY).queue();
        rankChannel.putPermissionOverride(serverConfiguration.getZoeRoleOption().getRole()).grant(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY).queue();
      }catch(InsufficientPermissionException e) {
        logger.info("Missing permission to apply Zoe role option rule, nothing bad");
      }
    }
    
    RankHistoryChannelRepository.createRankHistoryChannel(event.getGuild().getIdLong(), rankChannel.getIdLong());
    
    event.reply(LanguageManager.getText(server.serv_language, "rankChannelCorrectlyCreated"));
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
