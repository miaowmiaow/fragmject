package com.example.fragment.user.fragment

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.fragment.library.base.dialog.OnPictureEditorListener
import com.example.fragment.library.base.dialog.PermissionDialog
import com.example.fragment.library.base.dialog.PictureEditorDialog
import com.example.fragment.library.base.utils.ActivityCallback
import com.example.fragment.library.base.utils.ActivityResultHelper.requestStoragePermissions
import com.example.fragment.library.base.utils.ActivityResultHelper.startForResult
import com.example.fragment.library.base.utils.PermissionsCallback
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.user.databinding.FragmentAvatarBinding

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
        binding.black.setOnClickListener { baseActivity.onBackPressed() }
        binding.open.setOnClickListener { openAlbum() }
    }

    private fun openAlbum() {
        baseActivity.requestStoragePermissions(object: PermissionsCallback{
            override fun allow() {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
                baseActivity.startForResult(intent, object : ActivityCallback{
                    override fun onActivityResult(resultCode: Int, data: Intent?) {
                        data?.data?.let {uri->
                            var imagePath = ""
                            Log.d("TAG", "handleImageOnKitKat: uri is $uri")
                            Log.d("TAG", "handleImageOnKitKat : " + uri.authority)
                            Log.d("TAG", "handleImageOnKitKat urlPath: " + uri.scheme)
                            if (DocumentsContract.isDocumentUri(baseActivity, uri)) {
                                // 如果是document类型的Uri，则通过document id处理
                                val docId = DocumentsContract.getDocumentId(uri)
                                Log.d("TAG", "handleImageOnKitKat: docId$docId")
                                if ("com.android.providers.media.documents" == uri.authority) {
                                    val id =
                                        docId.split(":".toRegex()).toTypedArray()[1] // 解析出数字格式的id
                                    val selection = MediaStore.Images.Media._ID + "=" + id
                                    imagePath = getImagePath(
                                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                        selection
                                    )
                                } else if ("com.android.providers.downloads.documents" == uri.getAuthority()) {
                                    val contentUri: Uri = ContentUris.withAppendedId(
                                        Uri.parse("content://downloads/public_downloads"),
                                        docId.toLong()
                                    )
                                    imagePath = getImagePath(contentUri, "")
                                }
                            } else if ("content".equals(uri.scheme, ignoreCase = true)) {
                                // 如果是content类型的Uri，则使用普通方式处理
                                imagePath = getImagePath(uri, "")
                            } else if ("file".equals(uri.scheme, ignoreCase = true)) {
                                // 如果是file类型的Uri，直接获取图片路径即可
                                imagePath = uri.path.toString()
                            }
                            PictureEditorDialog.newInstance()
                                .setBitmapResource(imagePath)
                                .setOnPictureEditorListener(object : OnPictureEditorListener {
                                    override fun onPicture(pathName: String) {

                                    }
                                })
                                .show(childFragmentManager)
                        }
                    }

                })
            }

            override fun deny() {
                PermissionDialog.alert(baseActivity, "存储")
            }

        } )
    }

    private fun getImagePath(uri: Uri, selection: String): String {
        var path = ""
        // 通过Uri和selection来获取真实的图片路径
        val cursor = baseActivity.contentResolver.query(uri, null, selection, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
            }
            cursor.close()
        }
        Log.e("TAG", "getImagePath: $path")
        return path
    }
}