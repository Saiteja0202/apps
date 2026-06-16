package com.chromacatch.game

/** Interleaved position(3) + normal(3) for a unit cube centered at origin (side 1). */
object Geometry {
    val CUBE: FloatArray = run {
        // 6 faces, each: normal + 4 corners -> 2 triangles
        val faces = arrayOf(
            // normal, then 4 corners (ccw)
            floatArrayOf(0f, 0f, 1f) to arrayOf(
                floatArrayOf(-.5f, -.5f, .5f), floatArrayOf(.5f, -.5f, .5f),
                floatArrayOf(.5f, .5f, .5f), floatArrayOf(-.5f, .5f, .5f)
            ),
            floatArrayOf(0f, 0f, -1f) to arrayOf(
                floatArrayOf(.5f, -.5f, -.5f), floatArrayOf(-.5f, -.5f, -.5f),
                floatArrayOf(-.5f, .5f, -.5f), floatArrayOf(.5f, .5f, -.5f)
            ),
            floatArrayOf(1f, 0f, 0f) to arrayOf(
                floatArrayOf(.5f, -.5f, .5f), floatArrayOf(.5f, -.5f, -.5f),
                floatArrayOf(.5f, .5f, -.5f), floatArrayOf(.5f, .5f, .5f)
            ),
            floatArrayOf(-1f, 0f, 0f) to arrayOf(
                floatArrayOf(-.5f, -.5f, -.5f), floatArrayOf(-.5f, -.5f, .5f),
                floatArrayOf(-.5f, .5f, .5f), floatArrayOf(-.5f, .5f, -.5f)
            ),
            floatArrayOf(0f, 1f, 0f) to arrayOf(
                floatArrayOf(-.5f, .5f, .5f), floatArrayOf(.5f, .5f, .5f),
                floatArrayOf(.5f, .5f, -.5f), floatArrayOf(-.5f, .5f, -.5f)
            ),
            floatArrayOf(0f, -1f, 0f) to arrayOf(
                floatArrayOf(-.5f, -.5f, -.5f), floatArrayOf(.5f, -.5f, -.5f),
                floatArrayOf(.5f, -.5f, .5f), floatArrayOf(-.5f, -.5f, .5f)
            )
        )
        val out = ArrayList<Float>(6 * 6 * 6)
        for ((n, c) in faces) {
            val tris = intArrayOf(0, 1, 2, 0, 2, 3)
            for (i in tris) {
                out.add(c[i][0]); out.add(c[i][1]); out.add(c[i][2])
                out.add(n[0]); out.add(n[1]); out.add(n[2])
            }
        }
        out.toFloatArray()
    }
    const val CUBE_VERTS = 36
}
