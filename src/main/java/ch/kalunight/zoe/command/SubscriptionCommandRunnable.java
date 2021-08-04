package ch.kalunight.zoe.command;

import java.util.List;

import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.ZoeUserManagementRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.ZoeUserRankManagementUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.entities.User;

public class SubscriptionCommandRunnable {

  private SubscriptionCommandRunnable() {
    // hide default public constructor
  }
  
  public static MessageEmbed executeCommand(Server server, User userToShow) {
    
    EmbedBuilder builder = new EmbedBuilder();
    
    builder.setAuthor(userToShow.getName(), null, userToShow.getEffectiveAvatarUrl());
    
    List<DTO.ZoeUserRole> assignedRoles = ZoeUserManagementRepository.getZoeSubscriptionByDiscordId(userToShow.getIdLong());
    List<DTO.Role> roles = ZoeUserRankManagementUtil.getRolesByAssignedRoles(assignedRoles);
    
    StringBuilder roleListBuilder = new StringBuilder();
    if(roles.isEmpty()) {
      roleListBuilder.append(String.format(LanguageManager.getText(server.getLanguage(), "subscriptionCommandNoRoles"), userToShow.getName()));
    }
    
    Field field = new Field(String.format(LanguageManager.getText(server.getLanguage(), "subscriptionCommandRoleTitle"), userToShow.getName()), "", false);
  }
}
