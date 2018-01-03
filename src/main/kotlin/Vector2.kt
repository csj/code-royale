package com.codingame.game

import java.lang.IllegalArgumentException
import java.lang.Math.cos
import java.lang.Math.sin

data class Vector2(val x: Double, val y: Double) {
  private val lengthSquared by lazy { x*x + y*y }
  val length by lazy { Math.sqrt(lengthSquared) }
  val isNearZero by lazy { Math.abs(x) < 1e-12 && Math.abs(y) < 1e-12 }
  val normalized: Vector2 by lazy {
    val len = length
    when(len) {
      0.0 -> {
        val ang = Math.random() * 2 * Math.PI
        Vector2(cos(ang), sin(ang))
      }
      else -> Vector2(x / len, y / len)
    }
  }
  constructor(x: Int, y: Int): this(x.toDouble(), y.toDouble())

  operator fun minus(other: Vector2) = Vector2(x - other.x, y - other.y)
  operator fun plus(other: Vector2) = Vector2(x + other.x, y + other.y)
  operator fun times(other: Double) = Vector2(x * other, y * other)
  operator fun div(other: Double) = when(other) {
    0.0 -> throw IllegalArgumentException("Division by zero")
    else -> Vector2(x / other, y / other)
  }
  fun resizedTo(newLength: Double) = normalized * newLength
  fun distanceTo(other: Vector2) = (this - other).length
  fun dot(other: Vector2) = x * other.x + y * other.y
  fun compareDirection(other: Vector2) = normalized.dot(other.normalized)   /* 1 == same direction, -1 == opposite direction */
  fun projectInDirectionOf(other: Vector2) = when {
    other.isNearZero -> throw IllegalArgumentException("cannot project in direction of zero")
    else -> other * (this.dot(other) / other.dot(other))
  }
  fun rejectInDirectionOf(other: Vector2) = this - projectInDirectionOf(other)
  fun towards(other: Vector2, maxDistance: Double) =
    if (distanceTo(other) < maxDistance) other
    else this + (other - this).resizedTo(maxDistance)
  fun clampWithin(minX: Double, maxX: Double, minY: Double, maxY: Double): Vector2 {
    val nx = when { x < minX -> minX; x > maxX -> maxX; else -> x }
    val ny = when { y < minY -> minY; y > maxY -> maxY; else -> y }
    return Vector2(nx, ny)
  }
  override fun toString(): String = "(${Math.round(x)}, ${Math.round(y)})"

  companion object {
    val Zero = Vector2(0, 0)
    fun random(maxX: Int, maxY: Int) = Vector2(Math.random() * maxX, Math.random() * maxY)
  }
}
