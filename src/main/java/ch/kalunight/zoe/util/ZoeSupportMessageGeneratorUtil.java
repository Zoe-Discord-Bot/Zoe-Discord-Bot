package ch.kalunight.zoe.util;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.kalunight.zoe.model.dto.DTO;
import ch.kalunight.zoe.model.sub.UserRank;
import ch.kalunight.zoe.repositories.ZoeUserManagementRepository;
import ch.kalunight.zoe.translation.LanguageManager;

public class ZoeSupportMessageGeneratorUtil {

  private static final List<String> supportPhrasesId = Collections.synchronizedList(new ArrayList<>());

  private static final List<String> incitativeSupportPhrasesId = Collections.synchronizedList(new ArrayList<>());

  private static final Logger logger = LoggerFactory.getLogger(ZoeSupportMessageGeneratorUtil.class);

  private static final Random rand = new Random();

  private static final int SUPPORT_TARGET = 80;

  private static final int CHANCE_RANDOM_MAX_RANGE = 10;

  private static final int CHANCE_TO_SHOW_INCITATIVE = 5; // 5 chance on 10

  private static Integer nbrSupporters = null;

  private static LocalDateTime nextRefresh = LocalDateTime.now().minusDays(1);

  static {
    supportPhrasesId.add("supportMessageDeveloppedOnePerson");
    supportPhrasesId.add("supportMessageSubscription");
    supportPhrasesId.add("supportMessagePlayersSupport");
    supportPhrasesId.add("supportMessageSubscriptionCommand");
    supportPhrasesId.add("supportMessageCoolBadge");
    supportPhrasesId.add("supportMessageEarlyAccess");
    supportPhrasesId.add("supportMessagePayServer");
    supportPhrasesId.add("supportMessageExclusiveDevNews");
    supportPhrasesId.add("supportMessageNameOnWebsite");
    supportPhrasesId.add("wikiMessage");
    supportPhrasesId.add("needHelpMessage");
    supportPhrasesId.add("supportMessageZoeLoveDev");
    supportPhrasesId.add("translationSupportMessagePromote");
    supportPhrasesId.add("translationErrorMessagePromote");

    incitativeSupportPhrasesId.add("supportMessageZoeNeedYourHelp");
    incitativeSupportPhrasesId.add("supportMessageZoeLoss");
    incitativeSupportPhrasesId.add("supportMessageZoeNeeded");
    incitativeSupportPhrasesId.add("supportMessageZoeLoveDonation");
    incitativeSupportPhrasesId.add("supportMessageZoeFutur");
    incitativeSupportPhrasesId.add("supportMessageZoeNeededTLDR");
  }

  private ZoeSupportMessageGeneratorUtil() {
    // hide default public constructor
  }

  public static String getRandomIncitativeSupportPhrase(String language) {
    String randomPromotionMessage = incitativeSupportPhrasesId.get(rand.nextInt(incitativeSupportPhrasesId.size()));

    if(randomPromotionMessage.equals("supportMessageZoeNeeded") 
        || randomPromotionMessage.equals("supportMessageZoeNeededTLDR")) {
      try {
        List<DTO.ZoeUserRole> subscribersTier1 = 
            ZoeUserManagementRepository.getZoeSubscriptionByRole(UserRank.SUB_TIER_1.getId());

        List<DTO.ZoeUserRole> subscribersTier2 = 
            ZoeUserManagementRepository.getZoeSubscriptionByRole(UserRank.SUB_TIER_2.getId());

        List<DTO.ZoeUserRole> subscribersTier3 = 
            ZoeUserManagementRepository.getZoeSubscriptionByRole(UserRank.SUB_TIER_3.getId());

        int totalPerMonth = 0;

        totalPerMonth += (subscribersTier1.size() * UserRank.SUB_TIER_1.getValue());
        totalPerMonth += (subscribersTier2.size() * UserRank.SUB_TIER_2.getValue());
        totalPerMonth += (subscribersTier3.size() * UserRank.SUB_TIER_3.getValue());

        if(randomPromotionMessage.equals("supportMessageZoeNeeded")) {
          return String.format(LanguageManager.getText(language, randomPromotionMessage),
              totalPerMonth, SUPPORT_TARGET);
        }else {
          return String.format(LanguageManager.getText(language, randomPromotionMessage),
              SUPPORT_TARGET, totalPerMonth, SUPPORT_TARGET);
        }

      }catch (SQLException e) {
        logger.error("SQL Error while getting all Zoe subscribers! Get Another Random Phrases", e);
        return getRandomIncitativeSupportPhrase(language);
      }
    }

    return LanguageManager.getText(language, randomPromotionMessage);
  }

  public static String getRandomSupportPhrase(String language) {

    int randomNumber = rand.nextInt(CHANCE_RANDOM_MAX_RANGE);
    if(randomNumber < CHANCE_TO_SHOW_INCITATIVE) {
      return getRandomIncitativeSupportPhrase(language);
    }else {
      return getRandomClassicSupportMessage(language);
    }
  }

  private static String getRandomClassicSupportMessage(String language) {
    String randomPromotionMessage = supportPhrasesId.get(rand.nextInt(supportPhrasesId.size()));

    if(randomPromotionMessage.equals("supportMessagePlayersSupport")) {
      if(nextRefresh.isBefore(LocalDateTime.now()) || nbrSupporters == null) {
        try {
          List<DTO.ZoeUserRole> subscribersTier1 = 
              ZoeUserManagementRepository.getZoeSubscriptionByRole(UserRank.SUB_TIER_1.getId());

          List<DTO.ZoeUserRole> subscribersTier2 = 
              ZoeUserManagementRepository.getZoeSubscriptionByRole(UserRank.SUB_TIER_2.getId());

          List<DTO.ZoeUserRole> subscribersTier3 = 
              ZoeUserManagementRepository.getZoeSubscriptionByRole(UserRank.SUB_TIER_3.getId());

          List<DTO.ZoeUserRole> staff = 
              ZoeUserManagementRepository.getZoeSubscriptionByRole(UserRank.STAFF.getId());

          nbrSupporters = subscribersTier1.size() + subscribersTier2.size() 
          + subscribersTier3.size() + staff.size();

          nextRefresh = LocalDateTime.now().plusDays(1);
        }catch (SQLException e) {
          logger.error("SQL Error while getting all Zoe subscribers! Get Another Random Phrases", e);
          return getRandomClassicSupportMessage(language);
        }
      }

      return String.format(LanguageManager.getText(language, randomPromotionMessage), nbrSupporters);
    }

    return LanguageManager.getText(language, randomPromotionMessage);
  }

}
