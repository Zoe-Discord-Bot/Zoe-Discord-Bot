package ch.kalunight.zoe.model.config.option;

import java.awt.Color;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.model.CommandGuildDiscordData;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.service.ServerChecker;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class RankRoleOption extends ConfigurationOption {

  private static final String DB_FAIL_ERROR_MESSAGE = "deleteLeaderboardErrorDatabase";

  private static final Logger logger = LoggerFactory.getLogger(RankRoleOption.class);

  private static final String DISABLE_ID = "disable";
  private static final String CANCEL_ID = "cancel";
  private static final String VALIDATE_ID = "validate";
  private static final String TFT_ID = "tft";
  private static final String FLEX_ID = "flex";
  private static final String SOLOQ_ID = "soloq";

  private static Color ironColor = new Color(148, 148, 143);
  private static Color bronzeColor = new Color(130, 83, 63);
  private static Color silverColor = new Color(138, 163, 170);
  private static Color goldColor = new Color(207, 159, 77);
  private static Color platinumColor = new Color(32, 219, 118);
  private static Color diamondColor = new Color(104, 92, 204);
  private static Color masterColor = new Color(143, 78, 181);
  private static Color grandMasterColor = new Color(223, 58, 68);
  private static Color challengerColor = new Color(65, 222, 249);

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
    soloqEnable = false;
    flexEnable = false;
    tftEnable = false;

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

        if(!isOptionEnable()) {
          Button validateButton = Button.success(VALIDATE_ID, Emoji.fromUnicode("✅"));
          Button cancelButton = Button.danger(CANCEL_ID, Emoji.fromUnicode("❌"));

          Message message = event.getChannel().sendMessage(String.format(LanguageManager.getText(server.getLanguage(), "rankRoleOptionLongDesc"), 
              LanguageManager.getText(server.getLanguage(), description)))
              .setActionRow(validateButton, cancelButton).complete();

          waiter.waitForEvent(ButtonClickEvent.class,               
              e -> e.getUser().getId().equals(event.getUser().getId()) && e.getChannel().equals(event.getChannel()) 
              && e.getMessage().equals(message),
              e -> {
                receiveValidationAndAskForOptions(e, event.getChannel(), server, waiter, event.getUser());
              },
              2, TimeUnit.MINUTES,
              () -> cancelOptionCreation(event.getChannel(), server));

          return;
        }

        createUpdateMessageOrDelete(event.getChannel(), event.getGuild(), server, waiter, event.getUser());
      }};
  }

  private void createUpdateMessageOrDelete(TextChannel channel, Guild guild, Server server, EventWaiter waiter, User user) {
    String enable = LanguageManager.getText(server.getLanguage(), "optionEnable");

    String soloqEnableString = enable;
    String flexEnableString = enable;
    String tftEnableString = enable;

    Button buttonSoloq = Button.primary(SOLOQ_ID, Emoji.fromUnicode("1️⃣"));
    Button buttonFlex = Button.primary(FLEX_ID, Emoji.fromUnicode("2️⃣"));
    Button buttonTFT = Button.primary(TFT_ID, Emoji.fromUnicode("3️⃣"));
    Button buttonValidate = Button.success(VALIDATE_ID, LanguageManager.getText(server.getLanguage(), "rankRoleOptionActivateButton"));
    Button buttonCancel = Button.secondary(CANCEL_ID, LanguageManager.getText(server.getLanguage(), "rankRoleOptionCancelButton"));
    Button buttonDelete = Button.danger(DISABLE_ID, LanguageManager.getText(server.getLanguage(), "rankRoleOptionDisableTheOptionButton"));

    Message message = channel.sendMessage(String.format(LanguageManager.getText(server.getLanguage(), "rankRoleOptionUpdateDescription"),
        LanguageManager.getText(server.getLanguage(), description), soloqEnableString, flexEnableString, tftEnableString))
        .setActionRow(buttonSoloq, buttonFlex, buttonTFT, buttonValidate, buttonCancel, buttonDelete).complete();

    waiter.waitForEvent(ButtonClickEvent.class,               
        e -> e.getUser().getId().equals(user.getId()) && e.getChannel().equals(channel) && e.getMessage().equals(message),
        e -> {
          selectButtonQueueUpdateOrDelete(e, server, waiter);
        },
        2, TimeUnit.MINUTES,
        () -> cancelOptionUpdate(channel, server));
  }

  private void receiveValidationAndAskForOptions(ButtonClickEvent event, TextChannel channel, Server server, EventWaiter waiter, User user) {
    event.deferEdit().queue();
    if(event.getButton().getId().equals(VALIDATE_ID)) {
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

      event.getHook().editOriginal(String.format(LanguageManager.getText(server.getLanguage(), "rankRoleOptionSelectQueueWaitMessage"),
          soloqEnableString, flexEnableString, tftEnableString))
      .setActionRow(buttonSoloq, buttonFlex, buttonTFT, buttonValidate, buttonCancel).queue();

      waiter.waitForEvent(ButtonClickEvent.class,               
          e -> e.getUser().getId().equals(user.getId()) && e.getChannel().equals(channel) && e.getMessage().equals(event.getMessage()),
          e -> {
            selectButtonQueueCreation(e, server, waiter);
          },
          2, TimeUnit.MINUTES,
          () -> cancelOptionCreation(channel, server));
    }else {
      channel.sendMessage(LanguageManager.getText(server.getLanguage(), "rankRoleOptionCancelCreation")).queue();
    }
  }

  private void selectButtonQueueUpdateOrDelete(ButtonClickEvent event, Server server, EventWaiter waiter) {
    event.getHook().getInteraction().deferReply().queue();
    String buttonIdSelected = event.getButton().getId();
    InteractionHook hook = event.getHook();

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
      updateOption(event.getGuild(), server, hook);
      return;
    }

    if(buttonIdSelected.equals(CANCEL_ID)) {
      hook.editOriginal(LanguageManager.getText(server.getLanguage(), "rankRoleOptionCancelUpdate")).setActionRows(new ArrayList<>()).queue();
      return;
    }

    if(buttonIdSelected.equals(DISABLE_ID)) {
      hook.editOriginal(LanguageManager.getText(server.getLanguage(), "rankRoleOptionDisableUpdateWait")).setActionRows(new ArrayList<>()).queue();
      deleteOption(event.getGuild(), server, hook);
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

    hook.editOriginal(String.format(LanguageManager.getText(server.getLanguage(), "rankRoleOptionUpdateDescription"),
        soloqEnableString, flexEnableString, tftEnableString)).queue();

    waiter.waitForEvent(ButtonClickEvent.class,               
        e -> e.getUser().getId().equals(event.getUser().getId()) && e.getChannel().equals(event.getChannel()) && e.getMessage().equals(event.getMessage()),
        e -> {
          selectButtonQueueUpdateOrDelete(e, server, waiter);
        },
        2, TimeUnit.MINUTES,
        () -> cancelOptionUpdate(event.getTextChannel(), server));
  }

  private void updateOption(Guild guild, Server server, InteractionHook hook) {
    if(!isOptionEnable()) {
      hook.editOriginal(LanguageManager.getText(server.getLanguage(), "rankRoleOptionNeedAtLeastOneQueue")).setActionRow(new ArrayList<>()).queue();
    }

    try {
      ConfigRepository.updateRankRoleOption(guild.getIdLong(), guild.getJDA(), soloqEnable, flexEnable, tftEnable);
    } catch (SQLException e) {
      logger.error("Database error with update RankRoleOption", e);
      hook.editOriginal(LanguageManager.getText(server.getLanguage(), DB_FAIL_ERROR_MESSAGE)).setActionRow(new ArrayList<>()).queue();
      return;
    }

    ServerChecker.getServerRefreshService().getServersStatusDetected().add(server);
    hook.editOriginal(LanguageManager.getText(server.getLanguage(), "rankRoleOptionUpdateSave")).setActionRow(new ArrayList<>()).queue();
  }

  private void deleteOption(Guild guild, Server server, InteractionHook hook) {

    try {
      ConfigRepository.updateRankRoleOption(guild.getIdLong(), guild.getJDA(), false, false, false);
    } catch (SQLException e) {
      logger.error("Database error with update RankRoleOption", e);
      hook.editOriginal(LanguageManager.getText(server.getLanguage(), DB_FAIL_ERROR_MESSAGE)).setActionRow(new ArrayList<>()).queue();
      return;
    }
    
    try {
      if(iron != null) {
        iron.delete().complete();
      }

      if(bronze != null) {
        bronze.delete().complete();
      }

      if(silver != null) {
        silver.delete().complete();
      }

      if(gold != null) {
        gold.delete().complete();
      }

      if(platinum != null) {
        platinum.delete().complete();
      }

      if(diamond != null) {
        diamond.delete().complete();
      }

      if(master != null) {
        master.delete().complete();
      }

      if(grandMaster != null) {
        grandMaster.delete().complete();
      }

      if(challenger != null) {
        challenger.delete().complete();
      }
    }catch (ErrorResponseException e) {
      if(e.getErrorResponse() != ErrorResponse.MISSING_PERMISSIONS) {
        logger.error("Unexpected exception when deleting role", e);
      }
      
      hook.editOriginal(LanguageManager.getText(server.getLanguage(), "rankRoleOptionDisableUpdateFailedToDelete")).queue();
      return;
    }

    hook.editOriginal(LanguageManager.getText(server.getLanguage(), "rankRoleOptionDisableUpdate")).queue();
  }

  private void selectButtonQueueCreation(ButtonClickEvent event, Server server, EventWaiter waiter) {
    event.getHook().getInteraction().deferReply().queue();
    String buttonIdSelected = event.getButton().getId();
    InteractionHook hook = event.getHook();

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
      hook.editOriginal(LanguageManager.getText(server.getLanguage(), "rankRoleOptionActivateWait")).setActionRows(new ArrayList<>()).queue();
      createOption(event.getGuild(), server, hook);
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

    waiter.waitForEvent(ButtonClickEvent.class,               
        e -> e.getUser().getId().equals(event.getUser().getId()) && e.getChannel().equals(event.getChannel()) && e.getMessage().equals(event.getMessage()),
        e -> {
          selectButtonQueueCreation(e, server, waiter);
        },
        2, TimeUnit.MINUTES,
        () -> cancelOptionCreation(event.getTextChannel(), server));
  }

  private void createOption(Guild guild, Server server, InteractionHook hook) {

    if(!isOptionEnable()) {
      hook.editOriginal(LanguageManager.getText(server.getLanguage(), "rankRoleOptionNeedAtLeastOneQueue")).setActionRow(new ArrayList<>()).queue();
    }

    try {
      iron = guild.createRole().setName(LanguageManager.getText(server.getLanguage(), "iron")).setColor(ironColor).complete();
      bronze = guild.createRole().setName(LanguageManager.getText(server.getLanguage(), "bronze")).setColor(bronzeColor).complete();
      silver = guild.createRole().setName(LanguageManager.getText(server.getLanguage(), "silver")).setColor(silverColor).complete();
      gold = guild.createRole().setName(LanguageManager.getText(server.getLanguage(), "gold")).setColor(goldColor).complete();
      platinum = guild.createRole().setName(LanguageManager.getText(server.getLanguage(), "platinum")).setColor(platinumColor).complete();
      diamond = guild.createRole().setName(LanguageManager.getText(server.getLanguage(), "diamond")).setColor(diamondColor).complete();
      master = guild.createRole().setName(LanguageManager.getText(server.getLanguage(), "master")).setColor(masterColor).complete();
      grandMaster = guild.createRole().setName(LanguageManager.getText(server.getLanguage(), "grandMaster")).setColor(grandMasterColor).complete();
      challenger = guild.createRole().setName(LanguageManager.getText(server.getLanguage(), "challenger")).setColor(challengerColor).complete();
    }catch(InsufficientPermissionException e) {
      hook.editOriginal(String.format(LanguageManager.getText(server.getLanguage(), "rankRoleOptionPermissionMissingError"), e.getPermission().getName())).queue();
      return;
    }catch (ErrorResponseException e) {
      if(e.getErrorResponse() == ErrorResponse.MAX_ROLES_PER_GUILD) {
        hook.editOriginal(LanguageManager.getText(server.getLanguage(), "rankRoleOptionMaxRoleGuild")).queue();
      }
      return;
    }

    try {
      ConfigRepository.updateRankRoleOption(server.serv_guildId, guild.getJDA(), iron.getIdLong(), bronze.getIdLong(), silver.getIdLong(), gold.getIdLong(), platinum.getIdLong(),
          diamond.getIdLong(), master.getIdLong(), grandMaster.getIdLong(), challenger.getIdLong());
      ConfigRepository.updateRankRoleOption(server.serv_guildId, guild.getJDA(), soloqEnable, flexEnable, tftEnable);
    } catch (SQLException e) {
      logger.error("Database error with update RankRoleOption", e);
      hook.editOriginal(LanguageManager.getText(server.getLanguage(), DB_FAIL_ERROR_MESSAGE)).queue();
      return;
    }

    ServerChecker.getServerRefreshService().getServersStatusDetected().add(server);
    hook.editOriginal(LanguageManager.getText(server.getLanguage(), "rankRoleOptionOptionCreated")).queue();
  }

  private void cancelOptionUpdate(TextChannel channel, Server server) {
    channel.sendMessage(LanguageManager.getText(server.getLanguage(), "rankRoleOptionCancelUpdate")).queue();
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

  public void setValues(DTO.RankRoleOption optionData, Guild guild) {
    soloqEnable = optionData.rankRoleOption_soloqEnable;
    flexEnable = optionData.rankRoleOption_flexEnable;
    tftEnable = optionData.rankRoleOption_tftEnable;

    if(optionData.rankRoleOption_ironId != null && optionData.rankRoleOption_ironId != 0){
      iron = guild.getRoleById(optionData.rankRoleOption_ironId);
    }

    if(optionData.rankRoleOption_bronzeId != null && optionData.rankRoleOption_bronzeId != 0){
      bronze = guild.getRoleById(optionData.rankRoleOption_bronzeId);
    }

    if(optionData.rankRoleOption_silverId != null && optionData.rankRoleOption_silverId != 0){
      silver = guild.getRoleById(optionData.rankRoleOption_silverId);
    }

    if(optionData.rankRoleOption_goldId != null && optionData.rankRoleOption_goldId != 0){
      gold = guild.getRoleById(optionData.rankRoleOption_goldId);
    }

    if(optionData.rankRoleOption_platinumId != null && optionData.rankRoleOption_platinumId != 0){
      platinum = guild.getRoleById(optionData.rankRoleOption_platinumId);
    }

    if(optionData.rankRoleOption_diamondId != null && optionData.rankRoleOption_diamondId != 0){
      diamond = guild.getRoleById(optionData.rankRoleOption_diamondId);
    }

    if(optionData.rankRoleOption_masterId != null && optionData.rankRoleOption_masterId != 0){
      master = guild.getRoleById(optionData.rankRoleOption_masterId);
    }

    if(optionData.rankRoleOption_grandMasterId != null && optionData.rankRoleOption_grandMasterId != 0){
      grandMaster = guild.getRoleById(optionData.rankRoleOption_grandMasterId);
    }

    if(optionData.rankRoleOption_challengerId != null && optionData.rankRoleOption_challengerId != 0){
      challenger = guild.getRoleById(optionData.rankRoleOption_challengerId);
    }
  }

  public boolean isOptionEnable() {
    return soloqEnable || flexEnable || tftEnable;
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