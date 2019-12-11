package ch.kalunight.zoe.command.delete;

import java.sql.SQLException;
import java.util.List;
import java.util.function.BiConsumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

public class DeletePlayerCommand extends ZoeCommand {

  public static final String USAGE_NAME = "player";

  public DeletePlayerCommand() {
    this.name = USAGE_NAME;
    this.help = "deletePlayerHelpMessage";
    this.arguments = "@DiscordMentionOfPlayer";
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(DeleteCommand.USAGE_NAME, USAGE_NAME, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    event.getTextChannel().sendTyping().complete();
    
    DTO.Server server = getServer(event.getGuild().getIdLong());
    
    ServerConfiguration config = ConfigRepository.getServerConfiguration(server.serv_guildId);
    
    if(!config.getUserSelfAdding().isOptionActivated() && 
        !event.getMember().getPermissions().contains(Permission.MANAGE_CHANNEL)) {
      
        event.reply(String.format(LanguageManager.getText(server.serv_language, "deletePlayerMissingPermission"),
            Permission.MANAGE_CHANNEL.getName()));
        return;
    }
    
    List<Member> members = event.getMessage().getMentionedMembers();

    if(members.size() != 1) {
      event.reply(LanguageManager.getText(server.serv_language, "deletePlayerMissingMentionPlayer"));
    } else {
      User user = members.get(0).getUser();
      if(!user.equals(event.getAuthor()) && !event.getMember().getPermissions().contains(Permission.MANAGE_CHANNEL)) {
        event.reply(String.format(LanguageManager.getText(server.serv_language, "deletePlayerOtherPlayerMissingPermission"), 
            Permission.MANAGE_CHANNEL.getName()));
        return;
      }
      
      DTO.Player player = PlayerRepository.getPlayer(server.serv_guildId, user.getIdLong());
      
      if(player == null) {
        event.reply(String.format(LanguageManager.getText(server.serv_language, "deletePlayerUserNotRegistered"), user.getName()));
      } else {
        PlayerRepository.deletePlayer(player.player_id);
        if(config.getZoeRoleOption().getRole() != null) {
          Member member = event.getGuild().getMember(user);
          if(member != null) {
            event.getGuild().removeRoleFromMember(member, config.getZoeRoleOption().getRole()).queue();
          }
        }
        event.reply(String.format(LanguageManager.getText(server.serv_language, "deletePlayerDoneMessage"),
            player.user.getName()));
      }
    }
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }
}
