package ch.kalunight.zoe.riotapi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientException;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Projections;

import ch.kalunight.zoe.model.dto.SavedClashTournament;
import ch.kalunight.zoe.model.dto.SavedMatch;
import ch.kalunight.zoe.model.dto.SavedSimpleMastery;
import ch.kalunight.zoe.model.dto.SavedSummoner;
import ch.kalunight.zoe.model.dto.ZoePlatform;
import no.stelar7.api.r4j.basic.constants.types.lol.GameQueueType;
import no.stelar7.api.r4j.basic.exceptions.APIHTTPErrorReason;
import no.stelar7.api.r4j.basic.exceptions.APIResponseException;
import no.stelar7.api.r4j.impl.R4J;
import no.stelar7.api.r4j.impl.lol.builders.championmastery.ChampionMasteryBuilder;
import no.stelar7.api.r4j.impl.lol.builders.league.LeagueBuilder;
import no.stelar7.api.r4j.impl.lol.builders.matchv5.match.MatchBuilder;
import no.stelar7.api.r4j.impl.lol.builders.matchv5.match.MatchListBuilder;
import no.stelar7.api.r4j.impl.lol.builders.thirdparty.ThirdPartyCodeBuilder;
import no.stelar7.api.r4j.pojo.lol.championmastery.ChampionMastery;
import no.stelar7.api.r4j.pojo.lol.clash.ClashPlayer;
import no.stelar7.api.r4j.pojo.lol.clash.ClashTeam;
import no.stelar7.api.r4j.pojo.lol.clash.ClashTournament;
import no.stelar7.api.r4j.pojo.lol.league.LeagueEntry;
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.lol.spectator.SpectatorGameInfo;
import no.stelar7.api.r4j.pojo.lol.summoner.Summoner;
import no.stelar7.api.r4j.pojo.tft.TFTMatch;
import no.stelar7.api.r4j.pojo.tft.league.TFTLeagueEntry;
import okio.Options;

public class CachedRiotApi {

  private static final String CLASH_TOURNAMENT_V1 = "CLASH-TOURNAMENT-V1";

  private static final String MASTERY_V4 = "MASTERY-V4";

  private static final String SUMMONER_V4 = "SUMMONER-V4";

  private static final String TFT_SUMMONER_V1 = "TFT-SUMMONER-V1";

  private static final String MATCH_V5 = "MATCH-V5";

  private R4J riotApi; 

  private MongoClient client;

  private MongoDatabase cacheDatabase;

  private MongoCollection<SavedMatch> matchCache;

  private MongoCollection<SavedSummoner> summonerCache;

  private MongoCollection<SavedSummoner> tftSummonerCache;

  private MongoCollection<SavedSimpleMastery> championsMasteryCache;

  private MongoCollection<SavedClashTournament> clashTournamentCache;

  public CachedRiotApi(R4J r4j, String connectString, String dbName) {
    this.riotApi = r4j;
    CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), org.bson.codecs.configuration.CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));
    client = new MongoClient(new MongoClientURI(connectString));

    Iterator<String> iterator = client.listDatabaseNames().iterator();

    boolean dbExist = false;

    while(iterator.hasNext()) {
      if(iterator.next().equals(dbName)) {
        dbExist = true;
      }
    }

    if(!dbExist) {
      throw new MongoClientException("Db doesn't exist! Please create it!");
    }

    cacheDatabase = client.getDatabase(dbName).withCodecRegistry(pojoCodecRegistry);

    List<String> allCollections = new ArrayList<>();
    Iterator<String> collectionsIterator = cacheDatabase.listCollectionNames().iterator();
    
    while(collectionsIterator.hasNext()) {
      allCollections.add(collectionsIterator.next());
    }

    Bson indexDateBson = Projections.fields(Projections.include("retrieveDate"));
    
    matchCache = cacheDatabase.getCollection(MATCH_V5, SavedMatch.class);
    if(!allCollections.contains(MATCH_V5)) {
      cacheDatabase.createCollection(MATCH_V5);
      matchCache = cacheDatabase.getCollection(MATCH_V5, SavedMatch.class);

      IndexOptions options = new IndexOptions();
      options.name("matchIndexConstraint");
      options.unique(true);

      Bson bson = Projections.include("zoePlatform", "gameId");
      matchCache.createIndex(bson, options);
      
      options = new IndexOptions();
      options.name("matchTTLIndex");
      options.expireAfter(14l, TimeUnit.DAYS);
      matchCache.createIndex(indexDateBson, options);
      
      options = new IndexOptions();
      options.name("matchChampionIdQueueIndex");
      
      bson = Projections.include("players.championId", "queueId");
      matchCache.createIndex(bson, options);
    }

    summonerCache = cacheDatabase.getCollection(SUMMONER_V4, SavedSummoner.class);
    if(!allCollections.contains(SUMMONER_V4)) {
      cacheDatabase.createCollection(SUMMONER_V4);
      summonerCache = cacheDatabase.getCollection(SUMMONER_V4, SavedSummoner.class);

      IndexOptions options = new IndexOptions();
      options.name("summonerIndexConstraint");
      options.unique(true);

      Bson bson = Projections.include("platform", "summonerId");
      summonerCache.createIndex(bson, options);
      
      options = new IndexOptions();
      options.name("summonerTTLIndex");
      options.expireAfter(10l, TimeUnit.SECONDS);
      summonerCache.createIndex(indexDateBson, options);
    }

    tftSummonerCache = cacheDatabase.getCollection(TFT_SUMMONER_V1, SavedSummoner.class);
    if(!allCollections.contains(TFT_SUMMONER_V1)) {
      cacheDatabase.createCollection(TFT_SUMMONER_V1);
      summonerCache = cacheDatabase.getCollection(TFT_SUMMONER_V1, SavedSummoner.class);

      IndexOptions options = new IndexOptions();
      options.name("tftSummonerIndexConstraint");
      options.unique(true);

      Bson bson = Projections.include("platform", "summonerId");
      summonerCache.createIndex(bson, options);
      
      options = new IndexOptions();
      options.name("TFTSummonerTTLIndex");
      options.expireAfter(1l, TimeUnit.DAYS);
      tftSummonerCache.createIndex(indexDateBson, options);
    }

    championsMasteryCache = cacheDatabase.getCollection(MASTERY_V4, SavedSimpleMastery.class);
    if(!allCollections.contains(MASTERY_V4)) {
      cacheDatabase.createCollection(MASTERY_V4);
      championsMasteryCache = cacheDatabase.getCollection(MASTERY_V4, SavedSimpleMastery.class);

      IndexOptions options = new IndexOptions();
      options.name("championMasteryIndexConstraint");
      options.unique(true);

      Bson bson = Projections.include("summonerId", "platform", "championId");
      championsMasteryCache.createIndex(bson, options);
      
      options = new IndexOptions();
      options.name("championMasteryTTLIndex");
      options.expireAfter(1l, TimeUnit.DAYS);
      championsMasteryCache.createIndex(indexDateBson, options);
    }

    clashTournamentCache = cacheDatabase.getCollection(CLASH_TOURNAMENT_V1, SavedClashTournament.class);
    if(!allCollections.contains(CLASH_TOURNAMENT_V1)) {
      cacheDatabase.createCollection(CLASH_TOURNAMENT_V1);
      clashTournamentCache = cacheDatabase.getCollection(CLASH_TOURNAMENT_V1, SavedClashTournament.class);

      IndexOptions options = new IndexOptions();
      options.name("clashTournamentIndexConstraint");
      options.unique(true);

      Bson bson = Projections.include("platform", "tournamentId");
      clashTournamentCache.createIndex(bson, options);
      
      options = new IndexOptions();
      options.name("clashTournamentTTLIndex");
      options.expireAfter(6l, TimeUnit.HOURS);
      clashTournamentCache.createIndex(indexDateBson, options);
    }
  }

  public SavedSummoner getSummonerByName(ZoePlatform platform, String summonerName) {
    Summoner summoner = riotApi.getLoLAPI().getSummonerAPI().getSummonerByName(platform.getLeagueShard(), summonerName);

    if(summoner == null) {
      throw new APIResponseException(APIHTTPErrorReason.ERROR_404, "Not found");
    }
    
    SavedSummoner savedSummoner = new SavedSummoner(summoner, platform);
    
    insertOrReplaceSummoner(savedSummoner, getSearchBsonForSummoner(platform, savedSummoner.getSummonerId()));
    
    return savedSummoner;
  }

  public SavedSummoner getSummonerBySummonerId(ZoePlatform platform, String summonerId) {

    SavedSummoner summoner = summonerCache.find(getSearchBsonForSummoner(platform, summonerId)).first();

    if(summoner != null) {
      return summoner;
    }

    Summoner summonerOriginal = riotApi.getLoLAPI().getSummonerAPI().getSummonerById(platform.getLeagueShard(), summonerId);

    if(summonerOriginal == null) {
      throw new APIResponseException(APIHTTPErrorReason.ERROR_404, "Not found");
    }
    
    summoner = new SavedSummoner(summonerOriginal, platform);

    insertOrReplaceSummoner(summoner, getSearchBsonForSummoner(platform, summoner.getSummonerId()));

    return summoner;
  }
  
  private void insertOrReplaceSummoner(SavedSummoner savedSummoner, Bson searchBsonForSummoner) {
    SavedSummoner summonerToReplace = summonerCache.find(searchBsonForSummoner).first();
    
    if(summonerToReplace == null) {
      summonerCache.insertOne(savedSummoner);
    }else {
      summonerCache.replaceOne(searchBsonForSummoner, savedSummoner);
    }
    
  }

  public SavedSummoner getTFTSummonerByName(ZoePlatform platform, String summonerName) {
    Summoner summoner = riotApi.getTFTAPI().getSummonerAPI().getSummonerByName(platform.getLeagueShard(), summonerName);

    if(summoner == null) {
      throw new APIResponseException(APIHTTPErrorReason.ERROR_404, "Not found");
    }
    
    SavedSummoner savedSummoner = new SavedSummoner(summoner, platform);

    insertOrReplaceTFTSummoner(savedSummoner, getSearchBsonForSummoner(platform, summoner.getSummonerId()));

    return savedSummoner;
  }

  public SavedSummoner getTFTSummonerBySummonerId(ZoePlatform platform, String summonerId) {

    SavedSummoner tftSummoner = tftSummonerCache.find(getSearchBsonForSummoner(platform, summonerId)).first();

    if(tftSummoner != null) {
      return tftSummoner;
    }

    Summoner tftSummonerOriginal = riotApi.getTFTAPI().getSummonerAPI().getSummonerById(platform.getLeagueShard(), summonerId);

    if(tftSummonerOriginal == null) {
      throw new APIResponseException(APIHTTPErrorReason.ERROR_404, "Not found");
    }
    
    tftSummoner = new SavedSummoner(tftSummonerOriginal, platform);

    insertOrReplaceTFTSummoner(tftSummoner, getSearchBsonForSummoner(platform, tftSummoner.getSummonerId()));

    return tftSummoner;
  }
  
  private void insertOrReplaceTFTSummoner(SavedSummoner savedSummoner, Bson searchBsonForSummoner) {
    SavedSummoner summonerToReplace = tftSummonerCache.find(searchBsonForSummoner).first();
    
    if(summonerToReplace == null) {
      tftSummonerCache.insertOne(savedSummoner);
    }else {
      tftSummonerCache.replaceOne(searchBsonForSummoner, savedSummoner);
    }
  }

  public SavedMatch getMatchById(ZoePlatform platform, String matchId) {

    Bson matchWanted = Projections.fields(Projections.computed("gameId", matchId), Projections.excludeId());

    SavedMatch matchDB = matchCache.find(matchWanted).first();

    if(matchDB != null) {
      return matchDB;
    }

    LOLMatch matchOriginal = new MatchBuilder().withPlatform(platform.getLeagueShard().toRegionShard()).withId(matchId).getMatch();

    if(matchOriginal == null) {
      throw new APIResponseException(APIHTTPErrorReason.ERROR_404, "Not found");
    }
    
    matchDB = new SavedMatch(matchOriginal, matchId, platform);
    
    insertOrReplaceMatch(matchDB, matchWanted);

    return matchDB;
  }

  private void insertOrReplaceMatch(SavedMatch newMatch, Bson matchToChange) {
    SavedMatch matchToReplace = matchCache.find(matchToChange).first();
    
    if(matchToReplace == null) {
      matchCache.insertOne(newMatch);
    }else {
      matchCache.replaceOne(matchToChange, newMatch);
    }
  }

  public List<String> getMatchListBySummonerId(ZoePlatform platform, String puuid){
    MatchListBuilder builder = new MatchListBuilder();

    builder.withPlatform(platform.getLeagueShard());
    builder.withPuuid(puuid);

    return builder.get();
  }

  public List<String> getMatchListByPuuid(ZoePlatform platform, String puuid, int count){
    MatchListBuilder builder = new MatchListBuilder();

    builder.withPlatform(platform.getLeagueShard());
    builder.withPuuid(puuid);
    builder.withCount(count);

    return builder.get();
  }

  public List<String> getMatchListByPuuid(ZoePlatform platform, String puuid, GameQueueType queueWanted, int beginIndex) {
    MatchListBuilder builder = new MatchListBuilder();

    return builder.withPlatform(platform.getLeagueShard()).withPuuid(puuid).withQueue(queueWanted).withBeginIndex(beginIndex).withCount(100).get();
  }

  public List<LeagueEntry> getLeagueEntryBySummonerId(ZoePlatform platform, String summonerId) {
    return new LeagueBuilder().withPlatform(platform.getLeagueShard()).withSummonerId(summonerId).getLeagueEntries();
  }

  public List<TFTLeagueEntry> getTFTLeagueEntryByTFTSummonerId(ZoePlatform platform, String summonerId) {
    return riotApi.getTFTAPI().getLeagueAPI().getLeagueEntries(platform.getLeagueShard(), summonerId);
  }
  
  public List<LeagueEntry> getTFTLeagueEntryConvertedByTFTSummonerId(ZoePlatform platform, String summonerId) {
    List<TFTLeagueEntry> leagueEntryToConvert = getTFTLeagueEntryByTFTSummonerId(platform, summonerId);
    
    List<LeagueEntry> leagueEntryConverted = new ArrayList<>();
    for(TFTLeagueEntry entry : leagueEntryToConvert) {
      leagueEntryConverted.add((LeagueEntry) entry);
    }
    
    return leagueEntryConverted;
  }

  public List<SavedSimpleMastery> getChampionMasteryBySummonerId(ZoePlatform platform, String summonerId) {

    Bson masteryWanted = Projections.fields(Projections.computed("summonerId", summonerId), Projections.computed("platform", platform.getShowableName()));

    List<SavedSimpleMastery> championMastery = new ArrayList<>();

    MongoCursor<SavedSimpleMastery> cursor = championsMasteryCache.find(masteryWanted).iterator();

    while(cursor.hasNext()) {
      championMastery.add(cursor.next());
    }

    if(!championMastery.isEmpty()) {
      return championMastery;
    }

    List<ChampionMastery> championMasteryOriginal = new ChampionMasteryBuilder().withPlatform(platform.getLeagueShard()).withSummonerId(summonerId).getChampionMasteries();
    
    for(ChampionMastery masteryOrignial : championMasteryOriginal) {
      championMastery.add(new SavedSimpleMastery(masteryOrignial, summonerId, platform));
    }
    
    insertOrReplaceChampionMastery(championMastery, masteryWanted);

    return championMastery;
  }

  private void insertOrReplaceChampionMastery(List<SavedSimpleMastery> championMasteryToUpdate, Bson masteryWanted) {
    List<SavedSimpleMastery> championMasteryCache = new ArrayList<>();

    MongoCursor<SavedSimpleMastery> cursor = championsMasteryCache.find(masteryWanted).iterator();

    while(cursor.hasNext()) {
      championMasteryCache.add(cursor.next());
    }
    
    for(SavedSimpleMastery championMastery : championMasteryToUpdate) {
      boolean championFound = false;
      for(SavedSimpleMastery masteryToCheck : championMasteryCache) {
        if(championMastery.getChampionId() == masteryToCheck.getChampionId()) {
          championFound = true;
          break;
        }
      }
      
      if(championFound) {
        championsMasteryCache.replaceOne(Projections.fields(Projections.computed("summonerId", championMastery.getSummonerId()),
            Projections.computed("platform", championMastery.getPlatform().getShowableName()), Projections.computed("championId", championMastery.getChampionId())), championMastery);
      }else {
        championsMasteryCache.insertOne(championMastery);
      }
    }
  }

  public String getThirdPartyCode(ZoePlatform platform, String summonerId) {
    return new ThirdPartyCodeBuilder().withPlatform(platform.getLeagueShard()).withSummonerId(summonerId).getCode();
  }

  public List<ClashPlayer> getClashPlayerBySummonerId(ZoePlatform platform, String summonerId){
    return riotApi.getLoLAPI().getClashAPI().getPlayerInfo(platform.getLeagueShard(), summonerId);
  }

  public List<SavedClashTournament> getTournaments(ZoePlatform selectedPlatform) {

    Bson tournamentWanted = Projections.fields(Projections.computed("server", selectedPlatform.getShowableName()));

    Iterator<SavedClashTournament> iterator = clashTournamentCache.find(tournamentWanted).iterator();

    List<SavedClashTournament> tournamentsToReturn = new ArrayList<>();

    while(iterator.hasNext()) {
      tournamentsToReturn.add(iterator.next());
    }

    if(!tournamentsToReturn.isEmpty()) {
      return tournamentsToReturn;
    }

    List<ClashTournament> clashTournaments = riotApi.getLoLAPI().getClashAPI().getTournaments(selectedPlatform.getLeagueShard());

    for(ClashTournament tournamentToRegister : clashTournaments) {
      SavedClashTournament tournamentDb = new SavedClashTournament(tournamentToRegister, selectedPlatform);

      clashTournamentCache.insertOne(tournamentDb);
      
      tournamentsToReturn.add(tournamentDb);
    }
    
    return tournamentsToReturn;
  }

  public ClashTeam getClashTeamById(ZoePlatform platform, String teamId) {
    return riotApi.getLoLAPI().getClashAPI().getTeam(platform.getLeagueShard(), teamId);
  }

  public SpectatorGameInfo getSpectatorGameInfo(ZoePlatform platform, String summonerId) {
    return riotApi.getLoLAPI().getSpectatorAPI().getCurrentGame(platform.getLeagueShard(), summonerId);
  }

  public SavedClashTournament getTournamentById(ZoePlatform platform, int tournamentId) {

    Bson tournamentWanted = Projections.fields(Projections.computed("serverName", platform.getShowableName()), Projections.excludeId());

    Iterator<SavedClashTournament> iterator = clashTournamentCache.find(tournamentWanted).iterator();

    SavedClashTournament tournamentToReturn = null;

    while(iterator.hasNext()) {
      SavedClashTournament tournamentToCheck = iterator.next();

      if(tournamentToCheck.getTournamentId() == tournamentId) {
        tournamentToReturn = tournamentToCheck;
        break;
      }
    }

    if(tournamentToReturn != null) {
      return tournamentToReturn;
    }

    List<ClashTournament> clashTournaments = riotApi.getLoLAPI().getClashAPI().getTournaments(platform.getLeagueShard());

    for(ClashTournament tournamentToRegister : clashTournaments) {
      SavedClashTournament tournamentDb = new SavedClashTournament(tournamentToRegister, platform);

      clashTournamentCache.replaceOne(tournamentWanted, tournamentDb);

      if(tournamentDb.getTournamentId() == tournamentId) {
        tournamentToReturn = tournamentDb;
      }
    }

    return tournamentToReturn;
  }

  public List<String> getTFTMatchList(ZoePlatform platform, String tftPuuid, int nbrOfGames) {
    return riotApi.getTFTAPI().getMatchAPI().getMatchList(platform.getLeagueShard().toRegionShard(), tftPuuid, nbrOfGames);
  }

  public TFTMatch getTFTMatch(ZoePlatform platform, String matchId) {
    return riotApi.getTFTAPI().getMatchAPI().getMatch(platform.getLeagueShard().toRegionShard(), matchId);
  }

  private Bson getSearchBsonForSummoner(ZoePlatform platform, String summonerId) {
    return Projections.fields(Projections.computed("platform", platform.getShowableName()), Projections.computed("summonerId", summonerId));
  }

}
