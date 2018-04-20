import java.util.*;

/*
Level 1 Boss: Peon
Expected Player Skills: Build structures and train minions in an efficient manner
Strategy: Build a single Knight Barracks and train a wave of knight troops every 12 turns.
 */
class Player {

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int numObstacles = in.nextInt();
        ArrayList<Obst> obstacles = new ArrayList<Obst>();
        for (int i = 0; i < numObstacles; i++) {
            int obstacleId = in.nextInt();
            int x = in.nextInt();
            int y = in.nextInt();
            int radius = in.nextInt();
            obstacles.add(new Obst(obstacleId, x, y));
        }

        Obst barracks = null;
        int myQueenX = -1, myQueenY = -1;
        int count = 0;

        // game loop
        while (true) {
            int gold = in.nextInt();
            int touching = in.nextInt();
            for (int i = 0; i < numObstacles; i++) {
                int obstacleId = in.nextInt();
                int goldRemaining = in.nextInt(); // -1 if unknown
                int maxMineSize = in.nextInt(); // -1 if unknown
                int structureType = in.nextInt(); // -1 = No structure, 0 = Resource Mine, 1 = Tower, 2 = Barracks
                int owner = in.nextInt(); // -1 = No structure, 0 = Friendly, 1 = Enemy
                int param1 = in.nextInt();
                int param2 = in.nextInt();

                if (barracks != null && barracks.id == obstacleId){
                    barracks.type = structureType;
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
                    double dist = Math.sqrt(Math.pow(myQueenX - o.x, 2) + Math.pow(myQueenY - o.y, 2));
                    if(dist < minDist){
                        minDist = dist;
                        barracks = o;
                    }
                }
            }

            String action = "WAIT";
            if(barracks.type == -1){
                action = String.format("BUILD %d BARRACKS-KNIGHT", barracks.id);
            }

            count++;
            String train = "TRAIN";
            if (count == 12){
                count = 0;
                if (barracks.type == 2){
                    train = train + " " + barracks.id;
                }
            }

            // First line: A valid queen action
            // Second line: A set of training instructions
            System.out.println(action);
            System.out.println(train);
        }
    }
}

class Obst{
    int id, x, y;
    int type = -1;

    public Obst(int id, int x, int y){
        this.id = id;
        this.x = x;
        this.y = y;
    }
}