package zig.tic

import android.app.Application
import io.objectbox.Box
import zig.tic.tac.entity.MyObjectBox
import zig.tic.tac.entity.Task

class App : Application() {

    private lateinit var box: Box<Task>

    override fun onCreate() {
        super.onCreate()
        box = MyObjectBox.builder().androidContext(this).build().boxFor(Task::class.java)
    }

    fun getBox() = box
}