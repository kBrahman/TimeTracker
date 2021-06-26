package time.tracker.manager

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import time.tracker.App
import time.tracker.BuildConfig
import java.util.*

class AppOpenManager(private val app: App) : Application.ActivityLifecycleCallbacks, LifecycleObserver {

    companion object {
        private const val TAG = "AppOpenManager"
        private val AD_UNIT_ID = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/3419835294"
        else "ca-app-pub-8761730220693010/6652665960"
    }

    private var appOpenAd: AppOpenAd? = null
    private var loadCallback: AppOpenAdLoadCallback? = null
    private var currentActivity: Activity? = null
    private var isShowingAd = false
    private var loadTime: Long = 0

    /** Creates and returns ad request.  */
    private val adRequest: AdRequest
        get() = AdRequest.Builder().build()

    /** Utility method that checks if ad exists and can be shown.  */
    private val isAdAvailable: Boolean
        get() = appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)

    init {
        app.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this);
    }

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    /** LifecycleObserver methods  */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        showAdIfAvailable()
        Log.d(TAG, "onStart")
    }

    private fun showAdIfAvailable() {
        // Only show ad if there is not already an app open ad currently showing
        // and an ad is available.
        if (!isShowingAd && isAdAvailable) {
            Log.d(TAG, "Will show ad.")
            val fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // Set the reference to null so isAdAvailable() returns false.
                    appOpenAd = null
                    isShowingAd = false
                    fetchAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {}
                override fun onAdShowedFullScreenContent() {
                    isShowingAd = true
                }
            }
            appOpenAd?.fullScreenContentCallback = fullScreenContentCallback
            currentActivity?.let { appOpenAd?.show(it) }
        } else {
            Log.d(TAG, "Can not show ad.")
            fetchAd()
        }
    }

    /** Request an ad  */
    fun fetchAd() {
        if (isAdAvailable) {
            return;
        }
        loadCallback = object : AppOpenAdLoadCallback() {
            /**
             * Called when an app open ad has loaded.
             *
             * @param ad the loaded app open ad.
             */
            override fun onAdLoaded(ad: AppOpenAd) {
                appOpenAd = ad
                loadTime = Date().time
            }

            /**
             * Called when an app open ad has failed to load.
             *
             * @param loadAdError the error.
             */
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.i(TAG, "ad failed=>" + loadAdError.message)
            }
        }

        AppOpenAd.load(
            app, AD_UNIT_ID, adRequest,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallback!!)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity

    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        currentActivity = null
    }
}