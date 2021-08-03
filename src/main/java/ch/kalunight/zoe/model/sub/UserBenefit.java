package ch.kalunight.zoe.model.sub;

import java.util.ArrayList;
import java.util.List;

public enum UserBenefit {
  ACCESS_NEW_FEATURES_SMALL_AND_MEDIUM_SERVERS(0, "accessNewFeaturesSmallAndMediumName"),
  ACCESS_NEW_FEATURES_HUGE_SERVERS(1, "accessNewFeaturesHugeName"),
  NAME_ON_WEBSITE(2, "nameOnTheWebsiteName"),
  NAME_ON_RELEASE_NOTE(3, "nameOnReleaseNoteName"),
  NAME_ON_GITHUB(4, "nameOnGithubName"),
  EXCLUSIVE_DEV_NEWS(5, "exclusiveDevNewsName"),
  EVOLUTIVE_SUPPORTER_EMOTE(6, "evolutiveSupporterEmoteName"),
  BUG_REPORT_PRIORITY(7, "bugReportPriorityName"),
  SUPPORT_THE_PROJECT(8, "supportTheProjectName"),
  EXCLUSIVE_ACCESS_SUPPORTER_CHANNEL(9, "exclusiveAccessToSupporterChannelName");
  
  public static final int HUGE_SERVERS_START = 500;
  
  private int id;
  private String nameId;
  
  private UserBenefit(int id, String nameId) {
    this.id = id;
    this.nameId = nameId;
  }
  
  public static List<UserBenefit> getBenefitsByUserRank(UserRank rank){
    List<UserBenefit> userBenefits = new ArrayList<>();
    
    switch(rank) {
    case STAFF:
      userBenefits.add(ACCESS_NEW_FEATURES_SMALL_AND_MEDIUM_SERVERS);
      userBenefits.add(ACCESS_NEW_FEATURES_HUGE_SERVERS);
      userBenefits.add(EXCLUSIVE_DEV_NEWS);
      userBenefits.add(EXCLUSIVE_ACCESS_SUPPORTER_CHANNEL);
      userBenefits.add(SUPPORT_THE_PROJECT);
      break;
    case EXCEPTIONNAL_FEATURES_ACCESS:
      userBenefits.add(ACCESS_NEW_FEATURES_SMALL_AND_MEDIUM_SERVERS);
      userBenefits.add(ACCESS_NEW_FEATURES_HUGE_SERVERS);
      break;
    case SUB_TIER_3:
    case DEV:
      userBenefits.add(NAME_ON_GITHUB);
      userBenefits.add(NAME_ON_RELEASE_NOTE);
      userBenefits.add(ACCESS_NEW_FEATURES_HUGE_SERVERS);
    case SUB_TIER_2:
      userBenefits.add(ACCESS_NEW_FEATURES_SMALL_AND_MEDIUM_SERVERS);
      userBenefits.add(BUG_REPORT_PRIORITY);
    case SUB_TIER_1:
      userBenefits.add(NAME_ON_WEBSITE);
      userBenefits.add(SUPPORT_THE_PROJECT);
      userBenefits.add(EVOLUTIVE_SUPPORTER_EMOTE);
      userBenefits.add(EXCLUSIVE_DEV_NEWS);
      userBenefits.add(EXCLUSIVE_ACCESS_SUPPORTER_CHANNEL);
      break;
    default:
      break;
    }
    
    return userBenefits;
  }

  public int getId() {
    return id;
  }

  public String getNameId() {
    return nameId;
  }
  
}
