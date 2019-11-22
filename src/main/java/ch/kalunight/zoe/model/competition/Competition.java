package ch.kalunight.zoe.model.competition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Competition {

  private String name;
  private Objective objective;
  private List<ObjectivesFilters> objectivesFilter;

  public Competition(String name) {
    this.name = name;
    this.objectivesFilter = Collections.synchronizedList(new ArrayList<>());
  }

  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public Objective getObjective() {
    return objective;
  }
  
  public void setObjective(Objective objective) {
    this.objective = objective;
  }
  
  public List<ObjectivesFilters> getCompetition() {
    return objectivesFilter;
  }
  
  public void setCompetition(List<ObjectivesFilters> objectivesFilter) {
    this.objectivesFilter = objectivesFilter;
  }
  
}