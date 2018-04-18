package anims;

import java.util.ArrayList;
import java.util.List;

import com.codingame.gameengine.core.AbstractPlayer;
import com.codingame.gameengine.core.GameManager;
import com.codingame.gameengine.core.Module;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.google.inject.Inject;

public class AnimModule implements Module {

    GameManager<AbstractPlayer> gameManager;
    @Inject GraphicEntityModule entityModule;
    List<Anim> animEvents;

    @Inject
    AnimModule(GameManager<AbstractPlayer> gameManager) {
        this.gameManager = gameManager;
        animEvents = new ArrayList<>();
        gameManager.registerModule(this);
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

    public Anim createAnimationEvent(String id, double t) {
        Anim animEvent = new Anim(id, t);
        animEvents.add(animEvent);
        return animEvent;
    }

    private void sendFrameData() {
        gameManager.setViewData("anims", animEvents);
        animEvents.clear();
    }
}
