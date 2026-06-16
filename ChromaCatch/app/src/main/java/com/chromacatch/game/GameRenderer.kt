package com.chromacatch.game

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sin
import kotlin.random.Random

class GameRenderer(private val context: Context) : GLSurfaceView.Renderer {

    companion object {
        const val START = 0; const val PLAY = 1; const val OVER = 2
        private const val CATCHER_Y = -2.0f
        private const val CATCHER_HALF = 0.7f
        private const val CATCH_LINE = CATCHER_Y + CATCHER_HALF + 0.35f // gem center triggers here
        private const val SPAWN_Y = 4.4f
    }

    // ---- state shared with the HUD (mutated only on the GL thread) ----
    @Volatile var phase = START
    @Volatile var score = 0
    @Volatile var combo = 0
    @Volatile var best = 0

    private val COLORS = arrayOf(
        floatArrayOf(0.95f, 0.27f, 0.32f), // red
        floatArrayOf(1.0f, 0.79f, 0.20f),  // yellow
        floatArrayOf(0.30f, 0.82f, 0.48f), // green
        floatArrayOf(0.30f, 0.55f, 1.0f)   // blue
    )

    // GL handles
    private var program = 0
    private var uMVP = 0; private var uModel = 0; private var uColor = 0; private var uLight = 0
    private var vboId = 0
    private val proj = FloatArray(16); private val view = FloatArray(16)
    private val model = FloatArray(16); private val mvp = FloatArray(16); private val tmp = FloatArray(16)

    // game vars
    private var activeColor = 0
    private var curAngle = 0f
    private var targetAngle = 0f
    private var gemY = SPAWN_Y
    private var gemColor = 0
    private var gemSpin = 0f
    private var speed = 3f
    private var bounce = 0f
    private var shake = 0f
    private var bgT = 0f
    private var lastNanos = 0L

    private class P(var x: Float, var y: Float, var z: Float, var vx: Float, var vy: Float, var vz: Float, var life: Float, val maxLife: Float, val col: Int)
    private val particles = ArrayList<P>()

    private val prefs = context.getSharedPreferences("chroma", Context.MODE_PRIVATE)

    init { best = prefs.getInt("best", 0) }

    fun onTap(side: Int) {
        when (phase) {
            START, OVER -> startGame()
            PLAY -> {
                val dir = if (side < 0) -1 else 1
                activeColor = ((activeColor + dir) % 4 + 4) % 4
                targetAngle += dir * 90f
            }
        }
    }

    private fun startGame() {
        score = 0; combo = 0; activeColor = 0; curAngle = 0f; targetAngle = 0f
        speed = 3f; particles.clear(); spawnGem(); phase = PLAY
    }

    private fun spawnGem() {
        gemY = SPAWN_Y
        gemColor = Random.nextInt(4)
    }

    private fun onCatch() {
        val mult = 1 + combo / 5
        score += mult
        combo++
        bounce = 0.35f
        burst(gemColor)
        spawnGem()
    }

    private fun onMiss() {
        phase = OVER
        shake = 0.6f
        if (score > best) { best = score; prefs.edit().putInt("best", best).apply() }
    }

    private fun burst(col: Int) {
        repeat(16) {
            val a = Random.nextFloat() * 6.2832f
            val sp = 2f + Random.nextFloat() * 3f
            val up = 1f + Random.nextFloat() * 3f
            particles.add(P(0f, CATCH_LINE, 0f,
                kotlin.math.cos(a) * sp, up, kotlin.math.sin(a) * sp,
                0.7f, 0.7f, col))
        }
    }

    // ---- GLSurfaceView.Renderer ----
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)
        program = GLUtil.program(VS, FS)
        uMVP = GLES30.glGetUniformLocation(program, "uMVP")
        uModel = GLES30.glGetUniformLocation(program, "uModel")
        uColor = GLES30.glGetUniformLocation(program, "uColor")
        uLight = GLES30.glGetUniformLocation(program, "uLightDir")

        val buf: FloatBuffer = ByteBuffer.allocateDirect(Geometry.CUBE.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(Geometry.CUBE); position(0) }
        val ids = IntArray(1); GLES30.glGenBuffers(1, ids, 0); vboId = ids[0]
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, Geometry.CUBE.size * 4, buf, GLES30.GL_STATIC_DRAW)
        lastNanos = 0L
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.coerceAtLeast(1)
        Matrix.perspectiveM(proj, 0, 50f, aspect, 0.1f, 100f)
        Matrix.setLookAtM(view, 0, 0f, 0.6f, 8.2f, 0f, -0.3f, 0f, 0f, 1f, 0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        var dt = if (lastNanos == 0L) 0f else (now - lastNanos) / 1_000_000_000f
        lastNanos = now
        if (dt > 0.05f) dt = 0.05f
        update(dt)
        render()
    }

    private fun update(dt: Float) {
        bgT += dt
        curAngle += (targetAngle - curAngle) * (dt * 12f).coerceAtMost(1f)
        bounce += (0f - bounce) * (dt * 8f).coerceAtMost(1f)
        shake += (0f - shake) * (dt * 6f).coerceAtMost(1f)
        gemSpin += dt * 140f

        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.life -= dt
            if (p.life <= 0f) { it.remove(); continue }
            p.vy -= 6f * dt
            p.x += p.vx * dt; p.y += p.vy * dt; p.z += p.vz * dt
        }

        if (phase == PLAY) {
            speed = (3f + score * 0.07f).coerceAtMost(11f)
            gemY -= speed * dt
            if (gemY <= CATCH_LINE) {
                if (gemColor == activeColor) onCatch() else onMiss()
            }
        }
    }

    private fun render() {
        val pulse = 0.04f + 0.03f * sin(bgT * 0.6f)
        GLES30.glClearColor(0.027f + pulse, 0.039f + pulse, 0.078f + pulse, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        GLES30.glUseProgram(program)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 24, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 24, 12)
        GLES30.glUniform3f(uLight, 0.45f, 0.85f, 0.55f)

        // catcher
        val shakeX = if (shake > 0.001f) (Random.nextFloat() - 0.5f) * shake * 0.6f else 0f
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, shakeX, CATCHER_Y, 0f)
        Matrix.rotateM(model, 0, curAngle, 0f, 1f, 0f)
        val cs = (CATCHER_HALF * 2f) * (1f + bounce)
        Matrix.scaleM(model, 0, cs, cs, cs)
        drawCube(COLORS[activeColor])

        // gem
        if (phase != START) {
            Matrix.setIdentityM(model, 0)
            Matrix.translateM(model, 0, 0f, gemY, 0f)
            Matrix.rotateM(model, 0, gemSpin, 0.4f, 1f, 0.2f)
            Matrix.scaleM(model, 0, 0.62f, 0.62f, 0.62f)
            drawCube(COLORS[gemColor])
        }

        // particles
        for (p in particles) {
            val s = 0.16f * (p.life / p.maxLife).coerceIn(0f, 1f)
            Matrix.setIdentityM(model, 0)
            Matrix.translateM(model, 0, p.x, p.y, p.z)
            Matrix.rotateM(model, 0, p.life * 400f, 1f, 1f, 0f)
            Matrix.scaleM(model, 0, s, s, s)
            drawCube(COLORS[p.col])
        }
    }

    private fun drawCube(color: FloatArray) {
        Matrix.multiplyMM(tmp, 0, view, 0, model, 0)
        Matrix.multiplyMM(mvp, 0, proj, 0, tmp, 0)
        GLES30.glUniformMatrix4fv(uMVP, 1, false, mvp, 0)
        GLES30.glUniformMatrix4fv(uModel, 1, false, model, 0)
        GLES30.glUniform3f(uColor, color[0], color[1], color[2])
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, Geometry.CUBE_VERTS)
    }
}

private const val VS = """#version 300 es
layout(location=0) in vec3 aPos;
layout(location=1) in vec3 aNormal;
uniform mat4 uMVP;
uniform mat4 uModel;
out vec3 vN;
void main(){
    gl_Position = uMVP * vec4(aPos, 1.0);
    vN = mat3(uModel) * aNormal;
}
"""

private const val FS = """#version 300 es
precision mediump float;
in vec3 vN;
uniform vec3 uColor;
uniform vec3 uLightDir;
out vec4 frag;
void main(){
    vec3 n = normalize(vN);
    float d = max(dot(n, normalize(uLightDir)), 0.0);
    vec3 c = uColor * (0.40 + 0.75 * d);
    frag = vec4(c, 1.0);
}
"""
