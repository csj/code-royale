import java.util.Properties;

import com.codingame.gameengine.core.AbstractPlayer;
import com.codingame.gameengine.core.GameManager;
import com.codingame.gameengine.core.Referee;
import com.codingame.gameengine.module.entities.EntityManager;
import com.google.inject.Inject;

class SkeletonPlayer extends AbstractPlayer {

    @Override
    public int getExpectedOutputLines() {
        // Returns the number of expected lines of outputs for a player
        
        // TODO: Replace the returned value with a valid number. Most of the time the value is 1. 
        return -1;
    }
}

public class SkeletonReferee implements Referee {
    @Inject private GameManager<SkeletonPlayer> gameManager;
    @Inject private EntityManager entityManager;
    
    private int playerCount;

    @Override
    public Properties init(int playerCount, Properties params) {
        this.playerCount = playerCount;
        
        // Params contains all the game parameters that has been to generate this game
        // For instance, it can be a seed number, the size of a grid/map, ...  
        return params;
    }

    @Override
    public void gameTurn(int turn) {
        // Code your game logic.
        // See README.md if you want some code to bootstrap your project.
    }
}
