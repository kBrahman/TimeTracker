package time.tracker

import androidx.multidex.MultiDexApplication
import com.google.android.gms.ads.MobileAds
import io.objectbox.Box
import io.objectbox.BoxStore
import time.tracker.entity.MyObjectBox
import time.tracker.entity.Task
import time.tracker.manager.AppOpenManager


class App : MultiDexApplication() {

    internal lateinit var store: BoxStore
    private lateinit var appOpenManager: AppOpenManager
    lateinit var box: Box<Task>

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) { }
        appOpenManager = AppOpenManager(this);
        store = MyObjectBox.builder().androidContext(this).build()
        box = store.boxFor(Task::class.java)
    }
}