package com.example.fragment.module.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.utils.loadCircleCrop
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.user.R
import com.example.fragment.module.user.databinding.UserInfoFragmentBinding
import com.example.fragment.module.user.dialog.BirthdayDialog
import com.example.fragment.module.user.dialog.SexDialog
import com.example.fragment.module.user.model.UserViewModel

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
        binding.black.setOnClickListener { activity.onBackPressed() }
        binding.avatar.setOnClickListener { activity.navigation(Router.USER_AVATAR) }
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
            activity.navigation(Router.USER_CITY)
        }
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.userResult.observe(viewLifecycleOwner) {
            if (it.avatar.isNotBlank()) {
                binding.avatarImg.loadCircleCrop(it.avatar)
            }
            setUserInfo(binding.username, it.username)
            setUserInfo(binding.sexInfo, it.sex)
            setUserInfo(binding.birthdayInfo, it.birthday)
            setUserInfo(binding.cityInfo, it.city)
        }
        return viewModel
    }

    override fun initLoad() {
        if (viewModel.userResult.value == null) {
            viewModel.getUser()
        }
    }

    private fun setUserInfo(view: TextView, info: String) {
        var text = "保密"
        if (info.isNotBlank()) {
            text = info
        }
        view.text = text
    }

}