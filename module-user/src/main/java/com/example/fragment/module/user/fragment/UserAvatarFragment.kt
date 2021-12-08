package com.example.fragment.module.user.fragment

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.fragment.library.base.dialog.PermissionDialog
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.utils.ActivityCallback
import com.example.fragment.library.base.utils.ActivityResultHelper.requestStorage
import com.example.fragment.library.base.utils.ActivityResultHelper.startForResult
import com.example.fragment.library.base.utils.PermissionsCallback
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.module.user.R
import com.example.fragment.module.user.databinding.FragmentUserAvatarBinding
import com.example.fragment.module.user.model.UserViewModel
import com.example.miaow.picture.dialog.EditorFinishCallback
import com.example.miaow.picture.dialog.PictureEditorDialog
import com.example.miaow.picture.utils.AlbumUtils.getImagePath

class UserAvatarFragment : RouterFragment() {

    private val viewModel: UserViewModel by activityViewModels()
    private var _binding: FragmentUserAvatarBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserAvatarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun initView() {
        binding.image.setImageResource(R.drawable.avatar_1_raster)
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
        viewModel.localAvatarResult.observe(viewLifecycleOwner) { path ->
            if (!path.isNullOrBlank()) {
                BitmapFactory.decodeFile(path, BitmapFactory.Options())?.let { bitmap ->
                    binding.image.setImageBitmap(bitmap)
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
                    viewModel.localAvatarResult.postValue(path)
                    WanHelper.setLocalAvatar(path)
                }
            })
            .show(childFragmentManager)
    }

}