package ch.kalunight.zoe.riotapi;

import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Projections;

import ch.kalunight.zoe.model.dto.ZoePlatform;
import no.stelar7.api.r4j.impl.R4J;
import no.stelar7.api.r4j.impl.lol.builders.matchv5.match.MatchBuilder;
import no.stelar7.api.r4j.impl.lol.builders.matchv5.match.MatchListBuilder;
import no.stelar7.api.r4j.pojo.lol.match.v5.LOLMatch;
import no.stelar7.api.r4j.pojo.lol.summoner.Summoner;

public class CachedRiotApi {

  private R4J riotApi; 

  private MongoClient client;

  private MongoDatabase cacheDatabase;

  public CachedRiotApi(R4J r4j, String connectString, String dbName) {
    this.riotApi = r4j;
    client = new MongoClient(new MongoClientURI(connectString));
    cacheDatabase = client.getDatabase(dbName);
  }

  public Summoner getSummonerByName(ZoePlatform platform, String summonerName) {
    return riotApi.getLoLAPI().getSummonerAPI().getSummonerByName(platform.getLeagueShard(), summonerName);
  }

  public Summoner getSummonerBySummonerId(ZoePlatform platform, String summonerId) {
    return riotApi.getLoLAPI().getSummonerAPI().getSummonerById(platform.getLeagueShard(), summonerId);
  }

  public LOLMatch getMatchById(ZoePlatform platform, String matchId) {

    MongoCollection<Document> collection = cacheDatabase.getCollection("MATCH-V5");
    
    Bson matchWanted = Projections.computed("id", matchId);

    Document matchDB = collection.find(matchWanted).first();
    
    if(matchDB != null) {
      return matchDB.get(null, LOLMatch.class);
    }
    
    LOLMatch match = new MatchBuilder().withPlatform(platform.getLeagueShard().toRegionShard()).withId(matchId).getMatch();
    
    collection.insertOne(matchDB);
    
    return match;
  }
  
  public List<String> getMatchListBySummonerId(ZoePlatform platform, String puuid){
    MatchListBuilder builder = new MatchListBuilder();
    
    builder.withPlatform(platform.getLeagueShard());
    builder.withPuuid(puuid);
    
    
    return builder.get();
  }

  

}
