package com.golfmatch.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Hiltのエントリポイント（技術設計書 2章）。
 */
@HiltAndroidApp
class GolfMatchApplication : Application()
