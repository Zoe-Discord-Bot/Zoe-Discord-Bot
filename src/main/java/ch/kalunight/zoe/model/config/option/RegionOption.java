package ch.kalunight.zoe.model.config.option;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.SelectionDialog;
import ch.kalunight.zoe.ServerData;
import ch.kalunight.zoe.model.Server;
import ch.kalunight.zoe.model.static_data.SpellingLanguage;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Message;
import net.rithms.riot.constant.Platform;

public class RegionOption extends ConfigurationOption {

  private Platform region;

  public RegionOption() {
    super("default_region", "regionOptionDesc");
    this.region = null;
  }

  @Override
  public String getChoiceText(SpellingLanguage langage) {
    String strRegion = LanguageManager.getText(langage, "optionRegionDisable");

    if(region != null) {
      strRegion = this.region.getName().toUpperCase() + " (" + LanguageManager.getText(langage, "optionEnable") + ")";
    }

    return LanguageManager.getText(langage, description) + " : " + strRegion;
  }

  @Override
  public Consumer<CommandEvent> getChangeConsumer(EventWaiter waiter) {
    return new Consumer<CommandEvent>() {

      @Override
      public void accept(CommandEvent event) {
        event.getChannel().sendTyping().complete();
        Server server = ServerData.getServers().get(event.getGuild().getId());
        
        String message;
        if(region == null) {
          message = LanguageManager.getText(server.getLangage(), "regionOptionAnyRegionSelected");
        }else {
          message = String.format(LanguageManager.getText(server.getLangage(), "regionOptionRegionSelected"),
              region.getName().toUpperCase());
        }

        event.getChannel().sendMessage(message).queue();

        SelectionDialog.Builder selectAccountBuilder = new SelectionDialog.Builder()
            .addUsers(event.getAuthor())
            .setEventWaiter(waiter)
            .useLooping(true)
            .setColor(Color.BLUE)
            .setSelectedEnds("**", "**")
            .setCanceled(getSelectionCancelAction(server.getLangage()))
            .setTimeout(2, TimeUnit.MINUTES);

        List<Platform> regionsList = new ArrayList<>();
        List<String> regionChoices = new ArrayList<>();
        for(Platform regionMember : Platform.values()) {
          String actualChoice = String.format(LanguageManager.getText(server.getLangage(), "regionOptionRegionChoice"),
              regionMember.getName().toUpperCase());
          
          regionChoices.add(actualChoice);
          selectAccountBuilder.addChoices(actualChoice);
          regionsList.add(regionMember);
        }
        
        String anyChoice = LanguageManager.getText(server.getLangage(), "regionOptionDisableChoice");
        regionChoices.add(anyChoice);
        selectAccountBuilder.addChoices(anyChoice);

        selectAccountBuilder.setText(getUpdateMessageAfterChangeSelectAction(server.getLangage(), regionChoices));
        selectAccountBuilder.setSelectionConsumer(getSelectionDoneAction(server.getLangage(), regionsList));

        SelectionDialog dialog = selectAccountBuilder.build();
        dialog.display(event.getChannel());
      }};
  }

  private Function<Integer, String> getUpdateMessageAfterChangeSelectAction(SpellingLanguage language, List<String> choices) {
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

  private BiConsumer<Message, Integer> getSelectionDoneAction(SpellingLanguage language, List<Platform> regionsList) {
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

        selectionMessage.getTextChannel().sendMessage(String.format(LanguageManager.getText(language, "regionOptionDoneMessage"),
            strRegion)).queue();
      }
    };
  }

  private Consumer<Message> getSelectionCancelAction(SpellingLanguage language){
    return new Consumer<Message>() {
      @Override
      public void accept(Message message) {
        message.clearReactions().queue();
        message.editMessage(LanguageManager.getText(language, "regionOptionSelectionCanceledMessage")).queue();
      }
    };
  }

  @Override
  public String getSave() {
    String regionStr = NO_VALUE_REPRESENTATION;
    if(region != null) {
      regionStr = region.getId();
    }

    return id + ":" + regionStr;
  }

  @Override
  public void restoreSave(String save) {
    String[] saveDatas = save.split(":");

    if(!saveDatas[1].equals(NO_VALUE_REPRESENTATION)) {
      region = Platform.getPlatformById(saveDatas[1]);
    }
  }

  public Platform getRegion() {
    return region;
  }
}
