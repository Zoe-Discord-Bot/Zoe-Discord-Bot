package ch.kalunight.zoe.model.config.option;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.player_data.Player;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.PermissionOverride;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
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

        if(role == null) {
          event.reply("Option in activation : **" + description + "**\n\n"
              + "Activate this option will create a role named \"Zoe-Player\" and assigne it to all players registered. "
              + "You can update it like you want. "
              + "If it got deleted, the option will be disable automatically."
              + "\n\nIf you want to activate this option, say **Yes**. If finally no, you can respond by **No** (or something other than Yes).");

          waiter.waitForEvent(MessageReceivedEvent.class,
              e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel()), e -> receiveValidationAndCreateOption(e), 1,
              TimeUnit.MINUTES, () -> endRegistrationTime(event.getEvent()));

        }else {
          event.reply("This option it's currently **activated**.\n\n"
              + "Disable it will **delete the Zoe-Player role** and will remake infochannel readable by everyone."
              + "\n\nIf you want disable the option, send **Disable**. "
              + "If you don't want to disable it, say **Stop** (or somthing other than Disable).");

          waiter.waitForEvent(MessageReceivedEvent.class,
              e -> e.getAuthor().equals(event.getAuthor()) && e.getChannel().equals(event.getChannel()), e -> receiveValidationAndDisableOption(e), 1,
              TimeUnit.MINUTES, () -> endRegistrationTime(event.getEvent()));
        }
      }};
  }
  
  private void receiveValidationAndDisableOption(MessageReceivedEvent event) {
    event.getChannel().sendTyping().queue();
    String message = event.getMessage().getContentRaw();
    if(message.equalsIgnoreCase("Disable")) {
      event.getChannel().sendMessage("Right, i disable the option. Please wait 2 seconds...").complete();
      event.getChannel().sendTyping().complete();
      role.delete().complete();
      
      Server server = ServerData.getServers().get(event.getGuild().getId());
      
      if(server != null && server.getInfoChannel() != null) {
        PermissionOverride everyone = server.getInfoChannel().getPermissionOverride(server.getGuild().getPublicRole());
        everyone.getManager().grant(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY).complete();
      }
      
      event.getChannel().sendMessage("The option has been disable !").queue();
    }else {
      event.getChannel().sendMessage("Right, the option is still activate.").queue();
    }
  }

  private void receiveValidationAndCreateOption(MessageReceivedEvent event) {
    event.getChannel().sendTyping().queue();
    String message = event.getMessage().getContentRaw();
    if(message.equalsIgnoreCase("Yes")) {
      event.getChannel().sendMessage("Right, i will activate that quickly. Wait 2 seconds...").queue();
      event.getChannel().sendTyping().complete();
      GuildController guildController = event.getGuild().getController();
      RoleAction action = guildController.createRole();
      action.setName("Zoe-Player");
      action.setMentionable(false);
      role = action.complete();

      Server server = ServerData.getServers().get(event.getGuild().getId());
      for(Player player : server.getPlayers()) {
        Member member = event.getGuild().getMember(player.getDiscordUser());
        guildController.addSingleRoleToMember(member, role).queue();
      }

      if(server.getInfoChannel() != null) {
        PermissionOverride permissionZoePlayer = server.getInfoChannel().putPermissionOverride(role).complete();
        permissionZoePlayer.getManager().grant(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY).complete();

        PermissionOverride everyone = server.getInfoChannel().getPermissionOverride(server.getGuild().getPublicRole());
        everyone.getManager().deny(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY).complete();
      }
      event.getChannel().sendMessage("The configuration has endend ! Now only registered player can see the infoChannel.").complete();
    }else {
      event.getChannel().sendMessage("Well, if you have one day changed you mind. You know where i am.").queue();
    }
  }

  private void endRegistrationTime(MessageReceivedEvent event) {
    event.getChannel().sendTyping().queue();
    event.getTextChannel().sendMessage("Well, i have some others things to do. You can always me ask again to activate this option later.").queue();
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
