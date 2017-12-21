import com.codingame.gameengine.runner.GameRunner;

public class Main {
    public static void main(String[] args) {

        GameRunner gameRunner = new GameRunner();

        // Adds as many player as you need to test your game
        gameRunner.addJavaPlayer(TestPlayer.class);
        gameRunner.addJavaPlayer(TestPlayer.class);

        // gameRunner.addCommandLinePlayer("python3 /home/user/player.py");

        gameRunner.start();
    }
}
