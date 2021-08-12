package time.tracker.activity

import android.content.Context
import android.os.*
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension.Companion.fillToConstraints
import io.objectbox.Box
import kotlinx.coroutines.*
import time.tracker.App
import time.tracker.R
import time.tracker.entity.Task
import time.tracker.entity.Task_
import time.tracker.util.secondsToTime
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG: String = MainActivity::class.java.simpleName
    }

    private lateinit var handler: Handler
    private lateinit var box: Box<Task>
    private lateinit var tasks: SnapshotStateList<Task>
    private val cScope = CoroutineScope(Dispatchers.Default)

    @ExperimentalAnimationApi
    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column {
                TopAppBar(backgroundColor = Color.Black, contentColor = Color.White) {
                    Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                        Text("TimeTracker", fontSize = 24.sp)
                    }
                }
                ConstraintLayout(Modifier.fillMaxSize()) {
                    val allTasks = box.query().order(Task_.order).build().find()
                    Log.i(TAG, "all tasks from db=>$allTasks")
                    tasks = remember { mutableStateListOf(*allTasks.toTypedArray()) }
                    Log.i(TAG, "tasks=>${tasks.map { it }}")
                    Column(Modifier.padding(4.dp)) {
                        tasks.forEachIndexed { i, t ->
                            Spacer(Modifier.height(4.dp))
                            TaskItem(t, i, tasks) {
                                if (!((i == 0 && it.y + it.offset < it.y) ||
                                            (i == tasks.size - 1 && it.y + it.offset > it.y) ||
                                            (kotlin.math.abs(it.offset) < 84))
                                ) reorder(tasks, it)
                            }
                        }
                    }
                    Log.i(TAG, "constraint layout recomposed task count=>${tasks.size}")
                    FloatingActionButton(
                        backgroundColor = Color.Companion.Black,
                        contentColor = Color.White,
                        modifier = Modifier.constrainAs(createRef()) {
                            end.linkTo(parent.end, 8.dp)
                            bottom.linkTo(parent.bottom, 32.dp)
                        },
                        onClick = {
                            if (tasks.isNotEmpty() && tasks[0].startTime == 0L) return@FloatingActionButton
                            tasks.add(0, Task(getString(R.string.new_task), 0))
                            Log.i(TAG, "add task on click, size=>${tasks.size}")
                            Log.i(TAG, "list class=>${tasks.javaClass.name}")
                            tasks.forEachIndexed { index, task -> if (index != 0) task.order = index }
                        }) {
                        Icon(
                            painterResource(id = android.R.drawable.ic_input_add),
                            contentDescription = getString(R.string.new_task)
                        )
                    }
                }
            }
        }
//        setSupportActionBar(findViewById(R.id.toolbar))
        handler = Handler(Looper.myLooper()!!)
        val app = application as App
        box = app.box

//        adView.adListener = object : AdListener() {
//            override fun onAdLoaded() {
//                adView.visibility = VISIBLE
//            }
//        }
//        adView.loadAd(AdRequest.Builder().build())
//        if (BuildConfig.DEBUG) {
//            val started = AndroidObjectBrowser(app.store).start(this)
//            Log.i(TAG, "ObjectBrowser started: $started")
//        }
    }

    private fun reorder(tasks: SnapshotStateList<Task>, task: Task) {
        Log.i(TAG, "reordering task=>$task")
        val dragEndPosition = task.y + task.offset
        var min = kotlin.math.abs(tasks[0].y - dragEndPosition)
        var index = 0
        for (i in 1 until tasks.size) {
            if (tasks[i] == task) {
                continue
            }
            val candidate = kotlin.math.abs(tasks[i].y - dragEndPosition)
            if (candidate < min) {
                min = candidate
                index = i
            }
        }
        tasks.remove(task)
        tasks.add(index, task)
        tasks.forEachIndexed { i, t -> t.order = i }
    }

    override fun onStop() {
        Log.i(TAG, "on stop tasks=>${tasks.map { it }}")
        box.put(tasks)
        super.onStop()
    }

    @ExperimentalAnimationApi
    @ExperimentalMaterialApi
    @Composable
    private fun TaskItem(task: Task, index: Int, tasks: SnapshotStateList<Task>, calculatePosition: (Task) -> Unit) {
        task.elapsedTimeState = mutableStateOf(secondsToTime(task.elapsedTime))
        task.titleState = mutableStateOf(task.title)
        if (task.isRunning && (task.isRunningState == null || !task.isRunningState!!.value)) {
            start(task)
            if (task.isRunningState == null) task.isRunningState = mutableStateOf(true)

            Log.i(TAG, "started task running from DB task=>$task")
        }
        val elevation = remember { mutableStateOf(0.dp) }
        val offset = remember { mutableStateOf(0F) }
        val dismissState = rememberDismissState {
//            if (task.id != 0L) box.remove(task)
//            tasks.remove(task)
//            Log.i(TAG, "rememberDismissState")
            true
        }
        val isDismissed = dismissState.isDismissed(DismissDirection.EndToStart) ||
                dismissState.isDismissed(DismissDirection.StartToEnd)
        LaunchedEffect(isDismissed) {
            Log.i(TAG, "LaunchedEffect is dismissed=>$isDismissed")
            if (isDismissed) {
                if (task.id != 0L) box.remove(task)
                delay(300)
                tasks.remove(task)
                dismissState.snapTo(DismissValue.Default)
                Log.i(TAG, "removed task=>$task, count=>${tasks.size}")
            }
        }
        Log.i(TAG, "TaskItem recomposed=>$task; index=>$index, is dismissed=>$isDismissed")
        val itemModifier = Modifier
            .pointerInput(task) {
                detectDragGesturesAfterLongPress(onDragStart = { _ ->
                    elevation.value = 64.dp
                    vibrate()
                    Log.i(TAG, "on drag start=>$task; index=>$index")
                }, onDragEnd = {
                    calculatePosition(task)
                    elevation.value = 0.dp
                    offset.value = 0F
                    Log.i(TAG, "on drag end=>$task; index=>$index")
                }) { _, dragAmount ->
                    offset.value += dragAmount.y
                    task.offset = offset.value.roundToInt()
                    Log.i(TAG, "on drag: task=>$task; index=>$index")
                }
            }
//        AnimatedVisibility(
//            visible = !isDismissed, exit = shrinkVertically(
//                animationSpec = tween(
//                    durationMillis = 300,
//                )
//            )
//        ) {
        SwipeToDismiss(state = dismissState, background = { /*TODO*/ }) {
            Card(Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, offset.value.roundToInt()) }
                .onGloballyPositioned {
                    task.y = 4 + index * (4 + it.size.height)
                    task.offset = offset.value.roundToInt()
                }, border = BorderStroke(4.dp, Color.Black), shape = RoundedCornerShape(25),
                elevation = elevation.value
            ) {
                ConstraintLayout(Modifier.padding(start = 8.dp)) {
                    val (time, playBtn) = createRefs()
                    TextField(textStyle = TextStyle(fontSize = 20.sp),
                        modifier = Modifier
                            .constrainAs(createRef()) {
                                top.linkTo(parent.top)
                                bottom.linkTo(parent.bottom)
                                start.linkTo(parent.start)
                                end.linkTo(time.start)
                                width = fillToConstraints
                            },
                        label = {
                            Text(
                                getString(
                                    if (task.startTime > 0) R.string.current_name
                                    else R.string.new_task
                                )
                            )
                        },
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = Color.White,
                        ),
                        value = task.titleState.value,
                        onValueChange = { v ->
                            task.titleState.value = v
                            task.title = v
                        })
                    if (task.startTime > 0) {
                        Column(itemModifier.constrainAs(time) {
                            end.linkTo(playBtn.start, 16.dp)
                            width = fillToConstraints
                        }) {
                            Text(
                                task.elapsedTimeState.value,
                                fontSize = 31.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Image(
                        painterResource(
                            id = if (task.isRunningState?.value == true) R.drawable.ic_pause_circle_outline_24
                            else R.drawable.ic_play_circle_outline_24
                        ),
                        contentDescription = getString(R.string.start_task),
                        modifier = itemModifier
                            .constrainAs(playBtn) {
                                end.linkTo(parent.end, 4.dp)
                                top.linkTo(parent.top)
                                bottom.linkTo(parent.bottom)
                            }
                            .clickable(
                                interactionSource = MutableInteractionSource(),
                                indication = null
                            ) {
                                if (task.titleState.value.isEmpty()) task.titleState.value = task.title
                                val value = task.isRunning
                                task.isRunning = !value
                                task.isRunningState?.value = !value
                                if (task.isRunning) {
                                    task.startTime = System.currentTimeMillis()
                                    start(task)
                                }
                                Log.i(TAG, "start task click, task=>$task")
                            })
                }
            }
        }
//        }
    }

    private fun vibrate() {
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v?.vibrate(VibrationEffect.createOneShot(71, 251))
        } else {
            v?.vibrate(50)
        }
    }

    private fun start(task: Task): Job = cScope.launch {
        task.elapsedTime++
        withContext(Dispatchers.Main) {
            task.elapsedTimeState.value = secondsToTime(task.elapsedTime)
        }
        delay(1000)
        if (task.isRunning) {
            start(task)
        }
    }
}

