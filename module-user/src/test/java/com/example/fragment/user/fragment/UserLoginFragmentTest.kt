package com.example.fragment.user.fragment

import com.example.fragment.module.user.fragment.UserLoginFragment
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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