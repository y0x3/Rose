package com.tiempo.app.domain.lifepanel

data class ScaleSnapshot(
    val scale: TimeScale,
    val total: Int,
    val elapsed: Int,
    /** null cuando la persona ya superó la edad de referencia. */
    val currentIndex: Int?,
    val columns: Int,
    val beyondReference: Boolean
) {
    val remaining: Int get() = (total - elapsed - if (currentIndex != null) 1 else 0).coerceAtLeast(0)
    val percentElapsed: Float get() = if (total == 0) 0f else elapsed.toFloat() / total

    fun stateAt(index: Int): CellState = when {
        currentIndex != null && index == currentIndex -> CellState.CURRENT
        index < elapsed -> CellState.ELAPSED
        else -> CellState.FUTURE
    }
}