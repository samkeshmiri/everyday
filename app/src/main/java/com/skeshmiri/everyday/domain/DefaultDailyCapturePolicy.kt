package com.skeshmiri.everyday.domain

import com.skeshmiri.everyday.model.DailyPhoto

class DefaultDailyCapturePolicy : DailyCapturePolicy {
    override fun canCapture(todayPhoto: DailyPhoto?): Boolean = todayPhoto == null
}

