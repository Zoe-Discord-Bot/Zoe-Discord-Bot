package ch.kalunight.zoe.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.kalunight.zoe.model.dto.SavedMatch;

public class MatchReceiver {
  public final List<SavedMatch> matchs = Collections.synchronizedList(new ArrayList<>());
}
