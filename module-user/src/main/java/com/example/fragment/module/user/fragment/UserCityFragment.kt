package com.example.fragment.module.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.utils.PinyinUtils
import com.example.fragment.library.base.utils.ReadAssetsFileUtil
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.user.adapter.CityAdapter
import com.example.fragment.module.user.bean.CityBean
import com.example.fragment.module.user.bean.CityPickerBean
import com.example.fragment.module.user.databinding.UserCityFragmentBinding
import com.example.fragment.module.user.model.UserViewModel
import com.google.gson.Gson

class UserCityFragment : RouterFragment() {

    private val viewModel: UserViewModel by activityViewModels()
    private var _binding: UserCityFragmentBinding? = null
    private val binding get() = _binding!!
    private val cityAdapter = CityAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = UserCityFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.cityList.adapter = null
        _binding = null
    }

    override fun initView() {
        binding.black.setOnClickListener { activity.onBackPressed() }
        binding.secrecy.setOnClickListener {
            viewModel.getUserBean().let {
                it.city = "保密"
                viewModel.updateUserBean(it)
            }
            activity.onBackPressed()
        }
        binding.sideLetterBar.setOverlay(binding.letterOverlay)
        binding.searchText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val searchText = binding.searchText.text.toString()
                binding.cityList.toppingToPosition(cityAdapter.searchCity(searchText))
                return@setOnEditorActionListener true
            }
            false
        }
        binding.sideLetterBar.setOnLetterChangedListener { letter ->
            val position = cityAdapter.getLetterPosition(letter)
            binding.cityList.scrollToPosition(position)
        }
        binding.cityList.layoutManager = LinearLayoutManager(binding.cityList.context)
        binding.cityList.adapter = cityAdapter
        val json = ReadAssetsFileUtil.getJson(activity, "city.json")
        val bean = Gson().fromJson(json, CityPickerBean::class.java)
        val citys: HashSet<CityBean> = HashSet()
        for (areas in bean.data.areas) {
            for (children in areas.children) {
                val pinyin = PinyinUtils.getPinYin(children.name)
                citys.add(CityBean(children.id, children.name, areas.name, pinyin, false))
            }
        }
        //set转换list
        val cities = ArrayList<CityBean>(citys)
        //按照字母排序
        cities.sortWith { city, t1 -> city.pinyin.compareTo(t1.pinyin) }
        cityAdapter.setCityData(cities)
        cityAdapter.setOnCityClickListener(object : CityAdapter.OnCityClickListener {
            override fun onCityClick(name: String) {
                viewModel.getUserBean().let {
                    it.city = name
                    viewModel.updateUserBean(it)
                }
                activity.onBackPressed()
            }
        })
    }

    override fun initViewModel(): BaseViewModel {
        viewModel.userResult()
        return viewModel
    }

    private fun RecyclerView.toppingToPosition(position: Int) {
        if (layoutManager == null || layoutManager !is LinearLayoutManager) return
        val lm = layoutManager as LinearLayoutManager
        val firstItemPosition = lm.findFirstVisibleItemPosition()
        val lastItemPosition = lm.findLastVisibleItemPosition()
        when {
            position <= firstItemPosition -> smoothScrollToPosition(position)
            position <= lastItemPosition -> {
                val childView = getChildAt(position - firstItemPosition)
                smoothScrollBy(0, childView.top)
            }
            else -> {
                smoothScrollToPosition(position)
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        if (newState != RecyclerView.SCROLL_STATE_IDLE) return
                        removeOnScrollListener(this)
                        toppingToPosition(position)
                    }
                })
            }
        }
    }

}