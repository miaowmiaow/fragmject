package com.example.fragmject.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fragmject.core.domain.repository.UserRepository
import com.example.fragmject.core.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 导航层 ViewModel：提供当前登录用户状态，
 * 替代原先 AppNavGraph 内通过 Hilt EntryPoint 直接获取 UserRepository 的做法。
 */
@HiltViewModel
class AppNavViewModel @Inject constructor(
    userRepository: UserRepository,
) : ViewModel() {

    val user: StateFlow<User?> = userRepository.observeCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
