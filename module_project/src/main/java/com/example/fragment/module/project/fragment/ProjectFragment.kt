package com.example.fragment.module.project.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.example.fragment.library.base.component.view.SimpleTabLayout
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.module.project.databinding.FragmentProjectBinding
import com.example.fragment.module.project.model.ProjectViewModel

class ProjectFragment : ViewModelFragment<FragmentProjectBinding, ProjectViewModel>() {

    companion object {
        @JvmStatic
        fun newInstance(): ProjectFragment {
            return ProjectFragment()
        }
    }

    override fun setViewBinding(inflater: LayoutInflater): FragmentProjectBinding {
        return FragmentProjectBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        update()
    }

    private fun setupView() {
        binding.tab.setTabMod(SimpleTabLayout.MODE.FIXED)
    }

    private fun update() {

    }
}