package time.tracker

import androidx.multidex.MultiDexApplication
import com.google.android.gms.ads.MobileAds
import io.objectbox.Box
import time.tracker.entity.MyObjectBox
import time.tracker.entity.Task
import time.tracker.manager.AppOpenManager


class App : MultiDexApplication() {

    private lateinit var appOpenManager: AppOpenManager
    private lateinit var box: Box<Task>

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) { }
        appOpenManager = AppOpenManager(this);
        box = MyObjectBox.builder().androidContext(this).build().boxFor(Task::class.java)
    }

    fun getBox() = box
}