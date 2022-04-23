package time.tracker.entity

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class Task(var title: String = "New task", var order: Int = 0) {

    companion object {
        private const val TAG = "Task"
    }


    @Id
    var id = 0L
    var startTime = 0L
    var elapsedTime: Long = 0
    var isRunning = false

    @Transient
    lateinit var elapsedTimeState: MutableState<String>

    @Transient
    var isRunningState: MutableState<Boolean> = mutableStateOf(false)

    @Transient
    lateinit var titleState: MutableState<String>

    override fun toString(): String {
        return "Task(title='$title', elapsedTime=$elapsedTime, isRunning=$isRunning, order=$order)"
    }
}