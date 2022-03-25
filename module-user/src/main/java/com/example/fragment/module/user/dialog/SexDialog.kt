package com.example.fragment.module.user.dialog

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.fragment.library.base.dialog.BottomDialog
import com.example.fragment.module.user.databinding.SexDialogBinding

class SexDialog : BottomDialog() {

    companion object {
        @JvmStatic
        fun newInstance(): SexDialog {
            return SexDialog()
        }
    }

    private var _binding: SexDialogBinding? = null
    private val binding get() = _binding!!
    private val sexViews = arrayListOf<View>()
    private var sexIndex = -1
    private var listener: SexListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SexDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        setNavigationBar(binding.root, Color.TRANSPARENT, true)
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setNavigationBar(binding.root, Color.WHITE, true)
        sexViews.add(binding.secrecy)
        sexViews.add(binding.male)
        sexViews.add(binding.female)
        binding.cancel.setOnClickListener {
            dismiss()
        }
        binding.secrecy.setOnClickListener {
            selectionSex(0)
            listener?.onSex("保密")
            dismiss()
        }
        binding.male.setOnClickListener {
            selectionSex(1)
            listener?.onSex("男生")
            dismiss()
        }
        binding.female.setOnClickListener {
            selectionSex(2)
            listener?.onSex("女生")
            dismiss()
        }
        selectionSex(sexIndex)
    }

    private fun selectionSex(index: Int) {
        sexIndex = index
        sexViews.forEach {
            it.isSelected = false
        }
        sexViews[sexIndex].isSelected = true
    }

    fun setSex(sex: String): SexDialog {
        if ("保密" == sex) {
            sexIndex = 0
        }
        if ("男生" == sex) {
            sexIndex = 1
        }
        if ("女生" == sex) {
            sexIndex = 2
        }
        return this
    }

    fun setSexListener(listener: SexListener): SexDialog {
        this.listener = listener
        return this
    }

    interface SexListener {
        fun onSex(sex: String)
    }

}