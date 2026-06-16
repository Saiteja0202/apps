package com.arenaclash.game.core

class RP(
    val x: Float, val z: Float, val angle: Float,
    val colorIndex: Int, val alive: Boolean, val isLocal: Boolean,
    val name: String, val hp: Float, val kills: Int, val moving: Boolean
)

class RenderSnapshot(
    val players: List<RP>,
    val bulletX: FloatArray,
    val bulletZ: FloatArray,
    val packX: FloatArray,
    val packZ: FloatArray,
    val zoneR: Float,
    val arenaHalf: Float,
    val phase: Int,
    val localHp: Float,
    val aliveCount: Int,
    val totalPlayers: Int,
    val winnerName: String?
)
