package com.example.miaow.picture.selector.dialog

import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.fragment.library.base.R
import com.example.fragment.library.base.dialog.FullDialog
import com.example.miaow.picture.databinding.AlbumPreviewDialogBinding
import com.example.miaow.picture.selector.fragment.AlbumPreviewFragment

class AlbumPreviewDialog : FullDialog() {

    companion object {
        @JvmStatic
        fun newInstance(): AlbumPreviewDialog {
            return AlbumPreviewDialog()
        }
    }

    private var _binding: AlbumPreviewDialogBinding? = null
    private val binding get() = _binding!!
    private val selectedImages: MutableList<Uri> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AlbumPreviewDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.apply {
            setDimAmount(0f)
            attributes.gravity = Gravity.END
            setWindowAnimations(R.style.AnimRight)
        }
        binding.viewpager2.adapter = object : FragmentStateAdapter(
            childFragmentManager,
            viewLifecycleOwner.lifecycle
        ) {
            override fun getItemCount(): Int {
                return selectedImages.size
            }

            override fun createFragment(position: Int): Fragment {
                val fragment = AlbumPreviewFragment.newInstance()
                val bundle = Bundle()
                bundle.putParcelable(AlbumPreviewFragment.PATH, selectedImages[position])
                val args = fragment.arguments
                if (args != null) {
                    args.putAll(bundle)
                } else {
                    fragment.arguments = bundle
                }
                return fragment
            }
        }
        binding.editor.setOnClickListener {

        }
    }

    fun setSelectedImages(images: List<Uri>): AlbumPreviewDialog {
        selectedImages.clear()
        selectedImages.addAll(images)
        return this
    }

}
