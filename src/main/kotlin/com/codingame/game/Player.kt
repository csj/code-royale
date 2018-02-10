package com.codingame.game

import com.codingame.game.Constants.KING_HP
import com.codingame.gameengine.core.AbstractPlayer

class Player : AbstractPlayer() {
  override fun getExpectedOutputLines(): Int = 2
  lateinit var kingUnit: King
  lateinit var enemyPlayer: Player
  var isSecondPlayer: Boolean = false

  private fun fixOwner(player: Player?) = when (player) { null -> -1; this -> 0; else -> 1 }

  fun printObstacleInit(obstacle: Obstacle) {
    val (x,y) = obstacle.location
    val toks = listOf(obstacle.obstacleId, x.toInt(), y.toInt(), obstacle.radius, obstacle.minerals)
    sendInputLine(toks.joinToString(" "))
  }

  fun printObstaclePerTurn(obstacle: Obstacle) {
    val struc = obstacle.structure
    val toks = listOf(obstacle.obstacleId, obstacle.minerals) + when (struc) {
      is Mine -> listOf(0, fixOwner(struc.owner), struc.incomeRate, -1)
      is Tower -> listOf(1, fixOwner(struc.owner), struc.health, struc.attackRadius)
      is Barracks -> listOf(2, fixOwner(struc.owner), struc.progress, struc.creepType.ordinal)
      else -> listOf(-1, -1, -1, -1)
    }
    sendInputLine(toks.joinToString(" "))
  }

  val activeCreeps = mutableListOf<Creep>()

  fun allUnits() = activeCreeps + kingUnit

  var health by nonNegative(KING_HP)
  var resources = 0
  var resourcesPerTurn = 0

  fun checkKingHealth() {
    kingUnit.setHealth(health)
    if (health == 0) deactivate("Dead king")
    hud.update()
  }

  val hud by lazy { PlayerHUD(this, isSecondPlayer) }
}