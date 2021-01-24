package ch.kalunight.zoe.command.delete;

import java.awt.Color;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.SelectionDialog;

import ch.kalunight.zoe.Zoe;
import ch.kalunight.zoe.command.ZoeCommand;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.dto.DTO.ClashChannel;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.dto.DTO.SummonerCache;
import ch.kalunight.zoe.repositories.ClashChannelRepository;
import ch.kalunight.zoe.translation.LanguageManager;
import ch.kalunight.zoe.util.CommandUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.rithms.riot.api.RiotApiException;

public class DeleteClashChannelCommand extends ZoeCommand {

  private EventWaiter eventWaiter;

  public DeleteClashChannelCommand(EventWaiter waiter) {
    this.name = "clashChannel";
    String[] aliases = {"clash", "cc"};
    this.aliases = aliases;
    this.arguments = "";
    this.eventWaiter = waiter;
    this.help = "deleteClashChannelCommandHelpMessage";
    Permission[] permissionRequired = {Permission.MANAGE_CHANNEL};
    this.userPermissions = permissionRequired;
    Permission[] botPermissionRequiered = {Permission.MANAGE_CHANNEL, Permission.MANAGE_EMOTES, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ADD_REACTION};
    this.botPermissions = botPermissionRequiered;
    this.helpBiConsumer = CommandUtil.getHelpMethodIsChildren(DeleteCommand.USAGE_NAME, name, arguments, help);
  }

  @Override
  protected void executeCommand(CommandEvent event) throws SQLException {
    DTO.Server server = getServer(event.getGuild().getIdLong());

    List<ClashChannel> clashChannelList = ClashChannelRepository.getClashChannels(server.serv_guildId);
    List<ClashChannel> clashChannelChoiceInOrder = new ArrayList<>();
    AtomicBoolean actionDone = new AtomicBoolean(false);

    if(clashChannelList.isEmpty()) {
      event.reply(LanguageManager.getText(server.getLanguage(), "deleteClashChannelCommandNoClashChannelToDelete"));
      return;
    }

    Message messageLoading = event.getTextChannel().sendMessage(LanguageManager.getText(server.getLanguage(), "loadingData")).complete();
    
    SelectionDialog.Builder selectClashChannelBuilder = new SelectionDialog.Builder()
        .addUsers(event.getAuthor())
        .setEventWaiter(eventWaiter)
        .useLooping(true)
        .setColor(Color.GREEN)
        .setSelectedEnds("**", "**")
        .setCanceled(getSelectionCancelAction(server.getLanguage(), actionDone))
        .setSelectionConsumer(getSelectionConsumer(server, event, clashChannelList, actionDone))
        .setTimeout(2, TimeUnit.MINUTES);

    for(ClashChannel clashChannel : clashChannelList) {
      TextChannel channel = event.getGuild().getTextChannelById(clashChannel.clashChannel_channelId);
      if(channel == null) {
        logger.info("One clash channel has been deleted, refresh db stats...");
        ClashChannelRepository.deleteClashChannel(clashChannel.clashChannel_id);
        continue;
      }

      SummonerCache summoner;
      String showableAccountOwner;
      try {
        summoner = Zoe.getRiotApi().getSummonerWithRateLimit(clashChannel.clashChannel_data.getSelectedPlatform(), clashChannel.clashChannel_data.getSelectedSummonerId(), false);
        showableAccountOwner = "*" + clashChannel.clashChannel_data.getSelectedPlatform().getName().toUpperCase() 
            + "* " + summoner.getSumCacheData().getName();
      } catch (RiotApiException e) {
        logger.warn("Riot exception in delete clash channel command.", e);
        showableAccountOwner = "*" + clashChannel.clashChannel_data.getSelectedPlatform().getName().toUpperCase() + "* " + LanguageManager.getText(server.getLanguage(), "unknown");
      }
      
      String showableString = channel.getName() + " : " + showableAccountOwner;

      clashChannelChoiceInOrder.add(clashChannel);
      selectClashChannelBuilder.addChoices(showableString);
    }

    selectClashChannelBuilder.setText(LanguageManager.getText(server.getLanguage(), "deleteClashChannelCommandListProposal"));

    SelectionDialog choiceClashChannels = selectClashChannelBuilder.build();
    choiceClashChannels.display(messageLoading);
  }

  private Consumer<Message> getSelectionCancelAction(String language, AtomicBoolean actionDone){
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

  private BiConsumer<Message, Integer> getSelectionConsumer(Server server, CommandEvent event, List<ClashChannel> clashChannelList, AtomicBoolean actionDone) {
    return new BiConsumer<Message, Integer>() {
      @Override
      public void accept(Message selectionMessage, Integer objectiveSelection) {
        event.getChannel().sendTyping().queue();
        selectionMessage.clearReactions().queue();
        actionDone.set(true);

        ClashChannel clashChannel = clashChannelList.get(objectiveSelection - 1);

        TextChannel channelToClean = event.getGuild().getTextChannelById(clashChannel.clashChannel_channelId);

        List<Long> messagesIdToDelete = clashChannel.clashChannel_data.getAllClashChannel();

        for(Long messageIdToDelete : messagesIdToDelete) {
          Message message = channelToClean.retrieveMessageById(messageIdToDelete).complete();

          message.delete().queue();
        }

        try {
          ClashChannelRepository.deleteClashChannel(clashChannel.clashChannel_id);
        } catch(SQLException e) {
          logger.error("SQL Exception throw while deleted a clash channel !", e);
          event.reply(LanguageManager.getText(server.getLanguage(), "deleteLeaderboardErrorDatabase"));
          return;
        }

        event.reply(LanguageManager.getText(server.getLanguage(), "deleteClashChannelDoneCorrectly"));
      }
    };
  }

  @Override
  public BiConsumer<CommandEvent, Command> getHelpBiConsumer(CommandEvent event) {
    return helpBiConsumer;
  }

}
