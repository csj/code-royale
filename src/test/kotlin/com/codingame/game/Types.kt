package com.codingame.game

fun<T> List<T>.sample() = this[theRandom.nextInt(size)]
data class CreepInput(
  val location: Vector2,
  val health: Int,
  val creepType: CreepType
)

data class ObstaclePerTurnInput(
  val obstacleId: Int,
  val minerals: Int,
  val maxResourceRate: Int,
  val structureType: Int,
  val owner: Int,
  val incomeRateOrHealthOrCooldown: Int,
  val attackRadiusOrCreepType: Int
)

data class ObstacleInput(
  val obstacleId: Int,
  val location: Vector2,
  val radius: Int,
  var minerals: Int = -1,
  var maxResourceRate: Int = -1,
  var structureType: Int = -1,                 // -1 = None, 0 = Mine, 1 = Tower, 2 = Barracks
  var owner: Int = -1,                         // 0 = Us, 1 = Enemy
  var incomeRateOrHealthOrCooldown: Int = -1,  // mine / tower / barracks
  var attackRadiusOrCreepType: Int = -1        // tower / barracks
) {
  fun applyUpdate(update: ObstaclePerTurnInput) {
    structureType = update.structureType
    minerals = update.minerals
    maxResourceRate = update.maxResourceRate
    owner = update.owner
    incomeRateOrHealthOrCooldown = update.incomeRateOrHealthOrCooldown
    attackRadiusOrCreepType = update.attackRadiusOrCreepType
  }
}

data class AllInputs(
  val queenLoc: Vector2,
  val health: Int,
  val resources: Int,
  val enemyQueenLoc: Vector2,
  val enemyHealth: Int,
  val obstacles: List<ObstacleInput>,
  val friendlyCreeps: List<CreepInput>,
  val enemyCreeps: List<CreepInput>
)