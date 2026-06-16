package com.arenaclash.game.net

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

const val GAME_PORT = 52525
private val JSON = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@Serializable class InputDTO(val mx: Float = 0f, val mz: Float = 0f, val ax: Float = 0f, val az: Float = 0f, val fire: Boolean = false)
@Serializable class PlayerDTO(val id: Int, val name: String, val x: Float, val z: Float, val angle: Float, val hp: Float, val alive: Boolean, val color: Int, val kills: Int, val bot: Boolean, val moving: Boolean = false)
@Serializable class SnapshotDTO(val players: List<PlayerDTO>, val bx: List<Float>, val bz: List<Float>, val px: List<Float> = emptyList(), val pz: List<Float> = emptyList(), val zoneR: Float, val arenaHalf: Float, val phase: Int, val winner: String = "")
@Serializable class NetMessage(val type: String, val name: String = "", val id: Int = -1, val input: InputDTO? = null, val snap: SnapshotDTO? = null)

object IpUtil {
    fun localIp(context: Context): String {
        // Prefer Wi-Fi IP
        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ip = wm?.connectionInfo?.ipAddress ?: 0
            if (ip != 0) return String.format("%d.%d.%d.%d", ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff)
        } catch (_: Exception) {}
        // Fallback: first site-local IPv4
        try {
            for (ni in NetworkInterface.getNetworkInterfaces()) {
                for (addr in ni.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is InetAddress && addr.hostAddress?.contains('.') == true && addr.isSiteLocalAddress) {
                        return addr.hostAddress!!
                    }
                }
            }
        } catch (_: Exception) {}
        return "0.0.0.0"
    }
}

private class Conn(val id: Int, val socket: Socket) {
    private val out: OutputStream = socket.getOutputStream()
    fun send(line: String) { try { out.write(line.toByteArray()); out.flush() } catch (_: Exception) {} }
    fun close() { try { socket.close() } catch (_: Exception) {} }
}

class HostServer(private val port: Int = GAME_PORT) {
    private var server: ServerSocket? = null
    private val clients = CopyOnWriteArrayList<Conn>()
    @Volatile var running = false
    private var nextId = 1

    var onJoin: ((Int, String) -> Unit)? = null
    var onLeave: ((Int) -> Unit)? = null
    var onInput: ((Int, InputDTO) -> Unit)? = null

    fun start() {
        running = true
        thread(name = "host-accept") {
            try {
                server = ServerSocket(port)
                while (running) {
                    val sock = server!!.accept()
                    val id = nextId++
                    val conn = Conn(id, sock)
                    clients.add(conn)
                    listen(conn)
                }
            } catch (_: Exception) {}
        }
    }

    private fun listen(conn: Conn) {
        thread(name = "host-client-${conn.id}") {
            try {
                val br = BufferedReader(InputStreamReader(conn.socket.getInputStream()))
                while (running) {
                    val line = br.readLine() ?: break
                    val msg = runCatching { JSON.decodeFromString(NetMessage.serializer(), line) }.getOrNull() ?: continue
                    when (msg.type) {
                        "join" -> { onJoin?.invoke(conn.id, msg.name); conn.send(JSON.encodeToString(NetMessage.serializer(), NetMessage("welcome", id = conn.id)) + "\n") }
                        "input" -> msg.input?.let { onInput?.invoke(conn.id, it) }
                    }
                }
            } catch (_: Exception) {} finally {
                clients.remove(conn); conn.close(); onLeave?.invoke(conn.id)
            }
        }
    }

    fun broadcast(snap: SnapshotDTO) {
        val line = JSON.encodeToString(NetMessage.serializer(), NetMessage("snapshot", snap = snap)) + "\n"
        clients.forEach { it.send(line) }
    }

    fun clientCount() = clients.size

    fun stop() {
        running = false
        try { server?.close() } catch (_: Exception) {}
        clients.forEach { it.close() }; clients.clear()
    }
}

class GameClient(private val host: String, private val port: Int = GAME_PORT) {
    private var socket: Socket? = null
    private var out: OutputStream? = null
    @Volatile var running = false

    var onWelcome: ((Int) -> Unit)? = null
    var onSnapshot: ((SnapshotDTO) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    fun connect(name: String) {
        running = true
        thread(name = "client-net") {
            try {
                val s = Socket(host, port); socket = s; out = s.getOutputStream()
                send(NetMessage("join", name = name))
                val br = BufferedReader(InputStreamReader(s.getInputStream()))
                while (running) {
                    val line = br.readLine() ?: break
                    val msg = runCatching { JSON.decodeFromString(NetMessage.serializer(), line) }.getOrNull() ?: continue
                    when (msg.type) {
                        "welcome" -> onWelcome?.invoke(msg.id)
                        "snapshot" -> msg.snap?.let { onSnapshot?.invoke(it) }
                    }
                }
            } catch (e: Exception) { onError?.invoke(e.message ?: "connection failed") }
        }
    }

    fun sendInput(i: InputDTO) = send(NetMessage("input", input = i))

    private fun send(m: NetMessage) {
        try { out?.write((JSON.encodeToString(NetMessage.serializer(), m) + "\n").toByteArray()); out?.flush() } catch (_: Exception) {}
    }

    fun close() { running = false; try { socket?.close() } catch (_: Exception) {} }
}
