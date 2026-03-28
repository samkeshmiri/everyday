package com.skeshmiri.everyday.domain

import com.skeshmiri.everyday.model.DailyPhoto

fun interface DailyCapturePolicy {
    fun canCapture(todayPhoto: DailyPhoto?): Boolean
}

