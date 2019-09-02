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
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.core.entities.PermissionOverride;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.managers.GuildController;
import net.dv8tion.jda.core.requests.restaction.RoleAction;

public class RoleOption extends ConfigurationOption {

  private Role role;

  public RoleOption() {
    super("player_role", "Hide the infochannel to non-player with a role");
    role = null;
  }

  @Override
  public Consumer<CommandEvent> getChangeConsumer(EventWaiter waiter) {
    return new Consumer<CommandEvent>() {

      @Override
      public void accept(CommandEvent event) {
        
        if(!event.getGuild().getSelfMember().getPermissions().contains(Permission.MANAGE_ROLES)) {
          event.reply("I need the manage roles permission to activate this option. "
              + "Please give me this permission if you want to activate this option.");
          return;
        }

        ButtonMenu.Builder choiceBuilder = new ButtonMenu.Builder();

        choiceBuilder.setEventWaiter(waiter);
        choiceBuilder.addChoices("✅","❌");
        choiceBuilder.addUsers(event.getAuthor());
        choiceBuilder.setFinalAction(finalAction());

        choiceBuilder.setTimeout(2, TimeUnit.MINUTES);

        if(role == null) {
          choiceBuilder.setText("Option in activation : **" + description + "**\n\n"
              + "Activate this option will create a role named \"Zoe-Player\" and assigne it to all players registered. "
              + "You can update it like you want. If it got deleted, the option will be disable automatically.\n\n"
              + ":white_check_mark: : Activate this option\n"
              + ":x: : Cancel the activation");

          choiceBuilder.setAction(receiveValidationAndCreateOption(event.getChannel(), event.getGuild()));

          ButtonMenu menu = choiceBuilder.build();

          menu.display(event.getChannel());

        }else {
          choiceBuilder.setText("Option you want to disable : **" + description + "**\n\n"
              + "Disable it will **delete the Zoe-Player role** and will remake infochannel readable by everyone.\n\n"
              + ":white_check_mark: : **Disable** the option.\n"
              + ":x: : Cancel the deactivation");

          choiceBuilder.setAction(receiveValidationAndDisableOption(event.getChannel(), event.getGuild()));

          ButtonMenu menu = choiceBuilder.build();

          menu.display(event.getChannel());
        }
      }};
  }

  private Consumer<ReactionEmote> receiveValidationAndCreateOption(MessageChannel channel, Guild guild) {
    return new Consumer<ReactionEmote>() {

      @Override
      public void accept(ReactionEmote emoteUsed) {
        channel.sendTyping().complete();
        if(emoteUsed.getName().equals("✅")) {
          channel.sendMessage("Right, i will activate that quickly. Wait 2 seconds...").queue();
          channel.sendTyping().complete();
          GuildController guildController = guild.getController();
          RoleAction action = guildController.createRole();
          action.setName("Zoe-Player");
          action.setMentionable(false);
          action.setColor(Color.PINK);
          role = action.complete();

          Server server = ServerData.getServers().get(guild.getId());
          for(Player player : server.getPlayers()) {
            Member member = guild.getMember(player.getDiscordUser());
            guildController.addSingleRoleToMember(member, role).queue();
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
          channel.sendMessage("The configuration has ended ! Now only registered player can see the infoChannel.").complete();
        }else {
          channel.sendMessage("Well, if you have one day changed you mind. You know where i am.").queue();
        }
      }};

  }

  private Consumer<ReactionEmote> receiveValidationAndDisableOption(MessageChannel channel, Guild guild) {
    return new Consumer<ReactionEmote>() {
      
      @Override
      public void accept(ReactionEmote emoteUsed) {
        channel.sendTyping().complete();
        if(emoteUsed.getName().equals("✅")) {
          channel.sendMessage("Right, i disable the option. Please wait 2 seconds...").complete();
          channel.sendTyping().complete();
          role.delete().complete();

          Server server = ServerData.getServers().get(guild.getId());

          if(server != null && server.getInfoChannel() != null) {
            PermissionOverride everyone = server.getInfoChannel().getPermissionOverride(server.getGuild().getPublicRole());
            everyone.getManager().grant(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY).complete();
          }

          role = null;
          channel.sendMessage("The option has been disable !").queue();
        }else {
          channel.sendMessage("Right, the option is still activate.").queue();
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
  public String getChoiceText() {
    String status = "Disable";
    if(role != null) {
      status = "Enable";
    }
    return description + " : " + status;
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
