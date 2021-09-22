package com.example.fragment.user.fragment

import android.content.ContentUris
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.fragment.library.base.dialog.EditorFinishCallback
import com.example.fragment.library.base.dialog.PermissionDialog
import com.example.fragment.library.base.dialog.PictureEditorDialog
import com.example.fragment.library.base.utils.ActivityCallback
import com.example.fragment.library.base.utils.ActivityResultHelper.requestStoragePermissions
import com.example.fragment.library.base.utils.ActivityResultHelper.startForResult
import com.example.fragment.library.base.utils.PermissionsCallback
import com.example.fragment.library.base.utils.SPUtil
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.user.R
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
        binding.image.setImageResource(R.drawable.ic_logo)
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
                    pictureEditor(getImagePath(uri))
                }
            }
        })
    }

    private fun pictureEditor(path: String) {
        PictureEditorDialog.newInstance()
            .setBitmapResource(path)
            .setEditorFinishCallback(object : EditorFinishCallback {
                override fun onFinish(path: String) {
                    val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options())
                    binding.image.setImageBitmap(bitmap)
                    SPUtil.setString(Keys.AVATAR, path)
                }
            })
            .show(childFragmentManager)
    }

    private fun getImagePath(uri: Uri): String {
        var imagePath = ""
        if (DocumentsContract.isDocumentUri(baseActivity, uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            if ("com.android.providers.media.documents" == uri.authority) {
                val id = docId.split(":".toRegex()).toTypedArray()[1]
                val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val selection = MediaStore.Images.Media._ID + "=" + id
                imagePath = contentResolverQuery(contentUri, selection)
            } else if ("com.android.providers.downloads.documents" == uri.authority) {
                val uriString = "content://downloads/public_downloads"
                val contentUri = ContentUris.withAppendedId(Uri.parse(uriString), docId.toLong())
                imagePath = contentResolverQuery(contentUri)
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            imagePath = contentResolverQuery(uri)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            imagePath = uri.path.toString()
        }
        return imagePath
    }

    private fun contentResolverQuery(uri: Uri, selection: String = ""): String {
        val contentResolver = baseActivity.contentResolver
        val cursor = contentResolver.query(uri, null, selection, null, null)
        var path = ""
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
            }
            cursor.close()
        }
        return path
    }
}