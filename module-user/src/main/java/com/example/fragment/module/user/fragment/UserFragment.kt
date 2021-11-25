package com.example.fragment.module.user.fragment

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.fragment.library.base.bus.SharedFlowBus
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.common.bean.EventBean
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.user.databinding.FragmentUserBinding
import com.example.fragment.module.user.model.UserViewModel

class UserFragment : RouterFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): UserFragment {
            return UserFragment()
        }
    }

    private val viewModel: UserViewModel by activityViewModels()
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
        binding.logo.setOnClickListener { activity.navigation(Router.USER_LOGIN) }
        binding.username.setOnClickListener { activity.navigation(Router.USER_LOGIN) }
        binding.myCoin.setOnClickListener { activity.navigation(Router.MY_COIN) }
        binding.myCollection.setOnClickListener { activity.navigation(Router.MY_COLLECT) }
        binding.myShare.setOnClickListener { activity.navigation(Router.MY_SHARE) }
        binding.setting.setOnClickListener { activity.navigation(Router.SETTING) }
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.avatarResult.observe(viewLifecycleOwner) { path ->
            if(!path.isNullOrBlank()){
                BitmapFactory.decodeFile(path, BitmapFactory.Options())?.let { bitmap ->
                    binding.logo.setImageBitmap(bitmap)
                }
            }
        }
        viewModel.userResult.observe(viewLifecycleOwner) { userBean ->
            updateView(userBean)
        }
        return viewModel
    }

    override fun initLoad() {
        if (viewModel.avatarResult.value == null) {
            viewModel.getAvatar()
        }
        if (viewModel.userResult.value == null) {
            viewModel.getUser()
        }
        SharedFlowBus.onSticky(EventBean::class.java).observe(this) { eventBean ->
            if (eventBean.key == Keys.AVATAR) {
                BitmapFactory.decodeFile(eventBean.value, BitmapFactory.Options())?.let { bitmap ->
                    binding.logo.setImageBitmap(bitmap)
                }
            }
        }
        SharedFlowBus.onSticky(UserBean::class.java).observe(this) { userBean ->
            updateView(userBean)
        }
    }

    private fun updateView(userBean: UserBean) {
        if (userBean.id.isNotBlank()) {
            binding.logo.setOnClickListener { activity.navigation(Router.USER_AVATAR) }
            binding.username.setOnClickListener { activity.navigation(Router.USER_AVATAR) }
            binding.username.text = "欢迎回来！${userBean.username}"
        }
    }

}