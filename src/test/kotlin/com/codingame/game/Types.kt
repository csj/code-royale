package com.codingame.game

data class UnitInput(
  val location: Vector2,
  val isFriendly: Boolean,
  val creepType: CreepType?,
  val health: Int
)

data class ObstaclePerTurnInput(
  val obstacleId: Int,
  val gold: Int,
  val maxMineSize: Int,
  val structureType: Int,
  val owner: Int,
  val incomeRateOrHealthOrCooldown: Int,
  val attackRadiusOrCreepType: Int
)

data class ObstacleInput(
  val obstacleId: Int,
  val location: Vector2,
  val radius: Int,
  var gold: Int = -1,
  var maxMineSize: Int = -1,
  var structureType: Int = -1,                 // -1 = None, 0 = Mine, 1 = Tower, 2 = Barracks
  var owner: Int = -1,                         // 0 = Us, 1 = Enemy
  var incomeRateOrHealthOrCooldown: Int = -1,  // mine / tower / barracks
  var attackRadiusOrCreepType: Int = -1        // tower / barracks
) {
  fun applyUpdate(update: ObstaclePerTurnInput) {
    structureType = update.structureType
    gold = update.gold
    maxMineSize = update.maxMineSize
    owner = update.owner
    incomeRateOrHealthOrCooldown = update.incomeRateOrHealthOrCooldown
    attackRadiusOrCreepType = update.attackRadiusOrCreepType
  }
}

data class AllInputs(
  val queenLoc: Vector2,
  val health: Int,
  val gold: Int,
  val enemyQueenLoc: Vector2,
  val enemyHealth: Int,
  val obstacles: List<ObstacleInput>,
  val friendlyCreeps: List<UnitInput>,
  val enemyCreeps: List<UnitInput>
)