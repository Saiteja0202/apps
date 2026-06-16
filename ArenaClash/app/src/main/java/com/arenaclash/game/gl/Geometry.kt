package com.arenaclash.game.gl

object Geometry {
    val CUBE: FloatArray = run {
        val f = arrayOf(
            floatArrayOf(0f, 0f, 1f) to arrayOf(floatArrayOf(-.5f, -.5f, .5f), floatArrayOf(.5f, -.5f, .5f), floatArrayOf(.5f, .5f, .5f), floatArrayOf(-.5f, .5f, .5f)),
            floatArrayOf(0f, 0f, -1f) to arrayOf(floatArrayOf(.5f, -.5f, -.5f), floatArrayOf(-.5f, -.5f, -.5f), floatArrayOf(-.5f, .5f, -.5f), floatArrayOf(.5f, .5f, -.5f)),
            floatArrayOf(1f, 0f, 0f) to arrayOf(floatArrayOf(.5f, -.5f, .5f), floatArrayOf(.5f, -.5f, -.5f), floatArrayOf(.5f, .5f, -.5f), floatArrayOf(.5f, .5f, .5f)),
            floatArrayOf(-1f, 0f, 0f) to arrayOf(floatArrayOf(-.5f, -.5f, -.5f), floatArrayOf(-.5f, -.5f, .5f), floatArrayOf(-.5f, .5f, .5f), floatArrayOf(-.5f, .5f, -.5f)),
            floatArrayOf(0f, 1f, 0f) to arrayOf(floatArrayOf(-.5f, .5f, .5f), floatArrayOf(.5f, .5f, .5f), floatArrayOf(.5f, .5f, -.5f), floatArrayOf(-.5f, .5f, -.5f)),
            floatArrayOf(0f, -1f, 0f) to arrayOf(floatArrayOf(-.5f, -.5f, -.5f), floatArrayOf(.5f, -.5f, -.5f), floatArrayOf(.5f, -.5f, .5f), floatArrayOf(-.5f, -.5f, .5f))
        )
        val out = ArrayList<Float>(216)
        for ((n, c) in f) for (i in intArrayOf(0, 1, 2, 0, 2, 3)) {
            out.add(c[i][0]); out.add(c[i][1]); out.add(c[i][2]); out.add(n[0]); out.add(n[1]); out.add(n[2])
        }
        out.toFloatArray()
    }
    const val CUBE_VERTS = 36
}
