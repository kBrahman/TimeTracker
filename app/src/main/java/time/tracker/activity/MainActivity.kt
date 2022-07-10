package time.tracker.activity

import android.content.Context
import android.os.*
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension.Companion.fillToConstraints
import androidx.constraintlayout.compose.Dimension.Companion.matchParent
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.InterstitialAd
import com.facebook.ads.InterstitialAdListener
import io.objectbox.Box
import kotlinx.coroutines.*
import time.tracker.App
import time.tracker.R
import time.tracker.entity.Task
import time.tracker.entity.Task_
import time.tracker.util.move
import time.tracker.util.offsetEnd
import time.tracker.util.secondsToTime


@ExperimentalMaterialApi
@ExperimentalAnimationApi
class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG: String = MainActivity::class.java.simpleName
    }

    private var paused = false
    private lateinit var loading: MutableState<Boolean>
    private lateinit var box: Box<Task>
    private lateinit var tasks: SnapshotStateList<Task>
    private val cScope = CoroutineScope(Dispatchers.Default)
    private var timeOut = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val interstitialAd = InterstitialAd(this, "2766903800218516_2766908440218052")
//        AdSettings.addTestDevice("5dec1284-51f6-43c1-9b84-5ceef35455e6")
        interstitialAd.loadAd(
            interstitialAd.buildLoadAdConfig()
                .withAdListener(object : InterstitialAdListener {
                    override fun onError(p0: Ad?, err: AdError?) {
                        Log.e(TAG, "Interstitial ad failed to load: " + err?.errorMessage)
                        loading.value = false
                    }

                    override fun onAdLoaded(ad: Ad?) {
                        if (!timeOut) interstitialAd.show()
                        else interstitialAd.destroy()
                    }

                    override fun onAdClicked(p0: Ad?) {}
                    override fun onLoggingImpression(p0: Ad?) {}
                    override fun onInterstitialDisplayed(p0: Ad?) {}
                    override fun onInterstitialDismissed(p0: Ad?) {
                        loading.value = false
                        interstitialAd.destroy()
                    }
                }).build()
        )

        setContent {
            MaterialTheme(lightColors(Color.Black)) {
                Column {
                    TopAppBar(backgroundColor = Color.Black, contentColor = Color.White) {
                        Text("TimeTracker", fontSize = 20.sp)
                    }
                    ConstraintLayout(Modifier.fillMaxSize()) {
                        val allTasks = box.query().order(Task_.order).build().find()
                        tasks = remember { mutableStateListOf(*allTasks.toTypedArray()) }
                        val listState = rememberLazyListState()
                        var draggedItem by remember { mutableStateOf<LazyListItemInfo?>(null) }
                        val draggedDistance = remember { mutableStateOf(0F) }
                        loading = remember { mutableStateOf(true) }
                        LazyColumn(
                            Modifier
                                .fillMaxHeight()
                                .pointerInput(Unit) {
                                    detectDragGesturesAfterLongPress({ offset ->
                                        vibrate()
                                        draggedItem = listState.layoutInfo.visibleItemsInfo.find { item ->
                                            offset.y.toInt() in item.offset..item.offsetEnd
                                        }
                                        Log.i(TAG, "on drag start, index=>${draggedItem?.index}")
                                    }, {
                                        draggedDistance.value = 0F
                                        draggedItem = null
                                    }, {
                                        draggedDistance.value = 0F
                                        draggedItem = null
                                    }) { ch, off ->
                                        ch.consume()
                                        draggedDistance.value = draggedDistance.value.plus(off.y)
                                        draggedItem?.let { di ->
                                            val startOff = di.offset + draggedDistance.value
                                            val endOff = di.offsetEnd + draggedDistance.value
                                            val found = listState.layoutInfo.visibleItemsInfo.find { info ->
                                                val threshold = (info.offset + info.offsetEnd) / 2
                                                info != di &&
                                                        ((draggedDistance.value > 0 && endOff.toInt() in threshold + di.size / 4..info.offsetEnd) ||
                                                                (startOff.toInt() in info.offset..threshold - di.size / 4 &&
                                                                        draggedDistance.value < 0))
                                            }
                                            if (found != null) {
                                                val to = di.index
                                                val from = found.index
                                                tasks.move(from, to)
                                                Log.i(TAG, "moved from:$from, to:$to")
                                                draggedItem = found
                                                draggedDistance.value = endOff - found.offsetEnd
                                            }
                                        }
                                    }
                                },
                            listState,
                            PaddingValues(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalArrangement = spacedBy(4.dp)
                        ) {
                            items(count = tasks.size, key = { tasks[it].hashCode() }) { i ->
                                TaskItem(tasks[i], i, tasks, draggedDistance, draggedItem)
                            }
                        }
                        FloatingActionButton(
                            backgroundColor = Color.Companion.Black,
                            contentColor = Color.White,
                            modifier = Modifier.constrainAs(createRef()) {
                                end.linkTo(parent.end, 8.dp)
                                bottom.linkTo(parent.bottom, 32.dp)
                            },
                            onClick = {
                                if (tasks.isNotEmpty() && tasks[0].startTime == 0L) return@FloatingActionButton
                                reorder()
                                tasks.add(0, Task(getString(R.string.new_task), 0))
                            }) {
                            Icon(
                                painterResource(id = android.R.drawable.ic_input_add),
                                contentDescription = getString(R.string.new_task)
                            )
                        }
                        if (loading.value) Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.White)
                        ) { CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) }
                    }
                }
            }
        }
        val app = application as App
        box = app.box
        setTimer()
    }

    private fun setTimer() = cScope.launch {
        delay(7000)
        loading.value = false
        timeOut = true
    }

    private fun reorder() = tasks.forEachIndexed { i, t -> t.order = i + 1 }

    override fun onStop() {
        paused = true
        reorder()
        box.put(tasks)
        Log.i(TAG, "on stop tasks=>${tasks.map { it }}")
        super.onStop()
    }

    @ExperimentalAnimationApi
    @ExperimentalMaterialApi
    @Composable
    private fun TaskItem(
        task: Task,
        i: Int,
        tasks: SnapshotStateList<Task>,
        dragDistance: MutableState<Float>,
        draggedItem: LazyListItemInfo?
    ) {
        task.elapsedTimeState = mutableStateOf(secondsToTime(task.elapsedTime))
        task.titleState = mutableStateOf(task.title)
        if (task.isRunning && !task.isRunningState.value) {
            task.elapsedTime = (System.currentTimeMillis() - task.startTime) / 1000
            start(task)
            task.isRunningState.value = true
        }
        var isDeleted by remember { mutableStateOf(false) }

        val dismissState = rememberDismissState {
            if (it == DismissValue.DismissedToStart || it == DismissValue.DismissedToEnd) isDeleted = true
            isDeleted
        }
        val same = draggedItem?.index == i
        AnimatedVisibility(
            !isDeleted,
            Modifier.zIndex(if (same) 1F else 0F),
            exit = shrinkVertically(animationSpec = tween(durationMillis = 300))
        ) {
            SwipeToDismiss(state = dismissState, background = {}) {
                if (isDeleted) remove(task, tasks)
                Card(
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationY = if (same) dragDistance.value else 0F },
                    border = BorderStroke(4.dp, Color.Black),
                    shape = RoundedCornerShape(25),
                    elevation = if (same) 16.dp else 1.dp
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
                                    width = matchParent
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
                            Column(Modifier.constrainAs(time) {
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
                                id = if (task.isRunningState.value) R.drawable.ic_pause_circle_outline_24
                                else R.drawable.ic_play_circle_outline_24
                            ),
                            contentDescription = getString(R.string.start_task),
                            modifier = Modifier
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
                                    val running = task.isRunning
                                    task.isRunning = !running
                                    task.isRunningState.value = !running
                                    if (task.isRunning) {
                                        task.startTime = System.currentTimeMillis()
                                        start(task)
                                    }
                                    Log.i(TAG, "start task click, task=>$task")
                                })
                    }
                }
            }
        }
    }

    private fun remove(task: Task, tasks: SnapshotStateList<Task>) = cScope.launch {
        Log.i(TAG, "removing task=>$task")
        if (task.id != 0L) box.remove(task)
        delay(300)
        tasks.remove(task)
    }


    private fun vibrate() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
            .vibrate(CombinedVibration.createParallel(VibrationEffect.createOneShot(71, 251)))
    else {
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v?.vibrate(VibrationEffect.createOneShot(71, 251))
        else v?.vibrate(50)
    }

    override fun onResume() {
        super.onResume()
        if (paused) tasks.forEach {
            if (it.isRunning) {
                it.elapsedTime = (System.currentTimeMillis() - it.startTime) / 1000
                Log.i(TAG, "found running item in on resume elapsed time=>${it.elapsedTime}")
                start(it)
            }
        }
        paused = false
        Log.i(TAG, "on resume")
    }

    override fun onPause() {
        super.onPause()
        paused = true
        Log.i(TAG, "on pause")
    }

    private fun start(task: Task): Job = cScope.launch {
        task.elapsedTime++
        withContext(Dispatchers.Main) { task.elapsedTimeState.value = secondsToTime(task.elapsedTime) }
        delay(1000)
        if (task.isRunning && !paused) {
            start(task)
        }
    }
}

