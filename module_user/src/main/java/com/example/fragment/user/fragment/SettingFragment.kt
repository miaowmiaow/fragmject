package com.example.fragment.user.fragment

import android.app.Activity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import com.example.fragment.library.base.utils.CacheUtils
import com.example.fragment.library.base.utils.ScreenRecordHelper.startScreenRecord
import com.example.fragment.library.base.utils.ScreenRecordHelper.stopScreenRecord
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.dialog.StandardDialog
import com.example.fragment.library.common.fragment.ViewModelFragment
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.module.user.databinding.FragmentSettingBinding
import com.example.fragment.user.model.UserViewModel

class SettingFragment : ViewModelFragment<FragmentSettingBinding, UserViewModel>() {

    private var countDownTimer: CountDownTimer? = null

    override fun setViewBinding(inflater: LayoutInflater): FragmentSettingBinding {
        return FragmentSettingBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        update()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
    }

    private fun setupView() {
        binding.black.setOnClickListener { baseActivity.onBackPressed() }
        binding.systemTheme.setOnCheckedChangeListener { view, isChecked ->
            val uiMode = if (isChecked) -1 else 1
            updateSwitchButton(uiMode)
            WanHelper.setUIMode(uiMode)
            view.postDelayed({
                baseActivity.initUIMode()
            }, 200)
        }
        binding.darkTheme.setOnCheckedChangeListener { view, isChecked ->
            val uiMode = if (isChecked) 2 else 1
            updateSwitchButton(uiMode)
            WanHelper.setUIMode(uiMode)
            view.postDelayed({
                baseActivity.initUIMode()
            }, 300)
        }
        binding.screenRecord.setOnCheckedChangeListener { view, isChecked ->
            if (isChecked) {
                countDownTimer = object : CountDownTimer(5 * 1000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        baseActivity.alwaysShowTips("${(millisUntilFinished / 1000) + 1}s后开始录屏")
                    }

                    override fun onFinish() {
                        baseActivity.dismissTips()
                        baseActivity.startScreenRecord { code, message ->
                            if (code != Activity.RESULT_OK) {
                                baseActivity.showTips(message)
                                binding.screenRecord.isChecked = false
                            }
                        }
                    }
                }.start()
            } else {
                countDownTimer?.cancel()
                view.postDelayed({
                    baseActivity.dismissTips()
                    baseActivity.stopScreenRecord()
                }, 1500)
            }
        }
        binding.cacheSize.text = CacheUtils.getTotalCacheSize(baseActivity)
        binding.clearCache.setOnClickListener {
            StandardDialog.newInstance()
                .setContent("确定要清除缓存吗？")
                .setOnDialogClickListener(object : StandardDialog.OnDialogClickListener {
                    override fun onConfirm(dialog: StandardDialog) {
                        CacheUtils.clearAllCache(baseActivity)
                        binding.cacheSize.text = CacheUtils.getTotalCacheSize(baseActivity)
                    }

                    override fun onCancel(dialog: StandardDialog) {
                    }
                })
                .show(childFragmentManager)
        }
        binding.update.setOnClickListener {
            StandardDialog.newInstance()
                .setTitle("感谢使用")
                .setContent("喜欢的话，请给颗♥哈")
                .setOnDialogClickListener(object : StandardDialog.OnDialogClickListener {
                    override fun onConfirm(dialog: StandardDialog) {
                        val args = Bundle()
                        args.putString(Keys.URL, "https://github.com/miaowmiaow/FragmentProject")
                        baseActivity.navigation(Router.WEB, args)
                    }

                    override fun onCancel(dialog: StandardDialog) {
                    }
                })
                .show(childFragmentManager)
        }
        binding.about.setOnClickListener {
            val args = Bundle()
            args.putString(Keys.URL, "https://wanandroid.com")
            baseActivity.navigation(Router.WEB, args)
        }
        binding.privacyPolicy.setOnClickListener {
            val args = Bundle()
            args.putString(Keys.URL, "file:///android_asset/privacy_policy.html")
            baseActivity.navigation(Router.WEB, args)
        }
        binding.feedback.setOnClickListener {
            val args = Bundle()
            args.putString(Keys.URL, "https://github.com/miaowmiaow/FragmentProject/issues")
            baseActivity.navigation(Router.WEB, args)
        }
        binding.logout.setOnClickListener {
            StandardDialog.newInstance()
                .setContent("确定退出登录吗？")
                .setOnDialogClickListener(object : StandardDialog.OnDialogClickListener {
                    override fun onConfirm(dialog: StandardDialog) {
                        viewModel.logout()
                    }

                    override fun onCancel(dialog: StandardDialog) {
                    }
                })
                .show(childFragmentManager)
        }
    }

    private fun update() {
        WanHelper.getUser().observe(viewLifecycleOwner, { userBean ->
            binding.logout.visibility = if (userBean.id.isNotBlank()) View.VISIBLE else View.GONE
        })
        WanHelper.getUIMode().observe(viewLifecycleOwner, { result ->
            updateSwitchButton(result)
        })
        viewModel.logoutResult.observe(viewLifecycleOwner, { result ->
            if (result.errorCode == "0") {
                WanHelper.setUser(UserBean())
                baseActivity.onBackPressed()
            }
            if (result.errorMsg.isNotBlank()) {
                baseActivity.showTips(result.errorMsg)
            }
        })
    }

    private fun updateSwitchButton(uiMode: Int) {
        when (uiMode) {
            1 -> {
                binding.systemTheme.isChecked = false
                binding.systemTheme.isEnabled = true
                binding.darkTheme.isChecked = false
                binding.darkTheme.isEnabled = true
            }
            2 -> {
                binding.systemTheme.isChecked = false
                binding.systemTheme.isEnabled = false
                binding.darkTheme.isChecked = true
                binding.darkTheme.isEnabled = true
            }
            else -> {
                binding.systemTheme.isChecked = true
                binding.systemTheme.isEnabled = true
                binding.darkTheme.isChecked = false
                binding.darkTheme.isEnabled = false
            }
        }
    }

}