package animationModule;

import java.util.ArrayList;
import java.util.List;

import com.codingame.gameengine.core.AbstractPlayer;
import com.codingame.gameengine.core.GameManager;
import com.codingame.gameengine.core.Module;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.google.inject.Inject;

public class AnimatedEventModule implements Module {

    GameManager<AbstractPlayer> gameManager;
    @Inject GraphicEntityModule entityModule;
    List<ViewerEvent> animEvents;

    @Inject
    AnimatedEventModule(GameManager<AbstractPlayer> gameManager) {
        this.gameManager = gameManager;
        animEvents = new ArrayList<>();
        gameManager.registerModule(this);
    }

    @Override
    public void onGameInit() {
        gameManager.setViewGlobalData("animations", entityModule.getWorld());
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

    public ViewerEvent createAnimationEvent(String id, double t) {
        ViewerEvent animEvent = new ViewerEvent(id, t);
        animEvents.add(animEvent);
        return animEvent;
    }

    private void sendFrameData() {
        gameManager.setViewData("animations", animEvents);
        animEvents.clear();
    }
}
