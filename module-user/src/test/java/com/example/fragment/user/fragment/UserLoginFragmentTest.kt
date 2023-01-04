package com.example.fragment.user.fragment

import com.example.fragment.module.user.fragment.UserLoginFragment
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

//Gradle 任务运行
//Gradle 运行时，只能运行所有测试。

//javaTest
//通过运行 ./gradlew testDebugUnitTest 任务即可启动测试
//运行完成后的报告位于 build/reports/tests 目录。

//androidTest
//通过运行 ./gradlew connectedDebugAndroidTest 任务即可启动测试
//运行完成后的报告位于 build/reports/androidTests 目录。

class UserLoginFragmentTest {

    private lateinit var userLoginFragment: UserLoginFragment

    @Before
    fun init() {
        userLoginFragment = UserLoginFragment()
    }

    @Test
    fun checkParameter_isNotBlank_ReturnsTrue() {
        assertTrue(userLoginFragment.checkParameter("123", "123"))
    }

    @Test
    fun checkParameter_usernameBlank_ReturnsFalse() {
        assertFalse(userLoginFragment.checkParameter(" ", "123"))
    }

    @Test
    fun checkParameter_passwordBlank_ReturnsFalse() {
        assertFalse(userLoginFragment.checkParameter("123", " "))
    }

}