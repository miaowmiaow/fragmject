package com.example.fragment.module.user.fragment

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.fragment.library.base.bus.SharedFlowBus
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.module.user.databinding.FragmentUserBinding

class UserFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): UserFragment {
            return UserFragment()
        }
    }

    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        binding.username.text = "去登录"
        binding.logo.setOnClickListener { activity.navigation(Router.LOGIN) }
        binding.username.setOnClickListener { activity.navigation(Router.LOGIN) }
        binding.myCoin.setOnClickListener { activity.navigation(Router.MY_COIN) }
        binding.myCollection.setOnClickListener { activity.navigation(Router.MY_COLLECT) }
        binding.myShare.setOnClickListener { activity.navigation(Router.MY_SHARE) }
        binding.setting.setOnClickListener { activity.navigation(Router.SETTING) }
    }

    override fun initViewModel() {
    }

    override fun onLoad() {
        WanHelper.getUser().observe(this) { userBean ->
            updateView(userBean)
        }
        WanHelper.getAvatar().observe(viewLifecycleOwner) { path ->
            BitmapFactory.decodeFile(path, BitmapFactory.Options())?.let { bitmap ->
                binding.logo.setImageBitmap(bitmap)
            }
        }
        SharedFlowBus.onSticky(UserBean::class.java).observe(this) { userBean ->
            updateView(userBean)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateView(userBean: UserBean) {
        if (userBean.id.isNotBlank()) {
            binding.logo.setOnClickListener { activity.navigation(Router.AVATAR) }
            binding.username.setOnClickListener { activity.navigation(Router.AVATAR) }
            binding.username.text = "欢迎回来！${userBean.username}"
        }
    }

}