import java.util.*;
import java.io.*;
import java.math.*;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
public class WaitBot {

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int numObstacles = in.nextInt();
        for (int i = 0; i < numObstacles; i++) {
            int obstacleId = in.nextInt();
            int x = in.nextInt();
            int y = in.nextInt();
            int radius = in.nextInt();
        }

        // game loop
        while (true) {
            int queenX = in.nextInt();
            int queenY = in.nextInt();
            int health = in.nextInt();
            int gold = in.nextInt();
            int enemyQueenX = in.nextInt();
            int enemyQueenY = in.nextInt();
            int enemyHealth = in.nextInt();
            for (int i = 0; i < numObstacles; i++) {
                int obstacleId = in.nextInt();
                int goldRemaining = in.nextInt(); // -1 if unknown
                int maxMineSize = in.nextInt(); // -1 if unknown
                int structureType = in.nextInt(); // -1 = No structure, 0 = Resource Mine, 1 = Tower, 2 = Barracks
                int owner = in.nextInt(); // -1 = No structure, 0 = Friendly, 1 = Enemy
                int param1 = in.nextInt();
                int param2 = in.nextInt();
            }
            int numMyCreeps = in.nextInt();
            for (int i = 0; i < numMyCreeps; i++) {
                int x = in.nextInt();
                int y = in.nextInt();
                int creepHealth = in.nextInt();
                int creepType = in.nextInt(); // 0 = MELEE, 1 = RANGED, 2 = GIANT
            }
            int numEnemyCreeps = in.nextInt();
            for (int i = 0; i < numEnemyCreeps; i++) {
                int x = in.nextInt();
                int y = in.nextInt();
                int creepHealth = in.nextInt();
                int creepType = in.nextInt();
            }

            // Write an action using System.out.println()
            // To debug: System.err.println("Debug messages...");


            // First line: A valid queen action
            // Second line: A set of training instructions
            System.out.println("WAIT");
            System.out.println("TRAIN");
        }
    }
}