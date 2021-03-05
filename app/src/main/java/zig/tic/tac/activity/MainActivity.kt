package zig.tic.tac.activity

import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import io.objectbox.Box
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import zig.tic.App
import zig.tic.tac.R
import zig.tic.tac.adapter.TaskAdapter
import zig.tic.tac.entity.Task
import zig.tic.tac.util.secondsToHoursAndMinutes
import zig.tic.tac.util.secondsToTime


class MainActivity : AppCompatActivity(), Runnable {

    companion object {
        private val TAG: String = MainActivity::class.java.simpleName
    }

    private lateinit var timeMenuItem: MenuItem
    private lateinit var playPauseMenuItem: MenuItem
    private lateinit var handler: Handler
    private var seconds = 0
    private var currentTask: Task? = null
    private var isPaused = true
    private var tasks = mutableListOf<Task>()
    private lateinit var box: Box<Task>
    private lateinit var delMenuItem: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column {
                tasks.forEach {

                }
            }
        }
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener {
            tasks.add(Task(title = getString(R.string.new_task)))
        }
        rvTasks.setHasFixedSize(true)
        handler = Handler(Looper.myLooper()!!)
        box = (application as App).getBox()

        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                adView.visibility = VISIBLE
            }
        }
        adView.loadAd(AdRequest.Builder().build())
    }

    fun start(txt: String, taskToContinue: Task?) {
        title = txt
        timeMenuItem.isVisible = true
        playPauseMenuItem.isVisible = true
        delMenuItem.isVisible = true
        if (currentTask != null) {
            currentTask?.setElapsedTime(seconds)
            tasks.add(0, currentTask!!)
            box.put(currentTask)
            if (rvTasks.adapter == null) {
                rvTasks.adapter = TaskAdapter(tasks)
            } else {
                rvTasks.adapter?.notifyItemInserted(0)
            }
        }
        if (taskToContinue == null) {
            currentTask = Task(txt, System.currentTimeMillis())
            seconds = 0
        } else {
            currentTask = taskToContinue
            val i = tasks.indexOf(taskToContinue)
            tasks.removeAt(i)
            box.remove(taskToContinue)
            rvTasks.adapter?.notifyItemRemoved(i)
            seconds = taskToContinue.getElapsedTime()
        }
        handler.removeCallbacks(this)
        isPaused = false
        playPauseMenuItem.icon = getDrawable()
        handler.postDelayed(this, 1000)
    }

    override fun run() {
        if (!isPaused) {
            timeMenuItem.title = secondsToTime(++seconds)
            if (tasks.isNotEmpty()) {
                tvTotal.setText(R.string.total)
                val sum = seconds + tasks.sumBy { it.getElapsedTime() }
                tvTotal.append(secondsToHoursAndMinutes(sum))
                tvTotal.visibility = VISIBLE
            } else {
                tvTotal.visibility = GONE
            }
            handler.postDelayed(this, 1000)
        }
    }

    fun pause(item: MenuItem) {
        isPaused = if (isPaused) {
            handler.postDelayed(this, 1000)
            false
        } else true

        item.icon = getDrawable()
    }

    private fun getDrawable(): Drawable? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ContextCompat.getDrawable(
                this,
                if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause
            )
        } else {
            ContextCompat.getDrawable(
                this,
                if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        timeMenuItem = menu.findItem(R.id.action_time)
        playPauseMenuItem = menu.findItem(R.id.action_play_pause)
        delMenuItem = menu.findItem(R.id.action_delete)
        val list = box.all
        if (list.isNotEmpty()) {
            val sum = list.sumBy { it.getElapsedTime() }
            tvTotal.append(secondsToHoursAndMinutes(sum))
            tasks.addAll(list)
            currentTask = tasks.find { it.isRunning() }
            if (currentTask != null) {
                currentTask?.setIsRunning(false)
                title = currentTask?.title
                timeMenuItem.title =
                    secondsToTime(currentTask?.getElapsedTime()!! + getTimeClosed())
                timeMenuItem.isVisible = true
                playPauseMenuItem.isVisible = true
                delMenuItem.isVisible = true
                tasks.remove(currentTask!!)
                seconds = currentTask!!.getElapsedTime() + getTimeClosed()
                if (currentTask?.getTimeClosed()!! > 0) {
                    isPaused = false
                    handler.postDelayed(this, 1000)
                    currentTask?.setTimeClosed(0)
                }
                playPauseMenuItem.icon = getDrawable()
            }
            rvTasks.adapter = TaskAdapter(tasks)

        } else {
            tvTotal.visibility = GONE
        }
        return true
    }

    fun delete(item: MenuItem) {
        title = getString(R.string.app_name)
        playPauseMenuItem.isVisible = false
        timeMenuItem.isVisible = false
        item.isVisible = false
        if (currentTask?.getId() != null) {
            box.remove(currentTask)
        }
        isPaused = true
        handler.removeCallbacks(this)
        currentTask = null
        if (tasks.size > 1) {
            tvTotal.setText(R.string.total)
            tvTotal.append(secondsToHoursAndMinutes(tasks.sumBy { it.getElapsedTime() }))
        } else {
            tvTotal.visibility = GONE
        }
    }

    private fun getTimeClosed(): Int =
        if (currentTask?.getTimeClosed() == 0L) 0 else (System.currentTimeMillis() - currentTask?.getTimeClosed()!!).div(
            1000
        ).toInt()


    override fun onPause() {
        super.onPause()
        if (currentTask != null && !tasks.contains(currentTask!!)) {
            currentTask?.setIsRunning(true)
            currentTask?.setElapsedTime(seconds)
            if (!isPaused) currentTask?.setTimeClosed(System.currentTimeMillis())
            box.put(currentTask)
            handler.removeCallbacks(this)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isPaused && currentTask != null) {
            seconds += getTimeClosed()
            timeMenuItem.title = secondsToTime(seconds)
            handler.postDelayed(this, 1000)
        }
    }
}
