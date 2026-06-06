package com.michael.microbudgeting

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainActivityLaunchTest {

    @Test
    fun mainActivity_launchesWithoutCrashing() {
        Robolectric.buildActivity(MainActivity::class.java).setup().get()
    }
}
