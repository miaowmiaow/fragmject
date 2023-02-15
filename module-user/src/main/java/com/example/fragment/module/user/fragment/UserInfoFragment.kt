package com.example.fragment.module.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.fragment.library.base.vm.BaseViewModel
import com.example.fragment.library.base.utils.loadCircleCrop
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.user.R
import com.example.fragment.module.user.databinding.UserInfoFragmentBinding
import com.example.fragment.module.user.dialog.BirthdayDialog
import com.example.fragment.module.user.dialog.SexDialog
import com.example.fragment.module.user.vm.UserViewModel
import kotlinx.coroutines.launch

/**
 * 纯粹为以下知识点服务：
 * 1、ViewModels 在 Fragment 之间共享数据
 * 2、DialogFragment 的运用
 */
class UserInfoFragment : RouterFragment() {

    private val viewModel: UserViewModel by activityViewModels()
    private var _binding: UserInfoFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = UserInfoFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        binding.black.setOnClickListener { onBackPressed() }
        binding.avatar.setOnClickListener { navigation(Router.USER_AVATAR) }
        binding.avatarImg.loadCircleCrop(R.drawable.avatar_1_raster)
        binding.sex.setOnClickListener {
            SexDialog.newInstance()
                .setSex(binding.sexInfo.text.toString())
                .setSexListener(object : SexDialog.SexListener {
                    override fun onSex(sex: String) {
                        viewModel.getUserBean().let {
                            it.sex = sex
                            viewModel.updateUserBean(it)
                        }
                    }
                })
                .show(childFragmentManager)
        }
        binding.birthday.setOnClickListener {
            BirthdayDialog.newInstance()
                .setBirthday(binding.birthdayInfo.text.toString())
                .setBirthdayListener(object : BirthdayDialog.BirthdayListener {
                    override fun onBirthday(time: String) {
                        viewModel.getUserBean().let {
                            it.birthday = time
                            viewModel.updateUserBean(it)
                        }
                    }
                })
                .show(childFragmentManager)
        }
        binding.city.setOnClickListener {
            navigation(Router.USER_CITY)
        }
    }

    override fun initViewModel(): BaseViewModel {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect {
                    if (it.getUserBean().avatar.isNotBlank()) {
                        binding.avatarImg.loadCircleCrop(it.getUserBean().avatar)
                    }
                    setUserInfo(binding.username, it.getUserBean().username)
                    setUserInfo(binding.sexInfo, it.getUserBean().sex)
                    setUserInfo(binding.birthdayInfo, it.getUserBean().birthday)
                    setUserInfo(binding.cityInfo, it.getUserBean().city)
                }
            }
        }
        return viewModel
    }

    private fun setUserInfo(view: TextView, info: String) {
        var text = "保密"
        if (info.isNotBlank()) {
            text = info
        }
        view.text = text
    }

}