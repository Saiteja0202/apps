package com.arenaclash.game.gl

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.arenaclash.game.core.RP
import com.arenaclash.game.core.RenderSnapshot
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

class ArenaRenderer(private val provider: () -> RenderSnapshot?) : GLSurfaceView.Renderer {

    private var program = 0
    private var uMVP = 0; private var uModel = 0; private var uColor = 0; private var uLight = 0
    private var vbo = 0
    private val proj = FloatArray(16); private val view = FloatArray(16)
    private val model = FloatArray(16); private val baseM = FloatArray(16); private val partM = FloatArray(16)
    private val mvp = FloatArray(16); private val tmp = FloatArray(16)
    private var clock = 0f; private var lastN = 0L

    private val palette = arrayOf(
        floatArrayOf(0.30f, 0.55f, 1.0f), floatArrayOf(0.95f, 0.27f, 0.32f),
        floatArrayOf(0.30f, 0.82f, 0.48f), floatArrayOf(1.0f, 0.79f, 0.20f),
        floatArrayOf(0.78f, 0.40f, 1.0f), floatArrayOf(0.20f, 0.85f, 0.85f),
        floatArrayOf(1.0f, 0.55f, 0.20f), floatArrayOf(0.95f, 0.45f, 0.75f),
        floatArrayOf(0.6f, 0.85f, 0.30f), floatArrayOf(0.7f, 0.7f, 0.75f)
    )
    private val skin = floatArrayOf(0.93f, 0.78f, 0.62f)
    private val dark = floatArrayOf(0.13f, 0.14f, 0.18f)
    private val flame = floatArrayOf(1f, 0.6f, 0.15f)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE); GLES30.glCullFace(GLES30.GL_BACK)
        program = GLUtil.program(VS, FS)
        uMVP = GLES30.glGetUniformLocation(program, "uMVP")
        uModel = GLES30.glGetUniformLocation(program, "uModel")
        uColor = GLES30.glGetUniformLocation(program, "uColor")
        uLight = GLES30.glGetUniformLocation(program, "uLightDir")
        val b = ByteBuffer.allocateDirect(Geometry.CUBE.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        b.put(Geometry.CUBE); b.position(0)
        val ids = IntArray(1); GLES30.glGenBuffers(1, ids, 0); vbo = ids[0]
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, Geometry.CUBE.size * 4, b, GLES30.GL_STATIC_DRAW)
        lastN = 0L
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        Matrix.perspectiveM(proj, 0, 55f, width.toFloat() / height.coerceAtLeast(1), 0.1f, 200f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = if (lastN == 0L) 0f else (now - lastN) / 1_000_000_000f
        lastN = now; clock += dt

        GLES30.glClearColor(0.04f, 0.055f, 0.08f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        val s = provider() ?: return

        val local = s.players.firstOrNull { it.isLocal && it.alive } ?: s.players.firstOrNull { it.isLocal }
        val cx = local?.x ?: 0f; val cz = local?.z ?: 0f
        Matrix.setLookAtM(view, 0, cx, 19f, cz + 12f, cx, 0f, cz, 0f, 1f, 0f)

        GLES30.glUseProgram(program)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glEnableVertexAttribArray(0); GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 24, 0)
        GLES30.glEnableVertexAttribArray(1); GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 24, 12)
        GLES30.glUniform3f(uLight, 0.4f, 0.9f, 0.45f)

        val h = s.arenaHalf
        cube(0f, -0.15f, 0f, h * 2f, 0.3f, h * 2f, 0f, floatArrayOf(0.09f, 0.12f, 0.17f))
        // grid lines
        val grid = floatArrayOf(0.14f, 0.18f, 0.25f)
        var g = -h
        while (g <= h) {
            cube(g, 0.01f, 0f, 0.06f, 0.05f, h * 2f, 0f, grid)
            cube(0f, 0.01f, g, h * 2f, 0.05f, 0.06f, 0f, grid)
            g += 5f
        }
        // walls
        val wc = floatArrayOf(0.18f, 0.22f, 0.30f)
        cube(0f, 0.5f, -h, h * 2f, 1f, 0.5f, 0f, wc); cube(0f, 0.5f, h, h * 2f, 1f, 0.5f, 0f, wc)
        cube(-h, 0.5f, 0f, 0.5f, 1f, h * 2f, 0f, wc); cube(h, 0.5f, 0f, 0.5f, 1f, h * 2f, 0f, wc)

        // zone ring
        val zc = floatArrayOf(0.20f, 0.85f, 0.95f)
        val ring = 60
        for (i in 0 until ring) { val a = (i.toFloat() / ring) * 6.2832f; cube(cos(a) * s.zoneR, 0.2f, sin(a) * s.zoneR, 0.45f, 0.4f, 0.45f, 0f, zc) }

        // health packs (green box + white cross, bobbing)
        val packGreen = floatArrayOf(0.20f, 0.85f, 0.40f)
        val bob = 0.15f * sin(clock * 2.5f) + 0.55f
        for (i in s.packX.indices) {
            val px = s.packX[i]; val pz = s.packZ[i]
            cube(px, bob, pz, 0.6f, 0.6f, 0.6f, clock * 60f, packGreen)
            cube(px, bob + 0.45f, pz, 0.45f, 0.12f, 0.12f, 0f, floatArrayOf(1f, 1f, 1f))
            cube(px, bob + 0.45f, pz, 0.12f, 0.12f, 0.45f, 0f, floatArrayOf(1f, 1f, 1f))
        }

        // bullets
        for (i in s.bulletX.indices) cube(s.bulletX[i], 1.0f, s.bulletZ[i], 0.3f, 0.3f, 0.3f, clock * 200f, floatArrayOf(1f, 0.95f, 0.5f))

        // soldiers
        for (p in s.players) if (p.alive) drawSoldier(p)
    }

    private fun drawSoldier(p: RP) {
        val col = palette[p.colorIndex % palette.size]
        Matrix.setIdentityM(baseM, 0)
        Matrix.translateM(baseM, 0, p.x, 0f, p.z)
        Matrix.rotateM(baseM, 0, -(p.angle * 57.2958f), 0f, 1f, 0f)
        val swing = if (p.moving) sin(clock * 11f + p.colorIndex) * 0.32f else 0f

        part(swing, 0.38f, 0.16f, 0.24f, 0.78f, 0.24f, dark)        // leg L
        part(-swing, 0.38f, -0.16f, 0.24f, 0.78f, 0.24f, dark)      // leg R
        part(-0.34f, 1.05f, 0f, 0.24f, 0.62f, 0.42f, dark)          // jetpack
        if (p.moving) part(-0.46f, 0.62f, 0f, 0.18f, 0.4f, 0.26f, flame) // flame
        part(0f, 1.05f, 0f, 0.52f, 0.72f, 0.56f, col)              // torso
        part(0f, 1.62f, 0f, 0.44f, 0.44f, 0.44f, if (p.isLocal) floatArrayOf(1f, 1f, 1f) else skin) // head
        part(0.30f, 1.08f, 0.14f, 0.5f, 0.2f, 0.2f, col)           // front arm
        part(0.66f, 1.02f, 0.14f, 0.62f, 0.17f, 0.17f, dark)       // gun
        if (p.isLocal) part(0f, 2.25f, 0f, 0.26f, 0.26f, 0.26f, floatArrayOf(1f, 1f, 0.3f)) // marker
    }

    /** draws a part relative to the current soldier base transform */
    private fun part(lx: Float, ly: Float, lz: Float, sx: Float, sy: Float, sz: Float, color: FloatArray) {
        System.arraycopy(baseM, 0, partM, 0, 16)
        Matrix.translateM(partM, 0, lx, ly, lz)
        Matrix.scaleM(partM, 0, sx, sy, sz)
        Matrix.multiplyMM(tmp, 0, view, 0, partM, 0)
        Matrix.multiplyMM(mvp, 0, proj, 0, tmp, 0)
        GLES30.glUniformMatrix4fv(uMVP, 1, false, mvp, 0)
        GLES30.glUniformMatrix4fv(uModel, 1, false, partM, 0)
        GLES30.glUniform3f(uColor, color[0], color[1], color[2])
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, Geometry.CUBE_VERTS)
    }

    private fun cube(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, rotYdeg: Float, color: FloatArray) {
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, x, y, z)
        if (rotYdeg != 0f) Matrix.rotateM(model, 0, rotYdeg, 0f, 1f, 0f)
        Matrix.scaleM(model, 0, sx, sy, sz)
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
uniform mat4 uMVP; uniform mat4 uModel;
out vec3 vN;
void main(){ gl_Position = uMVP*vec4(aPos,1.0); vN = mat3(uModel)*aNormal; }
"""
private const val FS = """#version 300 es
precision mediump float;
in vec3 vN; uniform vec3 uColor; uniform vec3 uLightDir; out vec4 frag;
void main(){ vec3 n=normalize(vN); float d=max(dot(n,normalize(uLightDir)),0.0); frag=vec4(uColor*(0.45+0.7*d),1.0); }
"""
