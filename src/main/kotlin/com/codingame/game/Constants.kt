package com.codingame.game

object Constants {
  val QUEEN_SPEED = 60
  val TOWER_HP_INITIAL = 200
  val TOWER_HP_INCREMENT = 100
  val TOWER_HP_MAXIMUM = 800
  val TOWER_CREEP_DAMAGE_MAX = 10
  val TOWER_CREEP_DAMAGE_DROP_DISTANCE = 50
  val TOWER_QUEEN_DAMAGE = 1
  val TOWER_MELT_RATE = 4
  val TOWER_COVERAGE_PER_HP = 1000

  val GIANT_BUST_RATE = 80

  val OBSTACLE_GAP = 60
  val OBSTACLE_RADIUS_RANGE = 60..110
  val OBSTACLE_MINERAL_RANGE = 200..250
  val OBSTACLE_MINERAL_BASERATE_RANGE = 1..3
  val OBSTACLE_MINERAL_INCREASE = 50
  val OBSTACLE_MINERAL_INCREASE_DISTANCE_1 = 500
  val OBSTACLE_MINERAL_INCREASE_DISTANCE_2 = 200
  val OBSTACLE_PAIRS = 13

  val CREEP_DAMAGE = 1

  val QUEEN_RADIUS = 20
  val QUEEN_MASS = 10000
  val QUEEN_HP = 200
  val QUEEN_VISION = 300

  val WORLD_WIDTH = viewportX.last - viewportX.first
  val WORLD_HEIGHT = viewportY.last - viewportY.first

  val TOUCHING_DELTA = 5
}

enum class CreepType(val count: Int, val cost: Int, val speed: Int, val range: Int, val radius: Int,
                     val mass: Int, val hp: Int, val buildTime: Int, val assetName: String, val fillAssetName: String) {
  MELEE(   4, 80,  100, 0,   10, 400,  30,  5, "bug.png",  "bug-fill.png"),
  RANGED(  2, 120, 75 , 200, 15, 900,  45,  8, "archer.png", "archer-fill.png"),
  GIANT(   1, 140, 50 , 0,   25, 2000, 200, 10, "bulldozer.png", "bulldozer-fill.png")
}