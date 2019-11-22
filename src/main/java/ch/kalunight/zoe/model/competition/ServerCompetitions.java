package ch.kalunight.zoe.model.competition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerCompetitions {
  
  List<Competition> listsCompetitions = Collections.synchronizedList(new ArrayList<>());
  
  public ServerCompetitions() {
    listsCompetitions = new ArrayList<>();
  }
  
  
}
