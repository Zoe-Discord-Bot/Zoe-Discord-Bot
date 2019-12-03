package ch.kalunight.zoe.model.config.option;

import java.awt.Color;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.ButtonMenu;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.player_data.Player;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.requests.restaction.RoleAction;

public class RoleOption extends ConfigurationOption {

  private Role role;

  public RoleOption() {
    super("player_role", "roleOptionDescription");
    role = null;
  }

  @Override
  public Consumer<CommandEvent> getChangeConsumer(EventWaiter waiter) {
    return new Consumer<CommandEvent>() {

      @Override
      public void accept(CommandEvent event) {
        
        Server server = ServerData.getServers().get(event.getGuild().getId());
        
        if(!event.getGuild().getSelfMember().getPermissions().contains(Permission.MANAGE_ROLES)) {
          event.reply(LanguageManager.getText(server.getLangage(), "roleOptionPermissionNeeded"));
          return;
        }

        ButtonMenu.Builder choiceBuilder = new ButtonMenu.Builder();

        choiceBuilder.setEventWaiter(waiter);
        choiceBuilder.addChoices("✅","❌");
        choiceBuilder.addUsers(event.getAuthor());
        choiceBuilder.setFinalAction(finalAction());
        choiceBuilder.setColor(Color.BLUE);

        choiceBuilder.setTimeout(2, TimeUnit.MINUTES);

        if(role == null) {
          choiceBuilder.setText(String.format(LanguageManager.getText(server.getLangage(), "roleOptionLongDesc"), 
              LanguageManager.getText(server.getLangage(), description)));

          choiceBuilder.setAction(receiveValidationAndCreateOption(event.getChannel(), event.getGuild(), server.getLangage()));

          ButtonMenu menu = choiceBuilder.build();

          menu.display(event.getChannel());

        }else {
          choiceBuilder.setText(String.format(LanguageManager.getText(server.getLangage(), "roleOptionLongDescDisable"), description));

          choiceBuilder.setAction(receiveValidationAndDisableOption(event.getChannel(), event.getGuild(), server.getLangage()));

          ButtonMenu menu = choiceBuilder.build();

          menu.display(event.getChannel());
        }
      }};
  }

  private Consumer<ReactionEmote> receiveValidationAndCreateOption(MessageChannel channel, Guild guild, String langage) {
    return new Consumer<ReactionEmote>() {

      @Override
      public void accept(ReactionEmote emoteUsed) {
        channel.sendTyping().complete();
        if(emoteUsed.getName().equals("✅")) {
          channel.sendMessage(LanguageManager.getText(langage, "roleOptionActivateWaitMessage")).complete();
          channel.sendTyping().complete();
          RoleAction action = guild.createRole();
          action.setName("Zoe-Player");
          action.setMentionable(false);
          action.setColor(Color.PINK);
          role = action.complete();

          Server server = ServerData.getServers().get(guild.getId());
          for(Player player : server.getPlayers()) {
            Member member = guild.getMember(player.getDiscordUser());
            guild.addRoleToMember(member, role).queue();
          }

          if(server.getInfoChannel() != null) {
            PermissionOverride permissionZoePlayer = server.getInfoChannel().putPermissionOverride(role).complete();
            permissionZoePlayer.getManager().grant(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY).complete();

            PermissionOverride permissionZoe = server.getInfoChannel()
                .putPermissionOverride(server.getGuild().getMember(Zoe.getJda().getSelfUser())).complete();
            permissionZoe.getManager().grant(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY).complete();

            PermissionOverride everyone = server.getInfoChannel().putPermissionOverride(server.getGuild().getPublicRole()).complete();
            everyone.getManager().deny(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY).complete();
          }
          channel.sendMessage(LanguageManager.getText(langage, "roleOptionDoneMessage")).complete();
        }else {
          channel.sendMessage(LanguageManager.getText(langage, "roleOptionCancelMessage")).queue();
        }
      }};

  }

  private Consumer<ReactionEmote> receiveValidationAndDisableOption(MessageChannel channel, Guild guild, String langage) {
    return new Consumer<ReactionEmote>() {
      
      @Override
      public void accept(ReactionEmote emoteUsed) {
        channel.sendTyping().complete();
        if(emoteUsed.getName().equals("✅")) {
          channel.sendMessage(LanguageManager.getText(langage, "roleOptionDisableWaitMessage")).complete();
          channel.sendTyping().complete();
          role.delete().complete();

          Server server = ServerData.getServers().get(guild.getId());

          if(server != null && server.getInfoChannel() != null) {
            PermissionOverride everyone = server.getInfoChannel().getPermissionOverride(server.getGuild().getPublicRole());
            everyone.getManager().grant(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY).complete();
          }

          role = null;
          channel.sendMessage(LanguageManager.getText(langage, "roleOptionDoneMessageDisable")).queue();
        }else {
          channel.sendMessage(LanguageManager.getText(langage, "roleOptionDoneMessageStillActivate")).queue();
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
  public String getChoiceText(String langage) {
    String status = LanguageManager.getText(langage, "optionDisable");
    if(role != null) {
      status = LanguageManager.getText(langage, "optionEnable");
    }
    return LanguageManager.getText(langage, description) + " : " + status;
  }

  @Override
  public String getSave() {
    String save = NO_VALUE_REPRESENTATION;
    if(role != null) {
      save = role.getId() + ":" + role.getGuild().getId();
    }
    return id + ":" + save;
  }

  @Override
  public void restoreSave(String save) {
    String[] saveDatas = save.split(":");

    if(!saveDatas[1].equals(NO_VALUE_REPRESENTATION)) {
      Guild guild = Zoe.getJda().getGuildById(saveDatas[2]);
      if(guild != null) {
        role = guild.getRoleById(saveDatas[1]);
      }
    }
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }
}
