package ch.kalunight.zoe.model.config.option;

import java.awt.Color;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.ButtonMenu;

import ch.kalunight.zoe.model.CommandGuildDiscordData;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Player;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.InfoChannelRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.repositories.RankHistoryChannelRepository;
import ch.kalunight.zoe.repositories.RepoRessources;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.RoleAction;

public class RoleOption extends ConfigurationOption {

  private static final Logger logger = LoggerFactory.getLogger(RoleOption.class);
  
  private Role role;

  public RoleOption(long guildId) {
    super(guildId, "roleOptionName", "roleOptionDescription", OptionCategory.FEATURES, false);
    role = null;
  }

  @Override
  public Consumer<CommandGuildDiscordData> getChangeConsumer(EventWaiter waiter, DTO.Server server) {
    return new Consumer<CommandGuildDiscordData>() {

      @Override
      public void accept(CommandGuildDiscordData event) {

        if(!event.getGuild().getSelfMember().getPermissions().contains(Permission.MANAGE_ROLES)) {
          event.getChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "roleOptionPermissionNeeded")).queue();
          return;
        }

        ButtonMenu.Builder choiceBuilder = new ButtonMenu.Builder();

        choiceBuilder.setEventWaiter(waiter);
        choiceBuilder.addChoices("✅","❌");
        choiceBuilder.addUsers(event.getUser());
        choiceBuilder.setFinalAction(finalAction());
        choiceBuilder.setColor(Color.BLUE);

        choiceBuilder.setTimeout(2, TimeUnit.MINUTES);

        if(role == null) {
          choiceBuilder.setText(String.format(LanguageManager.getText(server.getLanguage(), "roleOptionLongDesc"), 
              LanguageManager.getText(server.getLanguage(), description)));

          choiceBuilder.setAction(receiveValidationAndCreateOption(event.getChannel(), event.getGuild(), server));

          ButtonMenu menu = choiceBuilder.build();

          menu.display(event.getChannel());

        }else {
          choiceBuilder.setText(String.format(LanguageManager.getText(server.getLanguage(), "roleOptionLongDescDisable"), description));

          choiceBuilder.setAction(receiveValidationAndDisableOption(event.getChannel(), event.getGuild(), server));

          ButtonMenu menu = choiceBuilder.build();

          menu.display(event.getChannel());
        }
      }};
  }

  private Consumer<ReactionEmote> receiveValidationAndCreateOption(MessageChannel channel, Guild guild, DTO.Server server) {
    return new Consumer<ReactionEmote>() {

      @Override
      public void accept(ReactionEmote emoteUsed) {
        channel.sendTyping().complete();
        try {
          if(emoteUsed.getName().equals("✅")) {
            channel.sendMessage(LanguageManager.getText(server.getLanguage(), "roleOptionActivateWaitMessage")).complete();
            channel.sendTyping().complete();
            RoleAction action = guild.createRole();
            action.setName("Zoe-Player");
            action.setMentionable(false);
            action.setColor(Color.PINK);
            role = action.complete();
            try {
              ConfigRepository.updateRoleOption(guildId, role.getIdLong(), guild.getJDA());
            } catch (SQLException e) {
              RepoRessources.sqlErrorReport(channel, server, e);
              return;
            }


            for(Player player : PlayerRepository.getPlayers(guildId)) {
              Member member = guild.retrieveMember(player.retrieveUser(guild.getJDA())).complete();
              guild.addRoleToMember(member, role).queue();
            }

            DTO.InfoChannel infochannelDb = InfoChannelRepository.getInfoChannel(guildId);
            if(infochannelDb != null) {
              TextChannel infochannel = guild.getTextChannelById(infochannelDb.infochannel_channelid);
              PermissionOverride permissionZoePlayer = infochannel.putPermissionOverride(role).complete();
              permissionZoePlayer.getManager().grant(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY).complete();

              PermissionOverride permissionZoe = infochannel
                  .putPermissionOverride(guild.retrieveMember(guild.getJDA().getSelfUser()).complete()).complete();
              permissionZoe.getManager().grant(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY).complete();

              PermissionOverride everyone = infochannel.putPermissionOverride(guild.getPublicRole()).complete();
              everyone.getManager().deny(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY).complete();
            }
            
            DTO.RankHistoryChannel rankChannelDb = RankHistoryChannelRepository.getRankHistoryChannel(guildId);
            if(rankChannelDb != null) {
              TextChannel rankChannel = guild.getTextChannelById(rankChannelDb.rhChannel_channelId);
              PermissionOverride permissionZoePlayer = rankChannel.putPermissionOverride(role).complete();
              permissionZoePlayer.getManager().grant(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY).complete();

              PermissionOverride permissionZoe = rankChannel
                  .putPermissionOverride(guild.getMember(guild.getJDA().getSelfUser())).complete();
              permissionZoe.getManager().grant(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY).complete();

              PermissionOverride everyone = rankChannel.putPermissionOverride(guild.getPublicRole()).complete();
              everyone.getManager().deny(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY).complete();
            }
            
            channel.sendMessage(LanguageManager.getText(server.getLanguage(), "roleOptionDoneMessage")).complete();
          }else {
            channel.sendMessage(LanguageManager.getText(server.getLanguage(), "roleOptionCancelMessage")).queue();
          }
        }catch(SQLException e) {
          logger.error("SQL Error when configure role option !", e);
          channel.sendMessage(LanguageManager.getText(server.getLanguage(), "errorSQLPleaseReport")).queue();
        }
      }};

  }

  private Consumer<ReactionEmote> receiveValidationAndDisableOption(MessageChannel channel, Guild guild, DTO.Server server) {
    return new Consumer<ReactionEmote>() {

      @Override
      public void accept(ReactionEmote emoteUsed) {
        channel.sendTyping().complete();
        if(emoteUsed.getName().equals("✅")) {
          channel.sendMessage(LanguageManager.getText(server.getLanguage(), "roleOptionDisableWaitMessage")).complete();
          channel.sendTyping().complete();
          role.delete().complete();
          try {
            ConfigRepository.updateRoleOption(guildId, 0, guild.getJDA());
          } catch (SQLException e) {
            RepoRessources.sqlErrorReport(channel, server, e);
            return;
          }

          DTO.InfoChannel infoChannelDb;
          try {
            infoChannelDb = InfoChannelRepository.getInfoChannel(server.serv_guildId);
          } catch(SQLException e) {
            logger.error("SQL Error when configure role option !", e);
            channel.sendMessage(LanguageManager.getText(server.getLanguage(), "errorSQLPleaseReport")).queue();
            return;
          }
          
          if(infoChannelDb != null) {
            TextChannel infochannel = guild.getTextChannelById(guildId);
            PermissionOverride everyone = infochannel.getPermissionOverride(guild.getPublicRole());
            everyone.getManager().grant(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY).complete();
          }

          role = null;
          channel.sendMessage(LanguageManager.getText(server.getLanguage(), "roleOptionDoneMessageDisable")).queue();
        }else {
          channel.sendMessage(LanguageManager.getText(server.getLanguage(), "roleOptionDoneMessageStillActivate")).queue();
        }
      }};
  }

  private Consumer<Message> finalAction(){
    return new Consumer<Message>() {

      @Override
      public void accept(Message message) {
        message.clearReactions().complete();
      }};
  }

  @Override
  public String getBaseChoiceText(String langage) {
    String status = LanguageManager.getText(langage, "optionDisable");
    if(role != null) {
      status = LanguageManager.getText(langage, "optionEnable");
    }
    return LanguageManager.getText(langage, description) + " : " + status;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }
}
