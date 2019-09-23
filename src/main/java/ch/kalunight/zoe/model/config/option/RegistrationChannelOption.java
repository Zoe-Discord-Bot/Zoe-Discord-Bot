package ch.kalunight.zoe.model.config.option;

import java.awt.Color;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.ButtonMenu;
import ch.kalunight.zoe.Zoe;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.MessageReaction.ReactionEmote;

public class RegistrationChannelOption extends ConfigurationOption {

  public enum CLEAN_CHANNEL_OPTION {
    DISABLE("Disable", "Option disable"),
    ONLY_ZOE_COMMANDS("Delete only Zoe commands", "This will delete all new zoe commands in the channel"),
    ALL("Delete all messages", "This will delete all new messages sended in the channel.");

    private final String name;
    private final String description;

    private CLEAN_CHANNEL_OPTION(String name, String description) {
      this.name = name;
      this.description = description;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }
  }

  private CLEAN_CHANNEL_OPTION cleanChannelOption;

  private TextChannel cleanChannel;

  public RegistrationChannelOption() {
    super("clean_channel", "Create a clean text channel where message/commands are automatically deleted.");   
    cleanChannelOption = CLEAN_CHANNEL_OPTION.DISABLE;
    cleanChannel = null;
  }

  @Override
  public Consumer<CommandEvent> getChangeConsumer(EventWaiter waiter) {
    return new Consumer<CommandEvent>() {

      @Override
      public void accept(CommandEvent event) {

        if(!event.getGuild().getSelfMember().getPermissions().contains(Permission.MESSAGE_MANAGE)) {
          event.reply("I need the message manage permission to activate this option.");
          return;
        }

        ButtonMenu.Builder choiceBuilder = new ButtonMenu.Builder();

        choiceBuilder.setEventWaiter(waiter);
        choiceBuilder.addChoices(":one:",":two:",":three:");
        choiceBuilder.addUsers(event.getAuthor());
        choiceBuilder.setFinalAction(finalAction());
        choiceBuilder.setColor(Color.BLUE);

        choiceBuilder.setTimeout(2, TimeUnit.MINUTES);

        choiceBuilder.setText("Option : **" + description + "**\n\n"
            + "This option will create a \"clean channel\" where messages are deleted after been sended. " 
            + "People with the permission Manage channel.\n"
            + "**The text channel will be selected after the option selection**.\n\n"
            + ":one: -> " + CLEAN_CHANNEL_OPTION.DISABLE.name + " : " + CLEAN_CHANNEL_OPTION.DISABLE.description + "\n"
            + ":two: -> " + CLEAN_CHANNEL_OPTION.ONLY_ZOE_COMMANDS.name + " : " + CLEAN_CHANNEL_OPTION.ONLY_ZOE_COMMANDS.description + "\n"
            + ":three: -> " + CLEAN_CHANNEL_OPTION.ALL.name + " : " + CLEAN_CHANNEL_OPTION.ALL.description + "\n");

        choiceBuilder.setAction(receiveValidationAndCreateOption(event.getChannel(), event.getGuild()));

        ButtonMenu menu = choiceBuilder.build();

        menu.display(event.getChannel());

      }};
  }
  
  private Consumer<ReactionEmote> receiveValidationAndCreateOption(MessageChannel channel, Guild guild) {
    return new Consumer<ReactionEmote>() {

      @Override
      public void accept(ReactionEmote emoteUsed) {
        channel.sendTyping().complete();
        if(emoteUsed.getName().equals(":two:") || emoteUsed.getName().equals(":three:")) {
          
          
          
        }else {
          
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
    String status = "";

    if(cleanChannelOption == CLEAN_CHANNEL_OPTION.DISABLE) {
      status = "Disable";
    } else {
      status = "Enable (" + cleanChannelOption.name + ")";
    }

    return description + " : " + status;
  }

  @Override
  public String getSave() {
    String save = id + ":" + cleanChannelOption.toString();
    if(cleanChannel != null) {
      save += ":" + cleanChannel.getId();
    }

    return save;
  }

  @Override
  public void restoreSave(String save) {
    String[] saveDatas = save.split(":");

    cleanChannelOption = CLEAN_CHANNEL_OPTION.valueOf(saveDatas[1]);

    if(saveDatas.length > 2) {
      cleanChannel = Zoe.getJda().getTextChannelById(saveDatas[2]);
    }
  }

}
