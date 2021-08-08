package ch.kalunight.zoe.model;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class CommandGuildDiscordData {

  private Member member;
  private Guild guild;
  private TextChannel channel;

  public CommandGuildDiscordData(Member member, Guild guild, TextChannel channel) {
    this.member = member;
    this.guild = guild;
    this.channel = channel;
  }

  public Member getMember() {
    return member;
  }

  public User getUser() {
    return member.getUser();
  }

  public Guild getGuild() {
    return guild;
  }

  public TextChannel getChannel() {
    return channel;
  }
}
