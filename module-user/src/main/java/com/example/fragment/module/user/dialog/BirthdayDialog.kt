package com.example.fragment.module.user.dialog

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.fragment.library.base.adapter.BaseAdapter
import com.example.fragment.library.base.dialog.BottomDialog
import com.example.fragment.library.base.utils.TimeUtil
import com.example.fragment.module.user.databinding.BirthdayDialogBinding
import com.example.fragment.module.user.databinding.WheelItemBinding

class BirthdayDialog : BottomDialog() {

    companion object {
        @JvmStatic
        fun newInstance(): BirthdayDialog {
            return BirthdayDialog()
        }
    }

    private var _binding: BirthdayDialogBinding? = null
    private val binding get() = _binding!!
    private val yearData: MutableList<String> = arrayListOf()
    private val monthData: MutableList<String> = arrayListOf()
    private val dayData: MutableList<String> = arrayListOf()
    private var birthday = TimeUtil.currentData("yyyy-MM-dd")
    private var listener: BirthdayListener? = null

    private val yearAdapter = object : BaseAdapter<String>() {

        override fun onCreateViewBinding(viewType: Int): (LayoutInflater, ViewGroup, Boolean) -> ViewBinding {
            return WheelItemBinding::inflate
        }

        override fun onItemView(holder: ViewBindHolder, position: Int, item: String) {
            val binding = holder.binding as WheelItemBinding
            binding.tv.text = "${item}年"
        }

    }

    private val monthAdapter = object : BaseAdapter<String>() {

        override fun onCreateViewBinding(viewType: Int): (LayoutInflater, ViewGroup, Boolean) -> ViewBinding {
            return WheelItemBinding::inflate
        }

        @SuppressLint("SetTextI18n")
        override fun onItemView(holder: ViewBindHolder, position: Int, item: String) {
            val binding = holder.binding as WheelItemBinding
            binding.tv.text = "${item}月"
        }
    }

    private val daysAdapter = object : BaseAdapter<String>() {

        override fun onCreateViewBinding(viewType: Int): (LayoutInflater, ViewGroup, Boolean) -> ViewBinding {
            return WheelItemBinding::inflate
        }

        @SuppressLint("SetTextI18n")
        override fun onItemView(holder: ViewBindHolder, position: Int, item: String) {
            val binding = holder.binding as WheelItemBinding
            binding.tv.text = "${item}日"
        }
    }

    init {
        val currYear = TimeUtil.timeFormat(System.currentTimeMillis(), "yyyy").toInt()
        for (i in 1900..currYear) {
            yearData.add("$i")
        }
        for (i in 1..12) {
            monthData.add("$i")
        }
        for (i in 1..31) {
            dayData.add("$i")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BirthdayDialogBinding.inflate(inflater, container, false)
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
        binding.cancel.setOnClickListener { dismiss() }
        binding.secrecy.setOnClickListener {
            listener?.onBirthday("保密")
            dismiss()
        }
        binding.config.setOnClickListener {
            val currYear = yearData[binding.year.findCenterItemPosition()]
            val currMonth = monthData[binding.month.findCenterItemPosition()]
            val currDay = dayData[binding.day.findCenterItemPosition()]
            listener?.onBirthday("$currYear-${currMonth}-${currDay}")
            dismiss()
        }
        binding.year.layoutManager = LinearLayoutManager(binding.year.context)
        binding.year.setSelectPosition(TimeUtil.timeFormat(birthday, "yyyy").toInt() - 1900)
        binding.year.setSelectMask(false)
        binding.year.setVisibleNum(2)
        binding.year.setWheelAdapter(yearAdapter as RecyclerView.Adapter<RecyclerView.ViewHolder>)
        yearAdapter.setNewData(yearData)
        binding.month.layoutManager = LinearLayoutManager(binding.year.context)
        binding.month.setSelectPosition(TimeUtil.timeFormat(birthday, "MM").toInt() - 1)
        binding.month.setSelectMask(false)
        binding.month.setVisibleNum(2)
        binding.month.setWheelAdapter(monthAdapter as RecyclerView.Adapter<RecyclerView.ViewHolder>)
        monthAdapter.setNewData(monthData)
        binding.day.layoutManager = LinearLayoutManager(binding.year.context)
        binding.day.setSelectPosition(TimeUtil.timeFormat(birthday, "dd").toInt() - 1)
        binding.day.setSelectMask(false)
        binding.day.setVisibleNum(2)
        binding.day.setWheelAdapter(daysAdapter as RecyclerView.Adapter<RecyclerView.ViewHolder>)
        daysAdapter.setNewData(dayData)
    }

    fun setBirthday(birthday: String): BirthdayDialog {
        if (birthday.isNotBlank() && "保密" != birthday) {
            this.birthday = birthday
        }
        return this
    }

    fun setBirthdayListener(listener: BirthdayListener): BirthdayDialog {
        this.listener = listener
        return this
    }

    interface BirthdayListener {
        fun onBirthday(time: String)
    }

}