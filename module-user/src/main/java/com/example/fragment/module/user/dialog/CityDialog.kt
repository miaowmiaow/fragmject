package com.example.fragment.module.user.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fragment.library.base.dialog.BottomDialog
import com.example.fragment.library.base.utils.GsonUtil
import com.example.fragment.library.base.utils.PinyinUtils
import com.example.fragment.library.base.utils.ReadAssetsFileUtil
import com.example.fragment.module.user.adapter.CityAdapter
import com.example.fragment.module.user.bean.CityBean
import com.example.fragment.module.user.bean.CityPickerBean
import com.example.fragment.module.user.databinding.CityDialogBinding
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.sortWith

class CityDialog : BottomDialog() {

    companion object {
        @JvmStatic
        fun newInstance(): CityDialog {
            return CityDialog()
        }
    }

    private var _binding: CityDialogBinding? = null
    private val binding get() = _binding!!

    private var cityAdapter = CityAdapter()

    private var listener: CityListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CityDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dialog?.window?.apply {
            setDimAmount(0F)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        super.onViewCreated(view, savedInstanceState)
        binding.black.setOnClickListener { dismiss() }
        binding.secrecy.setOnClickListener {
            listener?.onCity("保密")
            dismiss()
        }
        binding.sideLetterBar.setOverlay(binding.letterOverlay)
        binding.searchText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                binding.city.scrollToPosition(cityAdapter.searchCity(binding.searchText.text.toString()))
                return@setOnEditorActionListener true
            }
            false
        }
        binding.sideLetterBar.setOnLetterChangedListener { letter ->
            val position = cityAdapter.getLetterPosition(letter)
            binding.city.scrollToPosition(position)
        }
        binding.city.layoutManager = LinearLayoutManager(binding.city.context)
        binding.city.adapter = cityAdapter
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
        val cities: java.util.ArrayList<CityBean> = ArrayList(citys)
        //按照字母排序
        cities.sortWith { city, t1 -> city.pinyin.compareTo(t1.pinyin) }
        cityAdapter.setCityData(cities)
        cityAdapter.setOnCityClickListener(object : CityAdapter.OnCityClickListener{
            override fun onCityClick(name: String) {
                listener?.onCity(name)
                dismiss()
            }
        })
    }

    fun setCityListener(listener: CityListener): CityDialog {
        this.listener = listener
        return this
    }

    interface CityListener {
        fun onCity(name: String)
    }


}