package com.example.miaow.picture.selector.dialog

import android.content.ContentUris
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.example.fragment.library.base.R
import com.example.fragment.library.base.adapter.BaseAdapter
import com.example.fragment.library.base.dialog.FullDialog
import com.example.miaow.picture.databinding.AlbumSelectorDialogBinding
import com.example.miaow.picture.selector.adapter.AlbumAdapter
import com.example.miaow.picture.selector.bean.Album
import com.example.miaow.picture.selector.bean.Bucket
import com.example.miaow.picture.selector.pop.BucketSelectorPopupWindow
import com.example.miaow.picture.utils.getImagePath

class AlbumSelectorDialog : FullDialog() {

    companion object {
        @JvmStatic
        fun newInstance(): AlbumSelectorDialog {
            return AlbumSelectorDialog()
        }
    }

    private var _binding: AlbumSelectorDialogBinding? = null
    private val binding get() = _binding!!
    private val albumAdapter = AlbumAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AlbumSelectorDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        setStatusBar(binding.root, Color.parseColor("#00000000"), false)
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.apply {
            setDimAmount(0f)
            attributes.gravity = Gravity.END
            setWindowAnimations(R.style.AnimRight)
        }
        setStatusBar(binding.root, Color.parseColor("#555555"), false)
        val bucketData: MutableList<Bucket> = ArrayList()
        val albumData = view.context.queryAlbum().onEach { (key, value) ->
            val bucket = Bucket(key, value[value.size - 1].path, value.size.toString())
            if (key == "最近项目") bucketData.add(0, bucket) else bucketData.add(bucket)
        }
        val popupWindow = BucketSelectorPopupWindow(view.context)
        popupWindow.setBucketData(bucketData, 0)
        popupWindow.setOnBucketSelectedListener(
            object : BucketSelectorPopupWindow.OnBucketSelectedListener {
                override fun onBucketSelected(name: String) {
                    binding.bucketName.text = name
                    albumAdapter.setAlbumData(albumData[name])
                }
            })
        popupWindow.setOnDismissListener {
            binding.bucketBox.isSelected = false
        }
        binding.back.setOnClickListener { dismiss() }
        binding.bucket.setOnClickListener {
            val isBucket = binding.bucketBox.isSelected
            if (isBucket) {
                popupWindow.dismiss()
            } else {
                popupWindow.show(binding.titleBar)
            }
            binding.bucketBox.isSelected = !isBucket
        }
        binding.preview.setOnClickListener {
            AlbumPreviewDialog.newInstance()
                .setSelectedImages(albumAdapter.getSelectedImage())
                .show(childFragmentManager)
        }
        binding.originalBox.setOnClickListener {
            val isOriginal = binding.originalBox.isSelected
            binding.originalBox.isSelected = !isOriginal
        }
        binding.send.setOnClickListener { }
        binding.list.layoutManager = GridLayoutManager(binding.list.context, 4)
        binding.list.adapter = albumAdapter
        albumAdapter.setAlbumData(albumData["最近项目"])
        albumAdapter.setOnItemClickListener(object : BaseAdapter.OnItemClickListener {
            override fun onItemClick(holder: BaseAdapter.ViewBindHolder, position: Int) {
                AlbumPreviewDialog.newInstance()
                    .setSelectedImages(arrayListOf(albumAdapter.getItem(position).path))
                    .show(childFragmentManager)
            }
        })
    }
}

/**
 * 获取相册资源
 */
fun Context.queryAlbum(): Map<String, List<Album>> {
    val data = HashMap<String, MutableList<Album>>().apply {
        this["最近项目"] = ArrayList()
    }
    val uri = MediaStore.Files.getContentUri("external")
    contentResolver.query(
        uri,
        arrayOf(
            MediaStore.Files.FileColumns._ID,
            "bucket_display_name",
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        ),
        null,
        null,
        MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
    )?.apply {
        while (moveToNext()) {
            val mediaType = getInt(getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE))
            if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE || mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                val id = getLong(getColumnIndex(MediaStore.Files.FileColumns._ID))
                val contentUri = ContentUris.withAppendedId(uri, id)
                val bucketName = getString(getColumnIndex("bucket_display_name"))
                val name = getString(getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME))
                val date = getLong(getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED))
                val media = Album(contentUri, bucketName, name, date, mediaType)
                if (!data.containsKey(bucketName)) {
                    data[bucketName] = ArrayList()
                }
                data[bucketName]?.add(media)
                data["最近项目"]?.add(media)
            }
        }
        close()
    }
    return data
}