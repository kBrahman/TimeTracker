package zig.tic

import android.app.Application
import androidx.multidex.MultiDex
import com.google.android.gms.ads.MobileAds
import io.objectbox.Box
import zig.tic.tac.entity.MyObjectBox
import zig.tic.tac.entity.Task
import zig.tic.tac.manager.AppOpenManager


class App : Application() {

    private lateinit var appOpenManager: AppOpenManager
    private lateinit var box: Box<Task>

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) { }
        appOpenManager = AppOpenManager(this);
        MultiDex.install(this)
        box = MyObjectBox.builder().androidContext(this).build().boxFor(Task::class.java)
    }

    fun getBox() = box
}