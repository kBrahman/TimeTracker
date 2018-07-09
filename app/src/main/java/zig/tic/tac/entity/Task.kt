package zig.tic.tac.entity

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
class Task(val title: String = "", val startTime: Long = 0) {

    @Id
    private var id: Long? = null
    private var elapsedTime: Int = 0
    private var isRunning = false
    private var timeClosed = 0L

    fun setElapsedTime(elapsedTime: Int) {
        this.elapsedTime = elapsedTime
    }

    fun getElapsedTime(): Int = elapsedTime

    fun getId(): Long? = id

    fun setId(id: Long) {
        this.id = id
    }

    fun setIsRunning(isRunning: Boolean) {
        this.isRunning = isRunning
    }

    fun setTimeClosed(timeClosed: Long) {
        this.timeClosed = timeClosed
    }

    fun getTimeClosed(): Long = timeClosed

    fun isRunning(): Boolean = isRunning
}