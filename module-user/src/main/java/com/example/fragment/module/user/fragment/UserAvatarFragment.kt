package com.example.fragment.module.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.fragment.library.base.dialog.PermissionDialog
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.utils.PermissionsCallback
import com.example.fragment.library.base.utils.loadCircleCrop
import com.example.fragment.library.base.utils.requestStorage
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.user.R
import com.example.fragment.module.user.databinding.UserAvatarFragmentBinding
import com.example.fragment.module.user.model.UserViewModel
import com.example.miaow.picture.selector.bean.MediaBean
import com.example.miaow.picture.selector.dialog.PictureSelectorCallback
import com.example.miaow.picture.selector.dialog.PictureSelectorDialog

class UserAvatarFragment : RouterFragment() {

    private val viewModel: UserViewModel by activityViewModels()
    private var _binding: UserAvatarFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = UserAvatarFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        binding.black.setOnClickListener { activity.onBackPressed() }
        binding.image.loadCircleCrop(R.drawable.avatar_1_raster)
        binding.album.setOnClickListener {
            activity.requestStorage(object : PermissionsCallback {
                override fun allow() {
                    PictureSelectorDialog.newInstance()
                        .setPictureSelectorCallback(object : PictureSelectorCallback {
                            override fun onSelectedData(data: List<MediaBean>) {
                                if (data.isNullOrEmpty()) {
                                    return
                                }
                                viewModel.getUserBean().let {
                                    it.avatar = data[0].uri.toString()
                                    viewModel.updateUserBean(it)
                                }
                            }
                        })
                        .show(childFragmentManager)
                }

                override fun deny() {
                    PermissionDialog.alert(activity, "存储")
                }
            })
        }
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.userResult().observe(viewLifecycleOwner) {
            if (it.avatar.isNotBlank()) {
                binding.image.loadCircleCrop(it.avatar)
            }
        }
        return viewModel
    }

}