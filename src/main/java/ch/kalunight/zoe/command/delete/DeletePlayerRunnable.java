package ch.kalunight.zoe.command.delete;

import java.sql.SQLException;
import java.util.List;
import ch.kalunight.zoe.model.config.ServerConfiguration;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

public class DeletePlayerRunnable {

  public static final String USAGE_NAME = "player";
  
  private DeletePlayerRunnable() {
    // hide default public constructor
  }

  public static String executeCommand(Server server, Member author, List<Member> mentionnedMembers) throws SQLException {
    
    ServerConfiguration config = ConfigRepository.getServerConfiguration(server.serv_guildId, author.getJDA());
    
    if(!config.getUserSelfAdding().isOptionActivated() && 
        !author.getPermissions().contains(Permission.MANAGE_CHANNEL)) {
      
        return String.format(LanguageManager.getText(server.getLanguage(), "deletePlayerMissingPermission"),
            Permission.MANAGE_CHANNEL.getName());
    }
    
    List<Member> members = mentionnedMembers;

    if(members.size() != 1) {
      return LanguageManager.getText(server.getLanguage(), "deletePlayerMissingMentionPlayer");
    } else {
      User user = members.get(0).getUser();
      if(!user.equals(author.getUser()) && !author.getPermissions().contains(Permission.MANAGE_CHANNEL)) {
        return String.format(LanguageManager.getText(server.getLanguage(), "deletePlayerOtherPlayerMissingPermission"), 
            Permission.MANAGE_CHANNEL.getName());
      }
      
      DTO.Player player = PlayerRepository.getPlayer(server.serv_guildId, user.getIdLong());
      
      if(player == null) {
        return String.format(LanguageManager.getText(server.getLanguage(), "deletePlayerUserNotRegistered"), user.getName());
      } else {
        if(config.getZoeRoleOption().getRole() != null) {
          Member member = author.getGuild().retrieveMemberById(user.getId()).complete();
          if(member != null) {
            author.getGuild().removeRoleFromMember(member, config.getZoeRoleOption().getRole()).queue();
          }
        }
        PlayerRepository.deletePlayer(player, server.serv_guildId);
        return String.format(LanguageManager.getText(server.getLanguage(), "deletePlayerDoneMessage"),
            player.retrieveUser(author.getJDA()).getName());
      }
    }
  }
}
