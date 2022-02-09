package com.example.fragment.module.user.fragment

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.module.user.databinding.UserFragmentBinding
import com.example.fragment.module.user.model.UserViewModel

class UserFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): UserFragment {
            return UserFragment()
        }
    }

    private val viewModel: UserViewModel by activityViewModels()
    private var _binding: UserFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = UserFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        binding.logo.setOnClickListener { activity.navigation(Router.USER_LOGIN) }
        binding.username.setOnClickListener { activity.navigation(Router.USER_LOGIN) }
        binding.myCoin.setOnClickListener { activity.navigation(Router.MY_COIN) }
        binding.myCollection.setOnClickListener { activity.navigation(Router.MY_COLLECT) }
        binding.myShare.setOnClickListener { activity.navigation(Router.MY_SHARE) }
        binding.setting.setOnClickListener { activity.navigation(Router.SETTING) }
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.localAvatarResult.observe(viewLifecycleOwner) { path ->
            if (!path.isNullOrBlank()) {
                BitmapFactory.decodeFile(path, BitmapFactory.Options())?.let { bitmap ->
                    binding.logo.setImageBitmap(bitmap)
                }
            }
        }
        return viewModel
    }

    override fun initLoad() {
        if (viewModel.localAvatarResult.value == null) {
            viewModel.getLocalAvatar()
        }
    }

    override fun onStart() {
        super.onStart()
        //监听用户状态
        WanHelper.registerUser(this) { updateView(it) }
    }

    private fun updateView(userBean: UserBean) {
        if (userBean.id.isNotBlank()) {
            binding.logo.setOnClickListener { activity.navigation(Router.USER_AVATAR) }
            binding.username.setOnClickListener { activity.navigation(Router.USER_AVATAR) }
            binding.username.text = "欢迎回来！${userBean.username}"
        } else {
            binding.logo.setOnClickListener { activity.navigation(Router.USER_LOGIN) }
            binding.username.setOnClickListener { activity.navigation(Router.USER_LOGIN) }
            binding.username.text = "去登录"
        }
    }

}