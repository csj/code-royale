package animationModule;

import com.codingame.gameengine.core.AbstractPlayer;
import com.codingame.gameengine.core.GameManager;
import com.codingame.gameengine.core.Module;
import com.codingame.gameengine.module.entities.Entity;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimationModule implements Module {

    GameManager<AbstractPlayer> gameManager;
    List<AnimationEvent> animationEvents;

    @Inject
    AnimationModule(GameManager<AbstractPlayer> gameManager) {
        this.gameManager = gameManager;
        gameManager.registerModule(this);
        animationEvents = new ArrayList<>();
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
        gameManager.setViewData("animations", animationEvents);
        animationEvents.clear();
    }

    public void triggerAnimation(Entity<?> entity, String animationType) {
        int id = entity.getId();
        animationEvents.add(new AnimationEvent(id, animationType));
    }
}
