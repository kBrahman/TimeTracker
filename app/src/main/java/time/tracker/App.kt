package time.tracker

import android.app.Application
import io.objectbox.Box
import io.objectbox.BoxStore
import time.tracker.entity.MyObjectBox
import time.tracker.entity.Task


class App : Application() {

    private lateinit var store: BoxStore
    lateinit var box: Box<Task>

    override fun onCreate() {
        super.onCreate()
        store = MyObjectBox.builder().androidContext(this).build()
        box = store.boxFor(Task::class.java)
    }
}