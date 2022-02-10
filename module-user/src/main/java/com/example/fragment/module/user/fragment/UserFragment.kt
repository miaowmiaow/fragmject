package com.example.fragment.module.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import coil.load
import coil.transform.CircleCropTransformation
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.module.user.R
import com.example.fragment.module.user.databinding.UserFragmentBinding
import com.example.fragment.module.user.model.UserViewModel
import java.io.File

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
        binding.logo.load(R.drawable.avatar_1_raster) {
            crossfade(true)
            transformations(CircleCropTransformation())
        }
        binding.logo.setOnClickListener { activity.navigation(Router.USER_LOGIN) }
        binding.username.setOnClickListener { activity.navigation(Router.USER_LOGIN) }
        binding.myCoin.setOnClickListener { activity.navigation(Router.MY_COIN) }
        binding.myCollection.setOnClickListener { activity.navigation(Router.MY_COLLECT) }
        binding.myShare.setOnClickListener { activity.navigation(Router.MY_SHARE) }
        binding.setting.setOnClickListener { activity.navigation(Router.SETTING) }
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.userResult.observe(viewLifecycleOwner) { userBean ->
            if (userBean.id.isNotBlank()) {
                if (userBean.avatar.isNotBlank()) {
                    binding.logo.load(File(userBean.avatar)) {
                        crossfade(true)
                        transformations(CircleCropTransformation())
                    }
                }
                binding.logo.setOnClickListener { activity.navigation(Router.USER_INFO) }
                binding.username.setOnClickListener { activity.navigation(Router.USER_INFO) }
                binding.username.text = "欢迎回来！${userBean.username}"
            } else {
                binding.logo.load(R.drawable.avatar_1_raster) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                }
                binding.logo.setOnClickListener { activity.navigation(Router.USER_LOGIN) }
                binding.username.setOnClickListener { activity.navigation(Router.USER_LOGIN) }
                binding.username.text = "去登录"
            }
        }
        return viewModel
    }

    override fun initLoad() {
        if (viewModel.userResult.value == null) {
            viewModel.getUser()
        }
    }

    override fun onStart() {
        super.onStart()
        //监听用户状态
        WanHelper.registerUser(viewLifecycleOwner) { viewModel.userResult.postValue(it) }
    }

}