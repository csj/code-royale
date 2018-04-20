import java.util.*;
import java.io.*;
import java.math.*;

/*
Default AI
Strategy: Build a single barracks and train Knight minions whenever possible.
    If income is less than ten, build or upgrade a mine on the closest obstacle to the barracks.
    Otherwise, build a tower on the empty obstacle closest to the barracks.
 */
class Player {

  public static void main(String args[]) {
    Scanner in = new Scanner(System.in);
    int numObstacles = in.nextInt();
    Obst[] obstacles = new Obst[numObstacles];
    for (int i = 0; i < numObstacles; i++) {
      int obstacleId = in.nextInt();
      int x = in.nextInt();
      int y = in.nextInt();
      int radius = in.nextInt();
      obstacles[obstacleId] = new Obst(obstacleId, x, y);
    }

    Obst barracks = null;
    int myQueenX = -1, myQueenY = -1;

    // game loop
    while (true) {
      int gold = in.nextInt();
      int touching = in.nextInt();
      int income = 0;

      for (int i = 0; i < numObstacles; i++) {
        int obstacleId = in.nextInt();
        int goldRemaining = in.nextInt(); // -1 if unknown
        int maxMineSize = in.nextInt(); // -1 if unknown
        int structureType = in.nextInt(); // -1 = No structure, 0 = Resource Mine, 1 = Tower, 2 = Barracks
        int owner = in.nextInt(); // -1 = No structure, 0 = Friendly, 1 = Enemy
        int param1 = in.nextInt();
        int param2 = in.nextInt();

        obstacles[obstacleId].type = structureType;
        obstacles[obstacleId].maxSize = maxMineSize;
        obstacles[obstacleId].remainingGold = goldRemaining;
        obstacles[obstacleId].param1 = param1;
        obstacles[obstacleId].owner = owner;

        if (structureType == 0 && owner == 0){
          income += param1;
        }
      }
      int numUnits = in.nextInt();
      for (int i = 0; i < numUnits; i++) {
        int x = in.nextInt();
        int y = in.nextInt();
        int owner = in.nextInt();
        int type = in.nextInt();
        int health = in.nextInt();

        if (type == -1 && owner == 0){
          myQueenX = x;
          myQueenY = y;
        }
      }

      if(barracks == null){
        double minDist = Double.MAX_VALUE;
        for (Obst o: obstacles) {
          double dist = distance(myQueenX, o.x, myQueenY, o.y);
          if(dist < minDist){
            minDist = dist;
            barracks = o;
          }
        }
      }

      String action = "WAIT";
      if (barracks.type == -1){
        action = String.format("BUILD %d BARRACKS-KNIGHT", barracks.id);
      } else if (income < 10){
        double minDist = Double.MAX_VALUE;
        Obst target = null;
        for (Obst o: obstacles) {
          double dist = distance(barracks.x, o.x, barracks.y, o.y);
          if(dist < minDist && o.remainingGold > 0 && (o.type == -1 || (o.type == 0 && o.param1 < o.maxSize))){
            minDist = dist;
            target = o;
          }
        }
        if (target != null){
          action = String.format("BUILD %d MINE", target.id);
        }
      } else {
        double minDist = Double.MAX_VALUE;
        Obst target = null;
        for (Obst o: obstacles) {
          double dist = distance(barracks.x, o.x, barracks.y, o.y);
          if(dist < minDist && o.type == -1){
            minDist = dist;
            target = o;
          }
        }
        if (target != null){
          action = String.format("BUILD %d TOWER", target.id);
        }
      }

      String train = "TRAIN";
      if (gold >= 80){
        if (barracks.type == 2 && barracks.owner == 0){
          train = train + " " + barracks.id;
        }
      }

      // First line: A valid queen action
      // Second line: A set of training instructions
      System.out.println(action);
      System.out.println(train);
    }
  }

  static double distance(int x1, int x2, int y1, int y2){
    return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
  }
}

class Obst{
  int id, x, y;
  int type = -1;
  int owner;
  int maxSize;
  int remainingGold;
  int param1;

  public Obst(int id, int x, int y){
    this.id = id;
    this.x = x;
    this.y = y;
  }
}