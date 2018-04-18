package com.codingame.game

import com.codingame.game.Constants.OBSTACLE_PAIRS

object Constants {
  const val STARTING_GOLD = 100

  const val QUEEN_SPEED = 60
  const val TOWER_HP_INITIAL = 200
  const val TOWER_HP_INCREMENT = 100
  const val TOWER_HP_MAXIMUM = 800
  const val TOWER_CREEP_DAMAGE_MIN = 3
  const val TOWER_CREEP_DAMAGE_CLIMB_DISTANCE = 200
  const val TOWER_QUEEN_DAMAGE_MIN = 1
  const val TOWER_QUEEN_DAMAGE_CLIMB_DISTANCE = 200
  const val TOWER_MELT_RATE = 4
  const val TOWER_COVERAGE_PER_HP = 1000

  const val GIANT_BUST_RATE = 80

  const val OBSTACLE_GAP = 90
  val OBSTACLE_RADIUS_RANGE = 60..90
  val OBSTACLE_GOLD_RANGE = 200..250
  val OBSTACLE_MINE_BASESIZE_RANGE = 1..3
  const val OBSTACLE_GOLD_INCREASE = 50
  const val OBSTACLE_GOLD_INCREASE_DISTANCE_1 = 500
  const val OBSTACLE_GOLD_INCREASE_DISTANCE_2 = 200
  val OBSTACLE_PAIRS = 6..12

  const val KNIGHT_DAMAGE = 1
  const val ARCHER_DAMAGE = 2
  const val ARCHER_DAMAGE_TO_GIANTS = 10

  const val QUEEN_RADIUS = 30
  const val QUEEN_MASS = 10000
  const val QUEEN_HP = 200
  const val QUEEN_VISION = 300

  val WORLD_WIDTH = viewportX.last - viewportX.first
  val WORLD_HEIGHT = viewportY.last - viewportY.first

  const val TOUCHING_DELTA = 5
  const val WOOD_FIXED_INCOME = 10
}

object Leagues {
  var towers = true
  var giants = true
  var mines = true
  var fixedIncome:Int? = null
  var obstacles:Int = OBSTACLE_PAIRS.last
}

enum class CreepType(val count: Int, val cost: Int, val speed: Int, val range: Int, val radius: Int,
                     val mass: Int, val hp: Int, val buildTime: Int, val assetName: String) {
  KNIGHT(   4, 80,  100, 0,   20, 400,  30,  5, "Unite_Fantassin.png"),
  ARCHER(  2, 100, 75 , 200, 25, 900,  45,  8, "Unite_Archer.png"),
  GIANT(   1, 140, 50 , 0,   40, 2000, 200, 10, "Unite_Siege.png")
}