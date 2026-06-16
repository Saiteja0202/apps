package com.arenaclash.game.gl

import android.opengl.GLES30
import android.util.Log

object GLUtil {
    private fun compile(type: Int, src: String): Int {
        val s = GLES30.glCreateShader(type)
        GLES30.glShaderSource(s, src); GLES30.glCompileShader(s)
        val st = IntArray(1); GLES30.glGetShaderiv(s, GLES30.GL_COMPILE_STATUS, st, 0)
        if (st[0] == 0) Log.e("ArenaClash", "shader: " + GLES30.glGetShaderInfoLog(s))
        return s
    }
    fun program(vs: String, fs: String): Int {
        val p = GLES30.glCreateProgram()
        GLES30.glAttachShader(p, compile(GLES30.GL_VERTEX_SHADER, vs))
        GLES30.glAttachShader(p, compile(GLES30.GL_FRAGMENT_SHADER, fs))
        GLES30.glLinkProgram(p)
        val st = IntArray(1); GLES30.glGetProgramiv(p, GLES30.GL_LINK_STATUS, st, 0)
        if (st[0] == 0) Log.e("ArenaClash", "link: " + GLES30.glGetProgramInfoLog(p))
        return p
    }
}
