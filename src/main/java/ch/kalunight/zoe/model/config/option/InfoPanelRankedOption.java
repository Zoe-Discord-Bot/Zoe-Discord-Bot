package ch.kalunight.zoe.model.config.option;

import java.awt.Color;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.ButtonMenu;

import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.RepoRessources;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;

public class InfoPanelRankedOption extends ConfigurationOption {

  private static final String INFOPANEL_RANKED_DESC_ID = "infopanelRankedOptionDesc";

  private boolean optionActivated;

  public InfoPanelRankedOption(long guildId) {
    super(guildId, INFOPANEL_RANKED_DESC_ID);
    this.optionActivated = true;
  }

  @Override
  public Consumer<CommandEvent> getChangeConsumer(EventWaiter waiter, Server server) {
    return new Consumer<CommandEvent>() {

      @Override
      public void accept(CommandEvent event) {

        ButtonMenu.Builder choiceBuilder = new ButtonMenu.Builder();

        choiceBuilder.setEventWaiter(waiter);
        choiceBuilder.addChoices("✅","❌");
        choiceBuilder.addUsers(event.getAuthor());
        choiceBuilder.setFinalAction(finalAction());
        choiceBuilder.setColor(Color.BLUE);

        choiceBuilder.setTimeout(2, TimeUnit.MINUTES);

        if(!optionActivated) {

          choiceBuilder.setText(String.format(LanguageManager.getText(server.serv_language,
              "infoPanelRankedOptionLongDescEnable"), LanguageManager.getText(server.serv_language, INFOPANEL_RANKED_DESC_ID)));

          choiceBuilder.setAction(activateTheOption(event.getChannel(), server));

          ButtonMenu menu = choiceBuilder.build();

          menu.display(event.getChannel());

        }else {

          choiceBuilder.setText(String.format(LanguageManager.getText(server.serv_language, "infoPanelRankedOptionLongDescDisable"),
              LanguageManager.getText(server.serv_language, INFOPANEL_RANKED_DESC_ID)));

          choiceBuilder.setAction(disableTheOption(event.getChannel(), server));

          ButtonMenu menu = choiceBuilder.build();

          menu.display(event.getChannel());
        }
      }

    };
  }

  private Consumer<ReactionEmote> disableTheOption(MessageChannel messageChannel, DTO.Server server) {
    return new Consumer<ReactionEmote>() {

      @Override
      public void accept(ReactionEmote emote) {
        messageChannel.sendTyping().complete();

        if(emote.getName().equals("✅")) {
          try {
            ConfigRepository.updateInfoPanelRanked(guildId, false);
            optionActivated = false;
          } catch (SQLException e) {
            RepoRessources.sqlErrorReport(messageChannel, server, e);
            return;
          }
          messageChannel.sendMessage(LanguageManager.getText(server.serv_language, "cleanChannelOptionBeenDisable")).queue(); //Same text
        }else {
          messageChannel.sendMessage(LanguageManager.getText(server.serv_language, "cleanChannelOptionStillEnable")).queue(); //Same text
        }
      }};
  }

  private Consumer<ReactionEmote> activateTheOption(MessageChannel messageChannel, DTO.Server server) {
    return new Consumer<ReactionEmote>() {

      @Override
      public void accept(ReactionEmote emoteUsed) {
        messageChannel.sendTyping().complete();

        if(emoteUsed.getName().equals("✅")) {
          try {
            ConfigRepository.updateInfoPanelRanked(guildId, true);
            optionActivated = true;
          } catch (SQLException e) {
            RepoRessources.sqlErrorReport(messageChannel, server, e);
            return;
          }
          messageChannel.sendMessage(LanguageManager.getText(server.serv_language, "cleanChannelOptionBeenActivated")).queue(); //Same text
        }else {
          messageChannel.sendMessage(LanguageManager.getText(server.serv_language, "cleanChannelOptionStillDisable")).queue(); //Same text
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
  public String getChoiceText(String langage) throws SQLException {
    String status;

    if(optionActivated) {
      status = LanguageManager.getText(langage, "optionEnable");
    } else {
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
