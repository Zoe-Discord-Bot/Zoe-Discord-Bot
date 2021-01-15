package ch.kalunight.zoe.model.config.option;

import java.awt.Color;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.SelectionDialog;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.RepoRessources;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Message;
import net.rithms.riot.constant.Platform;

public class RegionOption extends ConfigurationOption {

  private Platform region;

  public RegionOption(long guildId) {
    super(guildId, "regionOptionDesc");
    this.region = null;
  }

  @Override
  public String getChoiceText(String langage) {
    String strRegion = LanguageManager.getText(langage, "optionRegionDisable");

    if(region != null) {
      strRegion = this.region.getName().toUpperCase() + " (" + LanguageManager.getText(langage, "optionEnable") + ")";
    }

    return LanguageManager.getText(langage, description) + " : " + strRegion;
  }

  @Override
  public Consumer<CommandEvent> getChangeConsumer(EventWaiter waiter, DTO.Server server) {
    return new Consumer<CommandEvent>() {

      @Override
      public void accept(CommandEvent event) {
        event.getChannel().sendTyping().complete();
        
        String message;
        if(region == null) {
          message = LanguageManager.getText(server.getLanguage(), "regionOptionAnyRegionSelected");
        }else {
          message = String.format(LanguageManager.getText(server.getLanguage(), "regionOptionRegionSelected"),
              region.getName().toUpperCase());
        }

        event.getChannel().sendMessage(message).queue();

        SelectionDialog.Builder selectAccountBuilder = new SelectionDialog.Builder()
            .addUsers(event.getAuthor())
            .setEventWaiter(waiter)
            .useLooping(true)
            .setColor(Color.BLUE)
            .setSelectedEnds("**", "**")
            .setCanceled(getSelectionCancelAction(server.getLanguage()))
            .setTimeout(2, TimeUnit.MINUTES);

        List<Platform> regionsList = new ArrayList<>();
        List<String> regionChoices = new ArrayList<>();
        for(Platform regionMember : Platform.values()) {
          String actualChoice = String.format(LanguageManager.getText(server.getLanguage(), "regionOptionRegionChoice"),
              regionMember.getName().toUpperCase());
          
          regionChoices.add(actualChoice);
          selectAccountBuilder.addChoices(actualChoice);
          regionsList.add(regionMember);
        }
        
        String anyChoice = LanguageManager.getText(server.getLanguage(), "regionOptionDisableChoice");
        regionChoices.add(anyChoice);
        selectAccountBuilder.addChoices(anyChoice);

        selectAccountBuilder.setText(getUpdateMessageAfterChangeSelectAction(server.getLanguage(), regionChoices));
        selectAccountBuilder.setSelectionConsumer(getSelectionDoneAction(server.getLanguage(), regionsList, server));

        SelectionDialog dialog = selectAccountBuilder.build();
        dialog.display(event.getChannel());
      }};
  }

  private Function<Integer, String> getUpdateMessageAfterChangeSelectAction(String language, List<String> choices) {
    return new Function<Integer, String>() {
      @Override
      public String apply(Integer index) {
        if(choices.size() == index) {
          return LanguageManager.getText(language, "regionOptionInSelectionAny");
        }

        return String.format(LanguageManager.getText(language, "regionOptionInSelectionRegion"), choices.get(index - 1));
      }
    };
  }

  private BiConsumer<Message, Integer> getSelectionDoneAction(String language, List<Platform> regionsList, DTO.Server server) {
    return new BiConsumer<Message, Integer>() {
      @Override
      public void accept(Message selectionMessage, Integer selectionOfRegion) {

        selectionMessage.clearReactions().queue();

        String strRegion;
        if(regionsList.size() == selectionOfRegion - 1) {
          strRegion = LanguageManager.getText(language, "regionOptionAnyRegion");
          region = null;
        }else {
          strRegion = regionsList.get(selectionOfRegion - 1).getName().toUpperCase();
          region = regionsList.get(selectionOfRegion - 1);
        }
        
        try {
          ConfigRepository.updateRegionOption(guildId, region);
        } catch (SQLException e) {
          RepoRessources.sqlErrorReport(selectionMessage.getChannel(), server, e);
          return;
        }

        selectionMessage.getTextChannel().sendMessage(String.format(LanguageManager.getText(language, "regionOptionDoneMessage"),
            strRegion)).queue();
      }
    };
  }

  private Consumer<Message> getSelectionCancelAction(String language){
    return new Consumer<Message>() {
      @Override
      public void accept(Message message) {
        message.clearReactions().queue();
        message.editMessage(LanguageManager.getText(language, "regionOptionSelectionCanceledMessage")).queue();
      }
    };
  }

  public Platform getRegion() {
    return region;
  }
  
  public void setRegion(Platform region) {
    this.region = region;
  }
}
