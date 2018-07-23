package zig.tic.tac.util

fun secondsToTime(seconds: Int): String {
    val h = seconds / 3600
    val m = seconds % 3600 / 60
    val s = seconds % 60
    return "$h:" + (if (m < 10) "0$m:" else "$m:") + if (s < 10) "0$s" else s
}

fun secondsToHoursAndMinutes(seconds: Int): String {
    val h = seconds / 3600
    val m = seconds % 3600 / 60
    val s = seconds % 60
    return " $h:" + (if (m < 10) "0$m" else "$m") + " min."
}