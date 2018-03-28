package com.codingame.game

class PlayerHUD(private val player: Player, isSecondPlayer: Boolean) {
  private val left = if (isSecondPlayer) 1920/2 else 0
  private val right = if (isSecondPlayer) 1920 else 1920/2
  private val top = viewportY.last
  private val bottom = 1080

  private val healthBarWidth = 406

  private val avatar = theEntityManager.createSprite()
    .setImage(player.avatarToken)
    .setY(bottom).setAnchorY(1.0)
    .apply { if (isSecondPlayer) { x = 1920; anchorX = 1.0 } else { x = 0 } }
    .setBaseWidth(140)
    .setBaseHeight(140)
    .setZIndex(4003)!!

  init {
//    avatar.setMask(theEntityManager.createCircle().setRadius(70).setX( if (isSecondPlayer) 1920-70 else 70).setY(bottom - 70))
  }

  private val healthBarFill = theEntityManager.createRectangle()!!
    .setLineAlpha(0.0)
    .setY(top + 40)
    .setX(if (isSecondPlayer) 1920 - 150 - healthBarWidth else 150)
    .setWidth(healthBarWidth).setHeight(28)
    .setFillColor(player.colorToken)
    .setLineWidth(0)
    .setZIndex(4002)

  private val playerName = theEntityManager.createText(player.nicknameToken)!!
    .setY(bottom - 45).setAnchorY(1.0)
    .apply { if (isSecondPlayer) { x = 1770; anchorX = 1.0 } else { x = 150 }}
    .setFillColor(0xffffff)
    .setScale(1.8)
    .setFontFamily("Arial Black")
    .setZIndex(4003)

  private val healthText = theEntityManager.createText(player.health.toString())!!
    .setX(healthBarFill.x + healthBarFill.width - 10).setY(healthBarFill.y + healthBarFill.height/2)
    .setAnchorX(1.0).setAnchorY(0.5)
    .setScale(1.3)
    .setFontFamily("Arial Black")
    .setFillColor(0xffffff)
    .setZIndex(4003)

  private val moneyText = theEntityManager.createText("0")
    .setY(bottom - 15).setAnchorY(1.0)
    .setX(if (isSecondPlayer) 1020 else 700)
    .setFillColor(0xffffff)
    .setScale(1.8)
    .setFontFamily("Arial Black")
    .setZIndex(4002)!!

  private val moneyIncText = theEntityManager.createText("")
    .setY(bottom - 20).setAnchorY(1.0)
    .setX(if (isSecondPlayer) 1225 else 915)
    .setAnchorX(1.0)
    .setFillColor(0xffffff)
    .setScale(1.5)
    .setFontFamily("Arial Black")
    .setZIndex(4002)!!

  fun update() {
    healthBarFill.width = healthBarWidth * player.health / Constants.QUEEN_HP
    healthText.text = player.health.toString()
    moneyText.text = player.resources.toString()
    moneyIncText.text = when (player.resourcesPerTurn) {
      0 -> ""
      else -> "(+${player.resourcesPerTurn})"
    }
    theEntityManager.commitEntityState(0.0, moneyText, moneyIncText)
  }

  companion object {
    lateinit var obstacles: List<Obstacle>
  }
}