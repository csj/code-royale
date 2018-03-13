package tooltipModule;

import com.codingame.gameengine.core.AbstractPlayer;
import com.codingame.gameengine.core.GameManager;
import com.codingame.gameengine.core.Module;
import com.codingame.gameengine.module.entities.Entity;
import com.google.inject.Inject;

import java.util.HashMap;
import java.util.Map;

public class TooltipModule implements Module {

  GameManager<AbstractPlayer> gameManager;
  Map<Integer, Map<String, Object>> registrations;
  Map<Integer, Map<String, Object>> newRegistrations;
  Map<Integer, String[]> extra, newExtra;

  @Inject
  TooltipModule(GameManager<AbstractPlayer> gameManager) {
    this.gameManager = gameManager;
    gameManager.registerModule(this);
    registrations = new HashMap<>();
    newRegistrations = new HashMap<>();
    extra = new HashMap<>();
    newExtra = new HashMap<>();
  }

  @Override
  public void onGameInit() {
    sendFrameData();
  }

  @Override
  public void onAfterGameTurn() {
    sendFrameData();
  }

  @Override
  public void onAfterOnEnd() {
    sendFrameData();
  }

  private void sendFrameData() {
    Object[] data = { newRegistrations, newExtra };
    gameManager.setViewData("tooltips", data);
    newRegistrations.clear();
    newExtra.clear();
  }

  public void registerEntity(Entity<?> entity) {
    registerEntity(entity, new HashMap<>());
  }

  public void registerEntity(Entity<?> entity, Map<String, Object> params) {
    int id = entity.getId();
    if (!params.equals(registrations.get(id))) {
      newRegistrations.put(id, params);
      registrations.put(id, params);
    }
  }

  public void updateExtraTooltipText(Entity<?> entity, String... lines) {
    int id = entity.getId();
    if (!lines.equals(extra.get(id))) {
      newExtra.put(id, lines);
      extra.put(id, lines);
    }
  }
}
