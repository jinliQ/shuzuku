package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.BridgeManager
import com.example.model.BridgeStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Shizuku Bridge", appName)
    }

    @Test
    fun `verify default bridge manager state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        BridgeManager.init(context)
        val status = BridgeManager.status.value
        assertEquals(19870, status.port)
        assertEquals("127.0.0.1", status.host)
        assertNotNull(status.token)
    }
}
