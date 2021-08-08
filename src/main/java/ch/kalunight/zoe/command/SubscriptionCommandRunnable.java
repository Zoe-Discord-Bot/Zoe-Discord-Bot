package ch.kalunight.zoe.command;

import java.awt.Color;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.sub.UserBenefit;
import ch.kalunight.zoe.model.sub.UserRank;
import ch.kalunight.zoe.repositories.ZoeUserManagementRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.ZoeUserRankManagementUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.entities.User;

public class SubscriptionCommandRunnable {

  private static final Logger logger = LoggerFactory.getLogger(SubscriptionCommandRunnable.class);
  
  private SubscriptionCommandRunnable() {
    // hide default public constructor
  }

  public static MessageEmbed executeCommand(String language, User userToShow, Guild guild) throws SQLException {

    EmbedBuilder builder = new EmbedBuilder();
    builder.setColor(Color.GREEN);
    
    builder.setAuthor(userToShow.getName(), null, userToShow.getEffectiveAvatarUrl());
    DTO.ZoeUser userSubData;
    List<DTO.ZoeUserRole> assignedRoles;
    List<DTO.Role> roles;
    try {
      userSubData = ZoeUserManagementRepository.getZoeUserByDiscordId(userToShow.getIdLong());
      if(userSubData == null) {
        ZoeUserManagementRepository.createZoeUser(userToShow.getIdLong());
        userSubData = ZoeUserManagementRepository.getZoeUserByDiscordId(userToShow.getIdLong());
      }
      
      assignedRoles = ZoeUserManagementRepository.getZoeSubscriptionByDiscordId(userToShow.getIdLong());
      roles = ZoeUserRankManagementUtil.getRolesByAssignedRoles(assignedRoles);
    } catch (SQLException e) {
      logger.error("SQL error in subscription command!", e);
      builder.setTitle(LanguageManager.getText(language, "errorSQLPleaseReport"));
      return builder.build();
    }

    StringBuilder roleListBuilder = new StringBuilder();
    if(roles.isEmpty()) {
      roleListBuilder.append(String.format(LanguageManager.getText(language, "subscriptionCommandNoRoles"), userToShow.getName()));
    }
    
    List<UserRank> allUserRanks = new ArrayList<>();
    int i = 0;
    for(DTO.Role role : roles) {
      UserRank rank = UserRank.getUserRankByRoleId(role.role_roleId);
      roleListBuilder.append(rank.getEmoteWithUser(userSubData) + " " +
          LanguageManager.getText(language, rank.getNameId()));
      allUserRanks.add(rank);
      i++;
      
      if(roles.size() != i) {
        roleListBuilder.append("\n");
      }
    }

    Field field = new Field(String.format(LanguageManager.getText(
        language, "subscriptionCommandRoleTitle"), userToShow.getName()),
        roleListBuilder.toString(), false);
    builder.addField(field);
    
    List<UserBenefit> benefits = UserBenefit.getBenefitsByUserRanks(allUserRanks);
    StringBuilder benefitBuilder = new StringBuilder();
    
    addBenefitToBenefitStringBuilder(language, benefits, benefitBuilder, UserBenefit.EVOLUTIVE_SUPPORTER_EMOTE);
    benefitBuilder.append("\n");
    addBenefitToBenefitStringBuilder(language, benefits, benefitBuilder, UserBenefit.NAME_ON_WEBSITE);
    benefitBuilder.append("\n");
    addBenefitToBenefitStringBuilder(language, benefits, benefitBuilder, UserBenefit.EXCLUSIVE_DEV_NEWS);
    benefitBuilder.append("\n");
    addBenefitToBenefitStringBuilder(language, benefits, benefitBuilder, UserBenefit.EXCLUSIVE_ACCESS_SUPPORTER_CHANNEL);
    benefitBuilder.append("\n");
    addBenefitToBenefitStringBuilder(language, benefits, benefitBuilder, UserBenefit.SUPPORT_THE_PROJECT);
    
    field = new Field(LanguageManager.getText(language, "subscriptionCommandBenefitTier1Title"), benefitBuilder.toString(), false);
    builder.addField(field);
    
    benefitBuilder = new StringBuilder();
    
    addBenefitToBenefitStringBuilder(language, benefits, benefitBuilder, UserBenefit.ACCESS_NEW_FEATURES_SMALL_AND_MEDIUM_SERVERS);
    benefitBuilder.append("\n");
    addBenefitToBenefitStringBuilder(language, benefits, benefitBuilder, UserBenefit.BUG_REPORT_PRIORITY);
    
    field = new Field(LanguageManager.getText(language, "subscriptionCommandBenefitTier2Title"), benefitBuilder.toString(), false);
    builder.addField(field);
    
    benefitBuilder = new StringBuilder();
    
    addBenefitToBenefitStringBuilder(language, benefits, benefitBuilder, UserBenefit.ACCESS_NEW_FEATURES_HUGE_SERVERS);
    benefitBuilder.append("\n");
    addBenefitToBenefitStringBuilder(language, benefits, benefitBuilder, UserBenefit.NAME_ON_RELEASE_NOTE);
    benefitBuilder.append("\n");
    addBenefitToBenefitStringBuilder(language, benefits, benefitBuilder, UserBenefit.NAME_ON_GITHUB);

    field = new Field(LanguageManager.getText(language, "subscriptionCommandBenefitTier3Title"), benefitBuilder.toString(), false);
    builder.addField(field);
    
    if(guild != null) {
      StringBuilder serverStatusBuilder = new StringBuilder();
      
      if(ZoeUserRankManagementUtil.isServerHaveAccessToEarlyAccessFeature(guild)) {
        serverStatusBuilder.append("✅ " + 
            LanguageManager.getText(language, "subscriptionCommandServerAccessNewFeatures"));
      }else {
        serverStatusBuilder.append("❌ " +
            LanguageManager.getText(language, "subscriptionCommandServerAccessNewFeatures"));
        
        Member member = guild.retrieveMember(userToShow).complete();
        
        if((benefits.contains(UserBenefit.ACCESS_NEW_FEATURES_HUGE_SERVERS) 
            || (benefits.contains(UserBenefit.ACCESS_NEW_FEATURES_SMALL_AND_MEDIUM_SERVERS)
                && UserBenefit.HUGE_SERVERS_START >= guild.getMemberCount()))
            && member.hasPermission(Permission.ADMINISTRATOR)) {
          
          serverStatusBuilder.append("\n"
            + LanguageManager.getText(language, "subscriptionCommandServerAccessNewFeaturesAdminNotRegistered"));
        }
      }
      
      field = new Field(LanguageManager.getText(language, "subscriptionCommandServerStatus"),
          serverStatusBuilder.toString(),
          false);
      builder.addField(field);
    }
    
    if(benefits.contains(UserBenefit.SUPPORT_THE_PROJECT)) {
      field = new Field(LanguageManager.getText(language, "subscriptionCommandUserSupportTitle"),
          LanguageManager.getText(language, "subscriptionCommandUserSupport"),
          false);
    }else {
      field = new Field(LanguageManager.getText(language, "subscriptionCommandUserNotSupportTitle"),
          LanguageManager.getText(language, "subscriptionCommandUserNotSupport"),
          false);
    }
    builder.addField(field);
    
    builder.setFooter(LanguageManager.getText(language, "subscriptionCommandUserFooterMessage"));
    
    return builder.build();
  }

  private static void addBenefitToBenefitStringBuilder(String language, List<UserBenefit> benefits,
      StringBuilder benefitBuilder, UserBenefit benefit) {
    if(benefits.contains(benefit)) {
      benefitBuilder.append("✅ ");
    }else {
      benefitBuilder.append("❌ ");
    }
    benefitBuilder.append(LanguageManager.getText(language, benefit.getNameId()));
  }
}
