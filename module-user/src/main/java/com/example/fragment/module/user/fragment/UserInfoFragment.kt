package com.example.fragment.module.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import coil.load
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.library.common.model.UserViewModel
import com.example.fragment.module.user.databinding.UserInfoFragmentBinding
import com.example.fragment.module.user.dialog.BirthdayDialog
import com.example.fragment.module.user.dialog.CityDialog
import com.example.fragment.module.user.dialog.SexDialog
import java.io.File

/**
 * 纯粹为以下知识点服务：
 * 1、ViewModels 在 Fragment 之间共享数据
 * 2、DialogFragment 的运用
 */
class UserInfoFragment : RouterFragment() {

    private val viewModel: UserViewModel by activityViewModels()
    private var _binding: UserInfoFragmentBinding? = null
    private val binding get() = _binding!!

    private var userBean = UserBean()

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
        binding.sex.setOnClickListener {
            SexDialog.newInstance()
                .setSex(binding.sexInfo.text.toString())
                .setSexListener(object : SexDialog.SexListener {
                    override fun onSex(sex: String) {
                        userBean.sex = sex
                        viewModel.updateUser(userBean)
                    }
                })
                .show(childFragmentManager)
        }
        binding.birthday.setOnClickListener {
            BirthdayDialog.newInstance()
                .setBirthday(binding.birthdayInfo.text.toString())
                .setBirthdayListener(object : BirthdayDialog.BirthdayListener {
                    override fun onBirthday(time: String) {
                        userBean.birthday = time
                        viewModel.updateUser(userBean)
                    }
                })
                .show(childFragmentManager)
        }
        binding.city.setOnClickListener {
            CityDialog.newInstance()
                .setCityListener(object : CityDialog.CityListener {
                    override fun onCity(name: String) {
                        userBean.city = name
                        viewModel.updateUser(userBean)
                    }
                })
                .show(childFragmentManager)
        }
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.userResult.observe(viewLifecycleOwner) {
            userBean = it
            if (userBean.avatar.isNotBlank()) {
                binding.avatarImg.load(File(userBean.avatar))
            }
            setUserInfo(binding.username, userBean.username)
            setUserInfo(binding.sexInfo, userBean.sex)
            setUserInfo(binding.birthdayInfo, userBean.birthday)
            setUserInfo(binding.cityInfo, userBean.city)
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