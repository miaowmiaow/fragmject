package com.example.fragment.library.base.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.fragment.library.base.databinding.ViewProgressBinding

class ProgressDialog : BaseDialog() {

    companion object {
        @JvmStatic
        fun newInstance(): ProgressDialog {
            return ProgressDialog()
        }
    }

    private lateinit var binding: ViewProgressBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ViewProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        binding.root.setOnClickListener {
            dismiss()
        }
        view.post {
            if (binding.progress.isStopped) {
                binding.progress.start()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!binding.progress.isStopped) {
            binding.progress.stop()
        }
    }

}