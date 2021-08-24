package ch.kalunight.zoe.command.delete;

import java.awt.Color;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.SelectionDialog;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.model.dto.DTO.ClashChannel;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.repositories.ClashChannelRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import no.stelar7.api.r4j.basic.exceptions.APIResponseException;
import no.stelar7.api.r4j.pojo.lol.summoner.Summoner;

public class DeleteClashChannelCommandRunnable {

  private static final Logger logger = LoggerFactory.getLogger(DeleteClashChannelCommandRunnable.class);
  
  private DeleteClashChannelCommandRunnable() {
    // hide default public constructor
  }
  
  public static void executeCommand(Server server, EventWaiter eventWaiter, Member author, Message messageLoading, TextChannel commandChannel, InteractionHook hook) throws SQLException {

    List<ClashChannel> clashChannelList = ClashChannelRepository.getClashChannels(server.serv_guildId);
    List<ClashChannel> clashChannelChoiceInOrder = new ArrayList<>();
    AtomicBoolean actionDone = new AtomicBoolean(false);

    if(clashChannelList.isEmpty()) {
      String message = LanguageManager.getText(server.getLanguage(), "deleteClashChannelCommandNoClashChannelToDelete");
      if(messageLoading != null) {
        messageLoading.editMessage(message).queue();
      }else {
        hook.editOriginal(message).queue();
      }
      return;
    }
    
    SelectionDialog.Builder selectClashChannelBuilder = new SelectionDialog.Builder()
        .addUsers(author.getUser())
        .setEventWaiter(eventWaiter)
        .useLooping(true)
        .setColor(Color.GREEN)
        .setSelectedEnds("**", "**")
        .setCanceled(getSelectionCancelAction(server.getLanguage(), actionDone))
        .setSelectionConsumer(getSelectionConsumer(server, clashChannelList, actionDone, commandChannel))
        .setTimeout(2, TimeUnit.MINUTES);

    for(ClashChannel clashChannel : clashChannelList) {
      TextChannel channel = commandChannel.getGuild().getTextChannelById(clashChannel.clashChannel_channelId);
      if(channel == null) {
        logger.info("One clash channel has been deleted, refresh db stats...");
        ClashChannelRepository.deleteClashChannel(clashChannel.clashChannel_id);
        continue;
      }

      Summoner summoner;
      String showableAccountOwner;
      try {
        summoner = Zoe.getRiotApi().getSummonerBySummonerId(clashChannel.clashChannel_data.getSelectedPlatform(), clashChannel.clashChannel_data.getSelectedSummonerId());
        showableAccountOwner = "*" + clashChannel.clashChannel_data.getSelectedPlatform().getShowableName() + "* " + summoner.getName();
      } catch (APIResponseException e) {
        logger.warn("Riot exception in delete clash channel command.", e);
        showableAccountOwner = "*" + clashChannel.clashChannel_data.getSelectedPlatform().getShowableName() + "* " + LanguageManager.getText(server.getLanguage(), "unknown");
      }
      
      String showableString = channel.getName() + " : " + showableAccountOwner;

      clashChannelChoiceInOrder.add(clashChannel);
      selectClashChannelBuilder.addChoices(showableString);
    }

    selectClashChannelBuilder.setText(LanguageManager.getText(server.getLanguage(), "deleteClashChannelCommandListProposal"));

    SelectionDialog choiceClashChannels = selectClashChannelBuilder.build();
    choiceClashChannels.display(commandChannel);
  }

  private static Consumer<Message> getSelectionCancelAction(String language, AtomicBoolean actionDone){
    return new Consumer<Message>() {
      @Override
      public void accept(Message message) {
        if(!actionDone.get()) {
          message.clearReactions().queue();
          message.editMessage(LanguageManager.getText(language, "deleteLeaderboardCancelMessage")).queue();
        }
      }
    };
  }

  private static BiConsumer<Message, Integer> getSelectionConsumer(Server server, List<ClashChannel> clashChannelList, AtomicBoolean actionDone, TextChannel channel) {
    return new BiConsumer<Message, Integer>() {
      @Override
      public void accept(Message selectionMessage, Integer objectiveSelection) {
        channel.sendTyping().queue();
        selectionMessage.clearReactions().queue();
        actionDone.set(true);

        ClashChannel clashChannel = clashChannelList.get(objectiveSelection - 1);

        TextChannel channelToClean = channel.getGuild().getTextChannelById(clashChannel.clashChannel_channelId);

        List<Long> messagesIdToDelete = clashChannel.clashChannel_data.getAllClashChannel();

        for(Long messageIdToDelete : messagesIdToDelete) {
          Message message = channelToClean.retrieveMessageById(messageIdToDelete).complete();

          message.delete().queue();
        }

        try {
          ClashChannelRepository.deleteClashChannel(clashChannel.clashChannel_id);
        } catch(SQLException e) {
          logger.error("SQL Exception throw while deleted a clash channel !", e);
          channel.sendMessage(LanguageManager.getText(server.getLanguage(), "deleteLeaderboardErrorDatabase")).queue();
          return;
        }

        channel.sendMessage(LanguageManager.getText(server.getLanguage(), "deleteClashChannelDoneCorrectly")).queue();
      }
    };
  }

}
