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
import ch.kalunight.zoe.model.static_data.SpellingLangage;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Message;
import net.rithms.riot.constant.Platform;

public class RegionOption extends ConfigurationOption {

  private Platform region;

  public RegionOption() {
    super("default_region", "Default region of the server");
    this.region = null;
  }

  @Override
  public String getChoiceText(SpellingLangage langage) {
    String strRegion = LanguageManager.getText(langage, "optionRegionDisable");

    if(region != null) {
      strRegion = this.region.getName().toUpperCase() + " (" + LanguageManager.getText(langage, "optionEnable") + ")";
    }

    return description + " : " + strRegion;
  }

  @Override
  public Consumer<CommandEvent> getChangeConsumer(EventWaiter waiter) {
    return new Consumer<CommandEvent>() {

      @Override
      public void accept(CommandEvent event) {
        event.getChannel().sendTyping().complete();
        String message;
        if(region == null) {
          message = "Currently **any** default region is setted. Which region you want to set ?";
        }else {
          message = "Currently default region is **" + region.getName().toUpperCase() + "**. Which region you want to set ?";
        }

        event.getChannel().sendMessage(message).queue();

        SelectionDialog.Builder selectAccountBuilder = new SelectionDialog.Builder()
            .addUsers(event.getAuthor())
            .setEventWaiter(waiter)
            .useLooping(true)
            .setColor(Color.BLUE)
            .setSelectedEnds("**", "**")
            .setCanceled(getSelectionCancelAction())
            .setTimeout(2, TimeUnit.MINUTES);

        List<Platform> regionsList = new ArrayList<>();
        List<String> regionChoices = new ArrayList<>();
        for(Platform regionMember : Platform.values()) {
          String actualChoice = "Region " + regionMember.getName().toUpperCase();
          regionChoices.add(actualChoice);
          selectAccountBuilder.addChoices(actualChoice);
          regionsList.add(regionMember);
        }
        regionChoices.add("Region Any (Option Disable)");
        selectAccountBuilder.addChoices("Region Any (Option Disable)");

        selectAccountBuilder.setText(getUpdateMessageAfterChangeSelectAction(regionChoices))
        .setSelectionConsumer(getSelectionDoneAction(regionsList));

        SelectionDialog dialog = selectAccountBuilder.build();
        dialog.display(event.getChannel());
      }};
  }

  private Function<Integer, String> getUpdateMessageAfterChangeSelectAction(List<String> choices) {
    return new Function<Integer, String>() {
      @Override
      public String apply(Integer index) {
        if(choices.size() == index) {
          return "Region selected : \"**Any**\"";
        }

        return "Region selected : \"**" + choices.get(index - 1) + "**\"";
      }
    };
  }

  private BiConsumer<Message, Integer> getSelectionDoneAction(List<Platform> regionsList) {
    return new BiConsumer<Message, Integer>() {
      @Override
      public void accept(Message selectionMessage, Integer selectionOfRegion) {

        selectionMessage.clearReactions().queue();

        String strRegion;
        if(regionsList.size() == selectionOfRegion - 1) {
          strRegion = "Any";
          region = null;
        }else {
          strRegion = regionsList.get(selectionOfRegion - 1).getName().toUpperCase();
          region = regionsList.get(selectionOfRegion - 1);
        }

        selectionMessage.getTextChannel().sendMessage("The default region of the server is now \"**" + strRegion + "**\".").queue();
      }
    };
  }

  private Consumer<Message> getSelectionCancelAction(){
    return new Consumer<Message>() {
      @Override
      public void accept(Message message) {
        message.clearReactions().queue();
        message.editMessage("Selection canceled").queue();
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
