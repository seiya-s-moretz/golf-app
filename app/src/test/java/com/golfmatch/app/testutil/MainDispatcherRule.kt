package com.golfmatch.app.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * ViewModelユニットテスト用のJUnitルール。`Dispatchers.Main`を[UnconfinedTestDispatcher]に差し替える。
 *
 * [UnconfinedTestDispatcher]を使うのは、本プロジェクトの各ViewModelが`viewModelScope.launch { ... }`
 * （`Dispatchers.Main.immediate`相当）内で状態更新を行っており、実機では呼び出しスレッドから
 * 最初のsuspendポイントまで同期的に実行されるため（多重操作防止のガード等の検証に必要）。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
