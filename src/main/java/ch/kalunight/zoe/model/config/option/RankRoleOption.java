package ch.kalunight.zoe.model.config.option;

import java.awt.Color;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.ButtonMenu;

import ch.kalunight.zoe.model.CommandGuildDiscordData;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class RankRoleOption extends ConfigurationOption {

  private static final String CANCEL_ID = "cancel";
  private static final String VALIDATE_ID = "validate";
  private static final String TFT_ID = "tft";
  private static final String FLEX_ID = "flex";
  private static final String SOLOQ_ID = "soloq";
  private boolean soloqEnable;
  private boolean flexEnable;
  private boolean tftEnable;

  private Role iron;
  private Role bronze;
  private Role silver;
  private Role gold;
  private Role platinum;
  private Role diamond;
  private Role master;
  private Role grandMaster;
  private Role challenger;

  public RankRoleOption(long guildId) {
    super(guildId, "rankRoleOptionDescription");
    iron = null;
    bronze = null;
    silver = null;
    gold = null;
    platinum = null;
    diamond = null;
    master = null;
    grandMaster = null;
    challenger = null;
  }

  @Override
  public Consumer<CommandGuildDiscordData> getChangeConsumer(EventWaiter waiter, Server server) {
    return new Consumer<CommandGuildDiscordData>() {

      @Override
      public void accept(CommandGuildDiscordData event) {

        if(!event.getGuild().getSelfMember().getPermissions().contains(Permission.MANAGE_ROLES)) {
          event.getChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "roleOptionPermissionNeeded")).queue();
          return;
        }

        if(isOptionEnable()) {
          ButtonMenu.Builder choiceBuilder = new ButtonMenu.Builder();

          choiceBuilder.setEventWaiter(waiter);
          choiceBuilder.addChoices("✅","❌");
          choiceBuilder.addUsers(event.getUser());
          choiceBuilder.setFinalAction(finalAction());
          choiceBuilder.setColor(Color.BLUE);

          choiceBuilder.setTimeout(2, TimeUnit.MINUTES);

          choiceBuilder.setText(String.format(LanguageManager.getText(server.getLanguage(), "rankRoleOptionLongDesc"), 
              LanguageManager.getText(server.getLanguage(), description)));

          choiceBuilder.setAction(receiveValidationAndAskForOptions(event.getChannel(), server, waiter, event.getUser()));

          ButtonMenu menu = choiceBuilder.build();

          menu.display(event.getChannel());
          return;
        }
        
        createUpdateMessageOrDelete(event.getChannel(), event.getGuild(), server);
      }};
  }

  private Consumer<ReactionEmote> createUpdateMessageOrDelete(TextChannel channel, Guild guild, Server server) {
    // TODO Auto-generated method stub
    return null;
  }

  private Consumer<ReactionEmote> receiveValidationAndAskForOptions(TextChannel channel, Server server, EventWaiter waiter, User user) {
    return new Consumer<ReactionEmote>() {

      @Override
      public void accept(ReactionEmote emoteUsed) {
        channel.sendTyping().complete();
        if(emoteUsed.getName().equals("✅")) {
          soloqEnable = true;
          flexEnable = true;
          tftEnable = true;

          String enable = LanguageManager.getText(server.getLanguage(), "optionEnable");

          String soloqEnableString = enable;
          String flexEnableString = enable;
          String tftEnableString = enable;

          Button buttonSoloq = Button.primary(SOLOQ_ID, Emoji.fromUnicode("1️⃣"));
          Button buttonFlex = Button.primary(FLEX_ID, Emoji.fromUnicode("2️⃣"));
          Button buttonTFT = Button.primary(TFT_ID, Emoji.fromUnicode("3️⃣"));
          Button buttonValidate = Button.success(VALIDATE_ID, LanguageManager.getText(server.getLanguage(), "rankRoleOptionActivateButton"));
          Button buttonCancel = Button.danger(CANCEL_ID, LanguageManager.getText(server.getLanguage(), "rankRoleOptionCancelButton"));

          Message message = channel.sendMessage(String.format(LanguageManager.getText(server.getLanguage(), "rankRoleOptionSelectQueueWaitMessage"),
              soloqEnableString, flexEnableString, tftEnableString))
              .setActionRow(buttonSoloq, buttonFlex, buttonTFT, buttonValidate, buttonCancel).complete();

          waiter.waitForEvent(ButtonClickEvent.class,               
              e -> e.getUser().getId().equals(user.getId()) && e.getChannel().equals(channel) && e.getMessage().equals(message),
              e -> {
                selectButtonQueue(e, server, waiter);
              },
              2, TimeUnit.MINUTES,
              () -> cancelOptionCreation(channel, server));
        }else {
          channel.sendMessage(LanguageManager.getText(server.getLanguage(), "rankRoleOptionCancelCreation")).queue();
        }
      }};
  }

  private void selectButtonQueue(ButtonClickEvent e, Server server, EventWaiter waiter) {
    String buttonIdSelected = e.getButton().getId();
    InteractionHook hook = e.getHook();

    if(buttonIdSelected.equals(SOLOQ_ID)) {
      soloqEnable = !soloqEnable;
    }

    if(buttonIdSelected.equals(FLEX_ID)) {
      flexEnable = !flexEnable;
    }

    if(buttonIdSelected.equals(TFT_ID)) {
      tftEnable = !tftEnable;
    }

    if(buttonIdSelected.equals(VALIDATE_ID)) {
      hook.editOriginal(LanguageManager.getText(server.getLanguage(), "rankRoleOptionActivatWait")).setActionRows(new ArrayList<>()).queue();
      //TODO: update db and roles
      return;
    }

    if(buttonIdSelected.equals(CANCEL_ID)) {
      hook.editOriginal(LanguageManager.getText(server.getLanguage(), "rankRoleOptionCancelCreation")).setActionRows(new ArrayList<>()).queue();
      return;
    }

    String enable = LanguageManager.getText(server.getLanguage(), "optionEnable");
    String disable = LanguageManager.getText(server.getLanguage(), "optionDisable");

    String soloqEnableString;
    String flexEnableString;
    String tftEnableString;

    if(soloqEnable) {
      soloqEnableString = enable;
    }else {
      soloqEnableString = disable;
    }

    if(flexEnable) {
      flexEnableString = enable;
    }else {
      flexEnableString = disable;
    }

    if(tftEnable) {
      tftEnableString = enable;
    }else {
      tftEnableString = disable;
    }

    hook.editOriginal(String.format(LanguageManager.getText(server.getLanguage(), "rankRoleOptionSelectQueueWaitMessage"),
        soloqEnableString, flexEnableString, tftEnableString)).queue();
  }

  private void cancelOptionCreation(TextChannel channel, Server server) {
    channel.sendMessage(LanguageManager.getText(server.getLanguage(), "rankRoleOptionCancelCreation")).queue();
  }

  @Override
  public String getChoiceText(String langage) throws SQLException {
    if(isOptionEnable()) {
      return String.format(LanguageManager.getText(langage, "rankRoleOptionDescriptionChangeText"), LanguageManager.getText(langage, "optionEnable"));
    }else {
      return String.format(LanguageManager.getText(langage, "rankRoleOptionDescriptionChangeText"), LanguageManager.getText(langage, "optionDisable"));
    }
  }

  public boolean isOptionEnable() {
    return soloqEnable || flexEnable || tftEnable;
  }

  private Consumer<Message> finalAction(){
    return new Consumer<Message>() {

      @Override
      public void accept(Message message) {
        message.clearReactions().queue();
      }};
  }

  public boolean isSoloqEnable() {
    return soloqEnable;
  }

  public void setSoloqEnable(boolean soloqEnable) {
    this.soloqEnable = soloqEnable;
  }

  public boolean isFlexEnable() {
    return flexEnable;
  }

  public void setFlexEnable(boolean flexEnable) {
    this.flexEnable = flexEnable;
  }

  public boolean isTftEnable() {
    return tftEnable;
  }

  public void setTftEnable(boolean tftEnable) {
    this.tftEnable = tftEnable;
  }

  public Role getIron() {
    return iron;
  }

  public void setIron(Role iron) {
    this.iron = iron;
  }

  public Role getBronze() {
    return bronze;
  }

  public void setBronze(Role bronze) {
    this.bronze = bronze;
  }

  public Role getSilver() {
    return silver;
  }

  public void setSilver(Role silver) {
    this.silver = silver;
  }

  public Role getGold() {
    return gold;
  }

  public void setGold(Role gold) {
    this.gold = gold;
  }

  public Role getPlatinum() {
    return platinum;
  }

  public void setPlatinum(Role platinum) {
    this.platinum = platinum;
  }

  public Role getDiamond() {
    return diamond;
  }

  public void setDiamond(Role diamond) {
    this.diamond = diamond;
  }

  public Role getMaster() {
    return master;
  }

  public void setMaster(Role master) {
    this.master = master;
  }

  public Role getGrandMaster() {
    return grandMaster;
  }

  public void setGrandMaster(Role grandMaster) {
    this.grandMaster = grandMaster;
  }

  public Role getChallenger() {
    return challenger;
  }

  public void setChallenger(Role challenger) {
    this.challenger = challenger;
  }

}
