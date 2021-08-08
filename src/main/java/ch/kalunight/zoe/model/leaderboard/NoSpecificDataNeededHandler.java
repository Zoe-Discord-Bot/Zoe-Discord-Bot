package ch.kalunight.zoe.model.leaderboard;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import ch.kalunight.zoe.model.dto.DTO.Server;
import ch.kalunight.zoe.model.leaderboard.dataholder.Objective;
import ch.kalunight.zoe.translation.LanguageManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

public class NoSpecificDataNeededHandler extends LeaderboardExtraDataHandler {

  public NoSpecificDataNeededHandler(Objective objective, EventWaiter waiter, Server server, boolean forceRefreshCheck, Member author, TextChannel channel) {
    super(objective, waiter, server, forceRefreshCheck, author, channel);
  }

  @Override
  public void handleSecondCreationPart() {
    channel.sendMessage(LanguageManager.getText(server.getLanguage(), "leaderboardSelectChannelLeaderboard")).queue();
    handleEndOfCreation("");
  }

}
