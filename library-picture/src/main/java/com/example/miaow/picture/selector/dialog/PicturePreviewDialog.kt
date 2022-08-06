package com.example.miaow.picture.selector.dialog

import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.example.fragment.library.base.dialog.FullDialog
import com.example.miaow.picture.R
import com.example.miaow.picture.databinding.PicturePreviewDialogBinding
import com.example.miaow.picture.databinding.PicturePreviewTabBinding
import com.example.miaow.picture.editor.dialog.PictureEditorCallback
import com.example.miaow.picture.editor.dialog.PictureEditorDialog
import com.example.miaow.picture.selector.adapter.PicturePreviewAdapter
import com.example.miaow.picture.selector.bean.MediaBean
import com.example.miaow.picture.selector.model.PictureViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class PicturePreviewDialog : FullDialog() {

    companion object {
        @JvmStatic
        fun newInstance(): PicturePreviewDialog {
            return PicturePreviewDialog()
        }
    }

    private val viewModel: PictureViewModel by activityViewModels()
    private var _binding: PicturePreviewDialogBinding? = null
    private val binding get() = _binding!!
    private val previewAdapter = PicturePreviewAdapter()
    private var _callback: PicturePreviewCallback? = null
    private val origSelectPosition: MutableList<Int> = ArrayList()
    private val currSelectPosition: MutableList<Int> = ArrayList()
    private var currPreviewPosition = -1
    private var isSinglePicture = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PicturePreviewDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.viewpager2.adapter = null
        _callback = null
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.apply {
            setDimAmount(0f)
            attributes.gravity = Gravity.END
            setWindowAnimations(R.style.AnimRight)
        }
        initView()
        initViewModel()
    }

    private fun initView() {
        binding.back.setOnClickListener { dismiss() }
        binding.config.setOnClickListener {
            _callback?.onFinish(currSelectPosition)
            dismiss()
        }
        binding.selectBox.setOnClickListener {
            val origPosition = if (!isSinglePicture) {
                origSelectPosition[binding.viewpager2.currentItem]
            } else binding.viewpager2.currentItem
            if (currSelectPosition.contains(origPosition)) {
                currSelectPosition.remove(origPosition)
                binding.selectBox.isSelected = false
                binding.tabLayout.getTabAt(binding.viewpager2.currentItem)?.customView?.let { tab ->
                    tab.findViewById<View>(R.id.dim).alpha = 0.75f
                }
            } else if (currSelectPosition.size < 9) {
                currSelectPosition.add(origPosition)
                binding.selectBox.isSelected = true
                binding.tabLayout.getTabAt(binding.viewpager2.currentItem)?.customView?.let { tab ->
                    tab.findViewById<View>(R.id.dim).alpha = 0f
                }
            }
        }
        binding.editor.setOnClickListener {
            val currentUri = previewAdapter.getItem(binding.viewpager2.currentItem).uri
            PictureEditorDialog.newInstance()
                .setBitmapPathOrUri(null, currentUri)
                .setPictureEditorCallback(object : PictureEditorCallback {
                    override fun onFinish(path: String?, uri: Uri?) {
                        uri?.let {
                            previewAdapter.getItem(binding.viewpager2.currentItem).uri = it
                            previewAdapter.notifyItemChanged(binding.viewpager2.currentItem)
                        }
                    }
                })
                .show(childFragmentManager)
        }
        binding.viewpager2.adapter = previewAdapter
        binding.viewpager2.offscreenPageLimit = 2
        binding.viewpager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val origPosition = if (!isSinglePicture) {
                    origSelectPosition[position]
                } else position
                binding.selectBox.isSelected = currSelectPosition.contains(origPosition)
            }
        })
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                tab.customView?.let {
                    it.findViewById<View>(R.id.selected).visibility = View.VISIBLE
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                tab.customView?.let {
                    it.findViewById<View>(R.id.selected).visibility = View.INVISIBLE
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun initViewModel() {
        viewModel.currAlbumResult.observe(viewLifecycleOwner) {
            if (!isSinglePicture) {
                val data: MutableList<MediaBean> = ArrayList()
                origSelectPosition.forEach { position ->
                    data.add(it[position])
                }
                previewAdapter.setNewData(data)
                TabLayoutMediator(
                    binding.tabLayout,
                    binding.viewpager2,
                    false,
                    true
                ) { tab, position ->
                    PicturePreviewTabBinding.inflate(LayoutInflater.from(binding.root.context))
                        .let { item ->
                            item.image.load(previewAdapter.getItem(position).uri)
                            tab.customView = item.root
                        }
                }.attach()
                binding.tabLayout.visibility = View.VISIBLE
            } else {
                previewAdapter.setNewData(it)
                binding.viewpager2.setCurrentItem(currPreviewPosition, false)
                binding.tabLayout.visibility = View.GONE
            }
        }
    }

    fun setSelectedPosition(
        selectPosition: List<Int>,
        previewPosition: Int = -1
    ): PicturePreviewDialog {
        origSelectPosition.clear()
        currSelectPosition.clear()
        origSelectPosition.addAll(selectPosition)
        currSelectPosition.addAll(selectPosition)
        currPreviewPosition = previewPosition
        isSinglePicture = previewPosition != -1
        return this
    }

    fun setPicturePreviewCallback(callback: PicturePreviewCallback): PicturePreviewDialog {
        this._callback = callback
        return this
    }

}

interface PicturePreviewCallback {
    fun onFinish(selectPosition: List<Int>)
}
