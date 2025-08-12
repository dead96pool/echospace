package com.chani.echospace.utils

import android.util.Log
import kotlin.system.measureTimeMillis

object PerformanceLogger {
    const val TAG = "EchoSpace_Performance"

    inline fun <T> measureAndLog(operation: String, block: () -> T): T {
        var result: T
        val time = measureTimeMillis {
            result = block()
        }
        Log.d(TAG, "$operation took ${time}ms")
        if (time > 100) {
            Log.w(TAG, "SLOW OPERATION: $operation took ${time}ms")
        }
        return result
    }

    fun logStep(step: String) {
        Log.d(TAG, "Step: $step at ${System.currentTimeMillis()}")
    }

    fun logFileCount(operation: String, count: Int) {
        Log.d(TAG, "$operation processed $count files")
    }
}
