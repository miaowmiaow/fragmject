package com.example.fragment.user.fragment

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.fragment.library.base.dialog.PermissionDialog
import com.example.fragment.library.base.utils.ActivityCallback
import com.example.fragment.library.base.utils.ActivityResultHelper.requestStoragePermissions
import com.example.fragment.library.base.utils.ActivityResultHelper.startForResult
import com.example.fragment.library.base.utils.PermissionsCallback
import com.example.fragment.library.base.utils.SPUtil
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.user.R
import com.example.fragment.module.user.databinding.FragmentAvatarBinding
import com.example.miaow.picture.dialog.EditorFinishCallback
import com.example.miaow.picture.dialog.PictureEditorDialog
import com.example.miaow.picture.utils.AlbumUtils.getImagePath

class AvatarFragment : RouterFragment() {

    private var _binding: FragmentAvatarBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAvatarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
    }

    private fun setupView() {
        binding.image.setImageResource(R.drawable.ic_logo)
        val path = SPUtil.getString(Keys.AVATAR)
        BitmapFactory.decodeFile(path, BitmapFactory.Options())?.let { bitmap ->
            binding.image.setImageBitmap(bitmap)
        }
        binding.black.setOnClickListener { baseActivity.onBackPressed() }
        binding.album.setOnClickListener {
            baseActivity.requestStoragePermissions(object : PermissionsCallback {
                override fun allow() {
                    openAlbum()
                }

                override fun deny() {
                    PermissionDialog.alert(baseActivity, "存储")
                }
            })
        }
    }

    private fun openAlbum() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        baseActivity.startForResult(intent, object : ActivityCallback {
            override fun onActivityResult(resultCode: Int, data: Intent?) {
                data?.data?.let { uri ->
                    pictureEditor(baseActivity.getImagePath(uri))
                }
            }
        })
    }

    private fun pictureEditor(path: String) {
        PictureEditorDialog.newInstance()
            .setBitmapPath(path)
            .setEditorFinishCallback(object : EditorFinishCallback {
                override fun onFinish(path: String) {
                    val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options())
                    binding.image.setImageBitmap(bitmap)
                    SPUtil.setString(Keys.AVATAR, path)
                }
            })
            .show(childFragmentManager)
    }

}