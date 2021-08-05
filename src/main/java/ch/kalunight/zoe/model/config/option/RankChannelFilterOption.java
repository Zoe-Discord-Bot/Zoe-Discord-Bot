package ch.kalunight.zoe.model.config.option;

import java.awt.Color;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.ButtonMenu;

import ch.kalunight.zoe.model.CommandGuildDiscordData;
import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.repositories.ConfigRepository;
import ch.kalunight.zoe.repositories.RepoRessources;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;

public class RankChannelFilterOption extends ConfigurationOption {

  private static final String UNICODE_ONE = "1\u20E3";
  private static final String EMOJI_ONE = ":one:";

  private static final String UNICODE_TWO = "2\u20E3";
  private static final String EMOJI_TWO = ":two:";

  private static final String UNICODE_THREE = "3\u20E3";
  private static final String EMOJI_THREE = ":three:";

  public enum RankChannelFilter {
    ALL("rankChannelFilterOptionAll", "rankChannelFilterOptionAllDesc", UNICODE_ONE, EMOJI_ONE),
    LOL_ONLY("rankChannelFilterOptionLoL", "rankChannelFilterOptionLoLDesc", UNICODE_TWO, EMOJI_TWO),
    TFT_ONLY("rankChannelFilterOptionTFT", "rankChannelFilterOptionTFTDesc", UNICODE_THREE, EMOJI_THREE);

    public final String optionName;
    public final String description;
    public final String unicode;
    public final String emoji;

    private RankChannelFilter(String optionName, String description, String unicode, String emoji) {
      this.optionName = optionName;
      this.description = description;
      this.unicode = unicode;
      this.emoji = emoji;
    }
  }

  private RankChannelFilter rankchannelFilter;

  /**
   * Used when update option, this is not the state of the option.
   */
  private RankChannelFilter tmpRankChannelFilter;

  public RankChannelFilterOption(long guildId) {
    super(guildId, "rankChannelFilterOptionName", "rankChannelFilterOptionDesc",
        OptionCategory.RANKCHANNEL, false);
    rankchannelFilter = RankChannelFilter.ALL;
  }

  @Override
  public Consumer<CommandGuildDiscordData> getChangeConsumer(EventWaiter waiter, DTO.Server server) {
    return new Consumer<CommandGuildDiscordData>() {

      @Override
      public void accept(CommandGuildDiscordData event) {

        ButtonMenu.Builder choiceBuilder = new ButtonMenu.Builder();

        choiceBuilder.setEventWaiter(waiter);

        choiceBuilder.addChoices(
            RankChannelFilter.ALL.unicode,
            RankChannelFilter.LOL_ONLY.unicode,
            RankChannelFilter.TFT_ONLY.unicode,
            "❌");
        choiceBuilder.addUsers(event.getUser());
        choiceBuilder.setFinalAction(finalAction());
        choiceBuilder.setColor(Color.BLUE);

        choiceBuilder.setTimeout(2, TimeUnit.MINUTES);

        choiceBuilder.setText(
            String.format(LanguageManager.getText(server.getLanguage(), "rankChannelFilterOptionLongDesc"),
                LanguageManager.getText(server.getLanguage(), description))
            + "\n" + RankChannelFilter.ALL.emoji 
            + " -> " + LanguageManager.getText(server.getLanguage(), RankChannelFilter.ALL.optionName)
            + " : " + LanguageManager.getText(server.getLanguage(), RankChannelFilter.ALL.description) + "\n"
            + RankChannelFilter.LOL_ONLY.emoji
            + " -> " + LanguageManager.getText(server.getLanguage(), RankChannelFilter.LOL_ONLY.optionName)
            + " : " + LanguageManager.getText(server.getLanguage(), RankChannelFilter.LOL_ONLY.description) + "\n"
            + RankChannelFilter.TFT_ONLY.emoji
            + " -> " + LanguageManager.getText(server.getLanguage(), RankChannelFilter.TFT_ONLY.optionName)
            + " : " + LanguageManager.getText(server.getLanguage(), RankChannelFilter.TFT_ONLY.description) + "\n");

        choiceBuilder.setAction(updateOption(event.getChannel(), waiter, event.getUser(), server));

        ButtonMenu menu = choiceBuilder.build();

        menu.display(event.getChannel());
      }};
  }

  private Consumer<ReactionEmote> updateOption(MessageChannel channel, EventWaiter eventWaiter,
      User user, DTO.Server server) {
    return new Consumer<ReactionEmote>() {

      @Override
      public void accept(ReactionEmote emoteUsed) {
        channel.sendTyping().complete();

        if(emoteUsed.getName().equals(RankChannelFilter.ALL.unicode)) {
          tmpRankChannelFilter = RankChannelFilter.ALL;
        }else if(emoteUsed.getName().equals(RankChannelFilter.LOL_ONLY.unicode)) {
          tmpRankChannelFilter = RankChannelFilter.LOL_ONLY;
        }else if(emoteUsed.getName().equals(RankChannelFilter.TFT_ONLY.unicode)){
          tmpRankChannelFilter = RankChannelFilter.TFT_ONLY;
        }else {
          channel.sendMessage(LanguageManager.getText(server.getLanguage(), "cleanChannelOptionCanceled")).queue();
          return;
        }

        try {
          ConfigRepository.updateRankchannelFilter(guildId, tmpRankChannelFilter, channel.getJDA());
        } catch(SQLException e) {
          RepoRessources.sqlErrorReport(channel, server, e);
          return;
        }

        if(tmpRankChannelFilter.equals(rankchannelFilter)) {
          channel.sendMessage(LanguageManager.getText(server.getLanguage(), "rankChannelFilterOptionNotChanged")).queue();
        }else {
          rankchannelFilter = tmpRankChannelFilter;
          channel.sendMessage(LanguageManager.getText(server.getLanguage(), "rankChannelFilterOptionChanged")).queue();
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
  public String getBaseChoiceText(String langage) throws SQLException {
    String status = LanguageManager.getText(langage, "rankChannelFilterOptionAll");

    switch(rankchannelFilter) {
    case ALL:
      status = LanguageManager.getText(langage, "rankChannelFilterOptionAll");
      break;
    case LOL_ONLY:
      status = LanguageManager.getText(langage, "rankChannelFilterOptionLoLOnly");
      break;
    case TFT_ONLY:
      status = LanguageManager.getText(langage, "rankChannelFilterOptionTFTOnly");
      break;
    }

    return LanguageManager.getText(langage, description) + " : " + status;
  }

  public RankChannelFilter getRankChannelFilter() {
    return rankchannelFilter;
  }

  public void setRankChannelFilter(RankChannelFilter rankChannelFilter) {
    this.rankchannelFilter = rankChannelFilter;
  }

}
