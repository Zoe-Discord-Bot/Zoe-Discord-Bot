package ch.kalunight.zoe.riotapi;

import java.util.ArrayList;
import java.util.List;

import org.bson.conversions.Bson;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Projections;

import ch.kalunight.zoe.model.dto.ClashTournamentMongodb;
import ch.kalunight.zoe.model.dto.ZoePlatform;
import no.stelar7.api.r4j.impl.R4J;
import no.stelar7.api.r4j.impl.lol.builders.championmastery.ChampionMasteryBuilder;
import no.stelar7.api.r4j.impl.lol.builders.league.LeagueBuilder;
import no.stelar7.api.r4j.impl.lol.builders.matchv5.match.MatchBuilder;
import no.stelar7.api.r4j.impl.lol.builders.matchv5.match.MatchListBuilder;
import no.stelar7.api.r4j.impl.lol.builders.thirdparty.ThirdPartyCodeBuilder;
import no.stelar7.api.r4j.pojo.lol.championmastery.ChampionMastery;
import no.stelar7.api.r4j.pojo.lol.clash.ClashPlayer;
import no.stelar7.api.r4j.pojo.lol.clash.ClashTournament;
import no.stelar7.api.r4j.pojo.lol.league.LeagueEntry;
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.lol.summoner.Summoner;

public class CachedRiotApi {

  private static final String CLASH_TOURNAMENT_V1 = "CLASH-TOURNAMENT-V1";

  private static final String MASTERY_V4 = "MASTERY-V4";

  private static final String SUMMONER_V4 = "SUMMONER-V4";

  private static final String TFT_SUMMONER_V1 = "TFT-SUMMONER-V1";

  private static final String MATCH_V5 = "MATCH-V5";

  private R4J riotApi; 

  private MongoClient client;

  private MongoDatabase cacheDatabase;

  private MongoCollection<LOLMatch> matchCache;

  private MongoCollection<Summoner> summonerCache;

  private MongoCollection<Summoner> tftSummonerCache;

  private MongoCollection<ChampionMastery> championsMasteryCache;

  private MongoCollection<ClashTournamentMongodb> clashTournamentCache;

  public CachedRiotApi(R4J r4j, String connectString, String dbName) {
    this.riotApi = r4j;
    client = new MongoClient(new MongoClientURI(connectString));
    cacheDatabase = client.getDatabase(dbName);

    matchCache = cacheDatabase.getCollection(MATCH_V5, LOLMatch.class);
    if(matchCache == null) {
      cacheDatabase.createCollection(MATCH_V5);
      matchCache = cacheDatabase.getCollection(MATCH_V5, LOLMatch.class);
    }

    summonerCache = cacheDatabase.getCollection(SUMMONER_V4, Summoner.class);
    if(summonerCache == null) {
      cacheDatabase.createCollection(SUMMONER_V4);
      summonerCache = cacheDatabase.getCollection(SUMMONER_V4, Summoner.class);
    }

    tftSummonerCache = cacheDatabase.getCollection(TFT_SUMMONER_V1, Summoner.class);
    if(summonerCache == null) {
      cacheDatabase.createCollection(TFT_SUMMONER_V1);
      summonerCache = cacheDatabase.getCollection(TFT_SUMMONER_V1, Summoner.class);
    }

    championsMasteryCache = cacheDatabase.getCollection(MASTERY_V4, ChampionMastery.class);
    if(championsMasteryCache == null) {
      cacheDatabase.createCollection(MASTERY_V4);
      championsMasteryCache = cacheDatabase.getCollection(MASTERY_V4, ChampionMastery.class);
      IndexOptions options = new IndexOptions();
      options.name("championMasteryIndexConstraint");
      options.unique(true);

      Bson bson = Projections.include("playerId", "championId");
      championsMasteryCache.createIndex(bson, options);
    }

    clashTournamentCache = cacheDatabase.getCollection(CLASH_TOURNAMENT_V1, ClashTournamentMongodb.class);
    if(clashTournamentCache == null) {
      cacheDatabase.createCollection(CLASH_TOURNAMENT_V1);
      clashTournamentCache = cacheDatabase.getCollection(CLASH_TOURNAMENT_V1, ClashTournamentMongodb.class);
    }
  }

  public Summoner getSummonerByName(ZoePlatform platform, String summonerName) {
    Summoner summoner = riotApi.getLoLAPI().getSummonerAPI().getSummonerByName(platform.getLeagueShard(), summonerName);

    summonerCache.insertOne(summoner);

    return summoner;
  }

  public Summoner getSummonerBySummonerId(ZoePlatform platform, String summonerId) {
    Bson matchWanted = Projections.computed("id", summonerId);

    Summoner summoner = summonerCache.find(matchWanted).first();

    if(summoner != null) {
      return summoner;
    }

    summoner = riotApi.getLoLAPI().getSummonerAPI().getSummonerById(platform.getLeagueShard(), summonerId);

    summonerCache.insertOne(summoner);

    return summoner;
  }

  public Summoner getTFTSummonerByName(ZoePlatform platform, String summonerName) {
    Summoner summoner = riotApi.getTFTAPI().getSummonerAPI().getSummonerByName(platform.getLeagueShard(), summonerName);

    tftSummonerCache.insertOne(summoner);

    return summoner;
  }

  public Summoner getTFTSummonerBySummonerId(ZoePlatform platform, String summonerId) {
    Bson matchWanted = Projections.computed("id", summonerId);

    Summoner tftSummoner = tftSummonerCache.find(matchWanted).first();

    if(tftSummoner != null) {
      return tftSummoner;
    }

    tftSummoner = riotApi.getTFTAPI().getSummonerAPI().getSummonerById(platform.getLeagueShard(), summonerId);

    tftSummonerCache.insertOne(tftSummoner);

    return tftSummoner;
  }

  public LOLMatch getMatchById(ZoePlatform platform, String matchId) {

    Bson matchWanted = Projections.computed("matchId", matchId);

    LOLMatch matchDB = matchCache.find(matchWanted).first();

    if(matchDB != null) {
      return matchDB;
    }

    LOLMatch match = new MatchBuilder().withPlatform(platform.getLeagueShard().toRegionShard()).withId(matchId).getMatch();

    matchCache.insertOne(matchDB);

    return match;
  }

  public List<String> getMatchListBySummonerId(ZoePlatform platform, String puuid){
    MatchListBuilder builder = new MatchListBuilder();

    builder.withPlatform(platform.getLeagueShard());
    builder.withPuuid(puuid);


    return builder.get();
  }

  public List<LeagueEntry> getLeagueEntryBySummonerId(ZoePlatform platform, String summonerId) {
    return new LeagueBuilder().withPlatform(platform.getLeagueShard()).withSummonerId(summonerId).getLeagueEntries();
  }

  public List<LeagueEntry> getTFTLeagueEntryByTFTSummonerId(ZoePlatform platform, String summonerId) {
    return riotApi.getTFTAPI().getLeagueAPI().getLeagueEntries(platform.getLeagueShard(), summonerId);
  }

  public List<ChampionMastery> getChampionMasteryBySummonerId(ZoePlatform platform, String summonerId) {

    Bson matchWanted = Projections.computed("playerId", summonerId);

    List<ChampionMastery> championMastery = new ArrayList<>();

    MongoCursor<ChampionMastery> cursor = championsMasteryCache.find(matchWanted).iterator();

    while(cursor.hasNext()) {
      championMastery.add(cursor.next());
    }

    if(championMastery.isEmpty()) {
      return championMastery;
    }

    championMastery = new ChampionMasteryBuilder().withPlatform(platform.getLeagueShard()).withSummonerId(summonerId).getChampionMasteries();

    championsMasteryCache.insertMany(championMastery);

    return championMastery;
  }

  public String getThirdPartyCode(ZoePlatform platform, String summonerId) {
    return new ThirdPartyCodeBuilder().withPlatform(platform.getLeagueShard()).withSummonerId(summonerId).getCode();
  }

  public List<ClashPlayer> getClashPlayerBySummonerId(ZoePlatform platform, String summonerId){
    return riotApi.getLoLAPI().getClashAPI().getPlayerInfo(platform.getLeagueShard(), summonerId);
  }

  public List<ClashTournament> getTournaments(ZoePlatform selectedPlatform) {

    Bson tournamentWanted = Projections.computed("serverName", selectedPlatform.getDbName());

    ClashTournamentMongodb tournaments = clashTournamentCache.find(tournamentWanted).first();
    
    if(tournaments != null) {
      return tournaments.getTournament();
    }

    List<ClashTournament> clashTournaments = riotApi.getLoLAPI().getClashAPI().getTournaments(selectedPlatform.getLeagueShard());
    
    ClashTournamentMongodb tournamentDb = new ClashTournamentMongodb(clashTournaments, selectedPlatform);

    clashTournamentCache.insertOne(tournamentDb);
    
    return clashTournaments;
  }

}
