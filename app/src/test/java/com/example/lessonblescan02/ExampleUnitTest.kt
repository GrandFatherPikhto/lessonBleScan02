package com.example.lessonblescan02

import android.os.ParcelUuid
import org.junit.After
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import java.util.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
    }

    @Test
    fun addition_isCorrect() {
        val uuid = ParcelUuid(UUID.randomUUID())
        println("UUID: $uuid")
        assertEquals(4, 2 + 2)
    }
}