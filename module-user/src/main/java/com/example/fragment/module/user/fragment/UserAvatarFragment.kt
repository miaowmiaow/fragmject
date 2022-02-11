package com.example.fragment.module.user.fragment

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import coil.load
import com.example.fragment.library.base.dialog.PermissionDialog
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.utils.ActivityCallback
import com.example.fragment.library.base.utils.ActivityResultHelper.requestStorage
import com.example.fragment.library.base.utils.ActivityResultHelper.startForResult
import com.example.fragment.library.base.utils.PermissionsCallback
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.library.common.model.UserViewModel
import com.example.fragment.module.user.databinding.UserAvatarFragmentBinding
import com.example.miaow.picture.dialog.EditorFinishCallback
import com.example.miaow.picture.dialog.PictureEditorDialog
import com.example.miaow.picture.utils.AlbumUtils.getImagePath
import java.io.File

class UserAvatarFragment : RouterFragment() {

    private val viewModel: UserViewModel by activityViewModels()
    private var _binding: UserAvatarFragmentBinding? = null
    private val binding get() = _binding!!

    private var userBean = UserBean()

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
        binding.album.setOnClickListener {
            activity.requestStorage(object : PermissionsCallback {
                override fun allow() {
                    openAlbum()
                }

                override fun deny() {
                    PermissionDialog.alert(activity, "存储")
                }
            })
        }
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.userResult.observe(viewLifecycleOwner) {
            userBean = it
            if (userBean.avatar.isNotBlank()) {
                binding.image.load(File(userBean.avatar))
            }
        }
        return viewModel
    }

    override fun initLoad() {
        if (viewModel.userResult.value == null) {
            viewModel.getUser()
        }
    }

    private fun openAlbum() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        activity.startForResult(intent, object : ActivityCallback {
            override fun onActivityResult(resultCode: Int, data: Intent?) {
                data?.data?.let { uri ->
                    pictureEditor(activity.getImagePath(uri))
                }
            }
        })
    }

    private fun pictureEditor(path: String) {
        PictureEditorDialog.newInstance()
            .setBitmapPath(path)
            .setEditorFinishCallback(object : EditorFinishCallback {
                override fun onFinish(path: String) {
                    userBean.avatar = path
                    viewModel.updateUser(userBean)
                }
            })
            .show(childFragmentManager)
    }

}