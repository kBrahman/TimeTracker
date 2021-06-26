package time.tracker.entity

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class Task(var title: String = "New task", var startTime: Long = 0) {

    var y = 0
    var offset = 0

    @Id
    private var id: Long? = null
    var elapsedTime: Int = 0
    val isRunning get() = isRunningState.value
    var endTime = 0L

    @Transient
    lateinit var elapsedTimeState: MutableState<String>

    @Transient
    val isRunningState = mutableStateOf(false)

    @Transient
    lateinit var titleState: MutableState<String>

    fun getId(): Long? = id

    fun setId(id: Long) {
        this.id = id
    }

    override fun toString(): String {
        return "Task(title='$title', elapsedTime=$elapsedTime, isRunning=$isRunning)"
    }
}