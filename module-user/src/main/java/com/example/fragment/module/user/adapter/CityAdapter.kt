package com.example.fragment.module.user.adapter

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.example.fragment.library.base.adapter.BaseAdapter
import com.example.fragment.library.base.utils.PinyinUtils
import com.example.fragment.module.user.bean.CityBean
import com.example.fragment.module.user.databinding.CityHotItemBinding
import com.example.fragment.module.user.databinding.CityItemBinding

class CityAdapter : BaseAdapter<CityBean>() {

    private val letterIndexes: MutableMap<String, Int> = HashMap()
    private var onCityClickListener: OnCityClickListener? = null

    override fun onCreateViewBinding(viewType: Int): (LayoutInflater, ViewGroup, Boolean) -> ViewBinding {
        return if (viewType == 0) {
            CityHotItemBinding::inflate
        } else {
            CityItemBinding::inflate
        }
    }

    override fun onItemView(holder: ViewBindHolder, position: Int, item: CityBean) {
        if (getItemViewType(position) == 0) {
            val binding = holder.binding as CityHotItemBinding
            binding.beijing.setOnClickListener {
                onCityClickListener?.onCityClick("北京市")
            }
            binding.shanghai.setOnClickListener {
                onCityClickListener?.onCityClick("上海市")
            }
            binding.guangzhou.setOnClickListener {
                onCityClickListener?.onCityClick("广东省-广州市")
            }
            binding.shenzhen.setOnClickListener {
                onCityClickListener?.onCityClick("广东省-深圳市")
            }
        } else {
            val binding = holder.binding as CityItemBinding
            var name = item.name
            if (name.length > 2 && !name.contains("自治州") && !name.contains("自治县")) {
                name = name.replace("市", "").replace("县", "").replace("地区", "")
            }
            binding.name.text = name
            val currentLetter = PinyinUtils.getFirstLetter(item.pinyin)
            val previousLetter = PinyinUtils.getFirstLetter(getItem(position - 1).pinyin)
            if (!TextUtils.equals(currentLetter, previousLetter)) {
                binding.letter.visibility = View.VISIBLE
                binding.letter.text = currentLetter
            } else {
                binding.letter.visibility = View.GONE
            }
            binding.name.setOnClickListener {
                if (item.pcName == item.name) {
                    onCityClickListener?.onCityClick(item.pcName)
                } else {
                    onCityClickListener?.onCityClick(item.pcName + "-" + item.name)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) 0 else 1
    }

    fun setCityData(newData: List<CityBean>) {
        clearData()
        addData(0, listOf(CityBean(-1, "热门", "0")))
        addData(newData)
        getData().forEachIndexed { index, cityBean ->
            //当前城市拼音首字母
            val currentLetter = PinyinUtils.getFirstLetter(cityBean.pinyin)
            //上个首字母，如果不存在设为""
            val previousLetter = if (index >= 1) PinyinUtils.getFirstLetter(
                getItem(index - 1).pinyin
            ) else ""
            if (!TextUtils.equals(currentLetter, previousLetter)) {
                letterIndexes[currentLetter] = index
            }
        }
        notifyItemRangeChanged(0, getData().size)
    }

    fun getLetterPosition(letter: String?): Int {
        val integer = letterIndexes[letter]
        return integer ?: -1
    }

    fun searchCity(name: String): Int {
        for (i in getData().indices) {
            val cityName: String = getData()[i].name
            if (name.isNotEmpty()) {
                val n1 = name.substring(0, 1)
                val cn1 = cityName.substring(0, 1)
                if (n1 == cn1 && cityName.contains(name)) {
                    return i
                }
            }
        }
        return 0
    }

    fun setOnCityClickListener(listener: OnCityClickListener) {
        this.onCityClickListener = listener
    }

    interface OnCityClickListener {
        fun onCityClick(name: String)
    }
}