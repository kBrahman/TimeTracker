package time.tracker.util

import androidx.compose.foundation.lazy.LazyListItemInfo

fun secondsToTime(seconds: Int): String {
    val h = seconds / 3600
    val m = seconds % 3600 / 60
    val s = seconds % 60
    return "$h:" + (if (m < 10) "0$m:" else "$m:") + if (s < 10) "0$s" else s
}

val LazyListItemInfo.offsetEnd: Int
    get() = this.offset + this.size

fun <T> MutableList<T>.move(from: Int, to: Int) {
    if (from == to) return
    val element = removeAt(from) ?: return
    add(to, element)
}