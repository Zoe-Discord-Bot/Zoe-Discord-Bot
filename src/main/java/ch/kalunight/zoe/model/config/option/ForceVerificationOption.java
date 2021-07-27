package ch.kalunight.zoe.model.config.option;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import ch.kalunight.zoe.model.CommandGuildDiscordData;
import ch.kalunight.zoe.model.dto.DTO.Player;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.PlayerRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.Button;

public class ForceVerificationOption extends ConfigurationOption {

  private static final Logger logger = LoggerFactory.getLogger(ForceVerificationOption.class);
  
  private static final String CONTINUE_WITH_DELETE_ID = "continueWithDelete";
  
  private boolean optionActivated;

  public ForceVerificationOption(long guildId) {
    super(guildId, "forceVerificationOptionName", "forceVerificationOptionDescription", OptionCategory.USER_MANAGEMENT);
    optionActivated = false;
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

        if(optionActivated) {
          Button disableButton = Button.danger(DISABLE_ID, LanguageManager.getText(server.getLanguage(), "rankRoleOptionDisableTheOptionButton"));
          Button cancelButton = Button.secondary(CANCEL_ID, LanguageManager.getText(server.getLanguage(), "rankRoleOptionCancelButton"));
          
          Message message = event.getChannel().sendMessage(String.format(LanguageManager.getText(server.getLanguage(), "forceVerificationOptionDisableDescription"), 
              LanguageManager.getText(server.getLanguage(), description)))
              .setActionRow(disableButton, cancelButton).complete();
          
          waiter.waitForEvent(ButtonClickEvent.class,               
              e -> e.getUser().getId().equals(event.getUser().getId()) && e.getChannel().equals(event.getChannel()) 
              && e.getMessage().equals(message),
              e -> {
                receiveValidationAndDisable(e, server, waiter, event.getUser());
              },
              2, TimeUnit.MINUTES,
              () -> cancelOptionCreation(server, message));
        }else {
          Button validateButton = Button.success(VALIDATE_ID, LanguageManager.getText(server.getLanguage(), "continueButton"));
          Button cancelButton = Button.secondary(CANCEL_ID, LanguageManager.getText(server.getLanguage(), "rankRoleOptionCancelButton"));

          Message message = event.getChannel().sendMessage(String.format(LanguageManager.getText(server.getLanguage(), "forceVerificationOptionLongDesc"), 
              LanguageManager.getText(server.getLanguage(), description), LanguageManager.getText(server.getLanguage(), "selfAddingOptionDesc")))
              .setActionRow(validateButton, cancelButton).complete();

          waiter.waitForEvent(ButtonClickEvent.class,               
              e -> e.getUser().getId().equals(event.getUser().getId()) && e.getChannel().equals(event.getChannel()) 
              && e.getMessage().equals(message),
              e -> {
                receiveValidationAndAskForOptions(e, server, waiter, event.getUser());
              },
              2, TimeUnit.MINUTES,
              () -> cancelOptionCreation(server, message));
        }
      }
    };
  }

  private void receiveValidationAndDisable(ButtonClickEvent event, Server server, EventWaiter waiter, User user) {
    event.deferEdit().queue();
    InteractionHook hook = event.getHook();

     if(event.getButton().getId().equals(CANCEL_ID)) {
      hook.editOriginal(LanguageManager.getText(server.getLanguage(), "rankRoleOptionCancelUpdate")).setActionRows(new ArrayList<>()).queue();
      return;
    }
    
    if(event.getButton().getId().equals(DISABLE_ID)) {
      try {
        ConfigRepository.updateForceVerificationOption(server.serv_guildId, event.getJDA(), false);
      } catch (SQLException e) {
        logger.error("Unexpected database error with forceVerificationOption", e);
        hook.editOriginal(LanguageManager.getText(server.getLanguage(), "errorSQLPleaseReport")).setActionRows(new ArrayList<>()).queue();
        return;
      }
      
      hook.editOriginal(LanguageManager.getText(server.getLanguage(), "rankRoleOptionDisableUpdate")).setActionRows(new ArrayList<>()).queue();
    }
  }

  private void receiveValidationAndAskForOptions(ButtonClickEvent event, Server server,
      EventWaiter waiter, User user) {
    event.deferEdit().queue();
    InteractionHook hook = event.getHook();

    if(event.getButton().getId().equals(CANCEL_ID)) {
      hook.editOriginal(LanguageManager.getText(server.getLanguage(), "rankRoleOptionCancelCreation")).setActionRows(new ArrayList<>()).queue();
      return;
    }

    if(event.getButton().getId().equals(VALIDATE_ID)) {
      Button continueWithoutDeleteButton = Button.success(VALIDATE_ID, LanguageManager.getText(server.getLanguage(), "forceVerificationContinueWithoutDelete"));
      Button continueWithDeleteButton = Button.danger(CONTINUE_WITH_DELETE_ID, LanguageManager.getText(server.getLanguage(), "forceVerificationContinueWithDelete"));
      Button cancelButton = Button.secondary(CANCEL_ID, LanguageManager.getText(server.getLanguage(), "rankRoleOptionCancelButton"));

      hook.editOriginal(LanguageManager.getText(server.getLanguage(), "forceVerificationInitSetupMessage"))
      .setActionRow(continueWithoutDeleteButton, continueWithDeleteButton, cancelButton).queue();

      waiter.waitForEvent(ButtonClickEvent.class,               
          e -> e.getUser().getId().equals(event.getUser().getId()) && e.getChannel().equals(event.getChannel()) 
          && e.getMessage().equals(event.getMessage()),
          e -> {
            setupTheOption(e, server, waiter, event.getUser());
          },
          2, TimeUnit.MINUTES,
          () -> cancelOptionCreation(server, event.getMessage()));
    }
  }

  private void setupTheOption(ButtonClickEvent event, Server server, EventWaiter waiter, User user) {
    event.deferEdit().queue();
    InteractionHook hook = event.getHook();

    if(event.getButton().getId().equals(CANCEL_ID)) {
      hook.editOriginal(LanguageManager.getText(server.getLanguage(), "rankRoleOptionCancelCreation")).setActionRows(new ArrayList<>()).queue();
      return;
    }

    if(event.getButton().getId().equals(VALIDATE_ID)) {
      hook.editOriginal(LanguageManager.getText(server.getLanguage(), "rankRoleOptionActivateWait")).setActionRows(new ArrayList<>()).queue();
      try {
        ConfigRepository.updateForceVerificationOption(server.serv_guildId, event.getJDA(), true);
      } catch (SQLException e) {
        logger.error("Unexpected database error with forceVerificationOption", e);
        hook.editOriginal(LanguageManager.getText(server.getLanguage(), "errorSQLPleaseReport")).setActionRows(new ArrayList<>()).queue();
        return;
      }
      hook.editOriginal(LanguageManager.getText(server.getLanguage(), "forceVerificationOptionCreated")).setActionRows(new ArrayList<>()).queue();
      return;
    }

    if(event.getButton().getId().equals(CONTINUE_WITH_DELETE_ID)) {
      hook.editOriginal(LanguageManager.getText(server.getLanguage(), "rankRoleOptionActivateWait")).setActionRows(new ArrayList<>()).queue();
      try {
        List<Player> players = PlayerRepository.getPlayers(server.serv_guildId);
        for(Player player : players) {
          PlayerRepository.deletePlayer(player, server.serv_guildId);
        }
        ConfigRepository.updateForceVerificationOption(server.serv_guildId, event.getJDA(), true);
      }catch (SQLException e) {
        logger.error("Unexpected database error with forceVerificationOption", e);
        hook.editOriginal(LanguageManager.getText(server.getLanguage(), "errorSQLPleaseReport")).setActionRows(new ArrayList<>()).queue();
        return;
      }
      hook.editOriginal(LanguageManager.getText(server.getLanguage(), "forceVerificationOptionCreated")).setActionRows(new ArrayList<>()).queue();
    }
  }

  private void cancelOptionCreation(Server server, Message message) {
    message.editMessage(LanguageManager.getText(server.getLanguage(), "rankRoleOptionCancelCreation")).setActionRows(new ArrayList<>()).queue();
  }

  @Override
  public String getChoiceText(String langage) throws SQLException {
    String status;

    if(optionActivated) {
      status = LanguageManager.getText(langage, "optionEnable");
    }else {
      status = LanguageManager.getText(langage, "optionDisable");
    }
    return LanguageManager.getText(langage, description) + " : " + status;
  }

  public boolean isOptionActivated() {
    return optionActivated;
  }

  public void setOptionActivated(boolean optionActivated) {
    this.optionActivated = optionActivated;
  }

}
