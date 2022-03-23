package com.example.fragment.module.user.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.utils.GsonUtil
import com.example.fragment.library.base.utils.PinyinUtils
import com.example.fragment.library.base.utils.ReadAssetsFileUtil
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.user.adapter.CityAdapter
import com.example.fragment.module.user.bean.CityBean
import com.example.fragment.module.user.bean.CityPickerBean
import com.example.fragment.module.user.databinding.UserCityFragmentBinding
import com.example.fragment.module.user.model.UserViewModel
import java.util.*
import kotlin.collections.ArrayList

class UserCityFragment : RouterFragment() {

    private val viewModel: UserViewModel by activityViewModels()
    private var _binding: UserCityFragmentBinding? = null
    private val binding get() = _binding!!
    private var cityAdapter = CityAdapter()

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
                binding.cityList.layoutManager?.let {
                    if (it is LinearLayoutManager) {
                        val firstPosition = it.findFirstCompletelyVisibleItemPosition()
                        val lastPosition = it.findLastCompletelyVisibleItemPosition()
                        val searchText = binding.searchText.text.toString()
                        val cityPosition = cityAdapter.searchCity(searchText)
                        val position = if (cityPosition > lastPosition) {
                            cityPosition + (lastPosition - firstPosition)
                        } else cityPosition
                        it.scrollToPosition(position)
                    }
                }
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
        val bean = GsonUtil.getBean(json, CityPickerBean::class.java)
        val citys: HashSet<CityBean> = HashSet()
        for (areasBean in bean.data.areas) {
            for (childrenBeanX in areasBean.children) {
                citys.add(
                    CityBean(
                        childrenBeanX.id,
                        childrenBeanX.name,
                        areasBean.name,
                        PinyinUtils.getPinYin(childrenBeanX.name),
                        false
                    )
                )
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
        return viewModel
    }

    override fun initLoad() {
        viewModel.getUser()
    }

}