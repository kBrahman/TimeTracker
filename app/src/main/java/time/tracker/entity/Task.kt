package time.tracker.entity

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class Task(var title: String = "New task", var order: Int) {

    companion object {
        private const val TAG = "Task"
    }

    var offset = 0

    @Id
    var id = 0L
    var startTime = 0L
    var elapsedTime: Int = 0
    var isRunning = false
        set(value) {
            isRunningState?.value = value
            field = value
            Log.i(TAG, "set is running to=>$value")
        }
    var endTime = 0L

    @Transient
    lateinit var elapsedTimeState: MutableState<String>

    @Transient
    var isRunningState: MutableState<Boolean>? = mutableStateOf(false)

    @Transient
    lateinit var titleState: MutableState<String>

    override fun toString(): String {
        return "Task(title='$title', elapsedTime=$elapsedTime, isRunning=$isRunning, order=$order)"
    }
}