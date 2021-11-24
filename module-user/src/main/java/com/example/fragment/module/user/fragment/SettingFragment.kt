package com.example.fragment.module.user.fragment

import android.app.Activity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.utils.CacheUtils
import com.example.fragment.library.base.utils.ScreenRecordHelper.startScreenRecord
import com.example.fragment.library.base.utils.ScreenRecordHelper.stopScreenRecord
import com.example.fragment.library.base.view.SwitchButton
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.dialog.StandardDialog
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.module.user.databinding.FragmentSettingBinding
import com.example.fragment.module.user.model.UserLoginViewModel
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

class SettingFragment : RouterFragment() {

    private var countDownTimer: CountDownTimer? = null

    private val viewModel: UserLoginViewModel by viewModels()
    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        countDownTimer?.cancel()
    }

    override fun initView() {
        binding.black.setOnClickListener { activity.onBackPressed() }
        binding.systemTheme.setOnCheckedChangeListener(object :
            SwitchButton.OnCheckedChangeListener {
            override fun onCheckedChanged(view: SwitchButton, isChecked: Boolean) {
                WanHelper.setUIMode(
                    if (isChecked) {
                        binding.darkTheme.setChecked(false)
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                        -1
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        1
                    }
                )
            }
        })
        binding.darkTheme.setOnCheckedChangeListener(object :
            SwitchButton.OnCheckedChangeListener {
            override fun onCheckedChanged(view: SwitchButton, isChecked: Boolean) {
                WanHelper.setUIMode(
                    if (isChecked) {
                        binding.systemTheme.setChecked(false)
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        2
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        1
                    }
                )
            }
        })
        binding.screenRecord.setOnCheckedChangeListener(object :
            SwitchButton.OnCheckedChangeListener {
            override fun onCheckedChanged(view: SwitchButton, isChecked: Boolean) {
                val status = if (isChecked) 1 else 0
                WanHelper.setScreenRecordStatus(status)
                if (isChecked) {
                    countDownTimer = object : CountDownTimer(5000, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                            activity.alwaysShowTips("${(millisUntilFinished / 1000) + 1}s后开始录屏")
                        }

                        override fun onFinish() {
                            activity.dismissTips()
                            activity.startScreenRecord { code, message ->
                                if (code != Activity.RESULT_OK) {
                                    activity.showTips(message)
                                    binding.screenRecord.setChecked(false)
                                }
                            }
                        }
                    }.start()
                } else {
                    countDownTimer?.cancel()
                    view.postDelayed({
                        activity.dismissTips()
                        activity.stopScreenRecord()
                    }, 1000)
                }
            }
        })
        binding.cacheSize.text = CacheUtils.getTotalCacheSize(activity)
        binding.clearCache.setOnClickListener {
            StandardDialog.newInstance()
                .setContent("确定要清除缓存吗？")
                .setOnDialogClickListener(object : StandardDialog.OnDialogClickListener {
                    override fun onConfirm(dialog: StandardDialog) {
                        CacheUtils.clearAllCache(activity)
                        binding.cacheSize.text = CacheUtils.getTotalCacheSize(activity)
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
                        val args = bundleOf(Keys.URL  to "https://github.com/miaowmiaow/fragmject.git")
                        activity.navigation(Router.SETTING2WEB, args)
                    }

                    override fun onCancel(dialog: StandardDialog) {
                    }
                })
                .show(childFragmentManager)
        }
        binding.about.setOnClickListener {
            val args = bundleOf(Keys.URL to "https://wanandroid.com")
            activity.navigation(Router.SETTING2WEB, args)
        }
        binding.privacyPolicy.setOnClickListener {
            var inputStream: InputStream? = null
            try {
                inputStream = resources.assets.open("privacy_policy.template")
                readRawFromStreamToString(inputStream)?.let { template ->
                    inputStream = resources.assets.open("privacy_policy.html")
                    readRawFromStreamToString(inputStream)?.let { html ->
                        val args = bundleOf(Keys.HTML to html.replace("{privacy_policy}", template))
                        activity.navigation(Router.SETTING2WEB, args)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                inputStream?.close()
            }
        }
        binding.feedback.setOnClickListener {
            val args = bundleOf(Keys.URL to "https://github.com/miaowmiaow/fragmject/issues")
            activity.navigation(Router.SETTING2WEB, args)
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

    override fun initViewModel(): BaseViewModel {
        viewModel.logoutResult.observe(viewLifecycleOwner) { result ->
            when (result.errorCode) {
                "0" -> {
                    WanHelper.setUser(UserBean())
                    activity.onBackPressed()
                }
                else -> activity.showTips(result.errorMsg)
            }
        }
        return viewModel
    }

    override fun initLoad() {
        WanHelper.getUser().observe(viewLifecycleOwner) { userBean ->
            binding.logout.visibility = if (userBean.id.isNotBlank()) View.VISIBLE else View.GONE
        }
        WanHelper.getUIMode().observe(viewLifecycleOwner) { result ->
            when (result) {
                1 -> {
                    binding.systemTheme.setChecked(false)
                    binding.darkTheme.setChecked(false)
                }
                2 -> {
                    binding.systemTheme.setChecked(false)
                    binding.darkTheme.setChecked(true)
                }
                else -> {
                    binding.systemTheme.setChecked(true)
                    binding.darkTheme.setChecked(false)
                }
            }
        }
        WanHelper.getScreenRecordStatus().observe(viewLifecycleOwner) { result ->
            when (result) {
                0 -> binding.screenRecord.setChecked(false)
                1 -> binding.screenRecord.setChecked(true)
            }
        }
    }

    @Throws(IOException::class)
    fun readRawFromStreamToString(inputStream: InputStream?): String? {
        if (inputStream == null) {
            return null
        }
        val baos = ByteArrayOutputStream()
        var len: Int
        val bytes = ByteArray(2048)
        while (inputStream.read(bytes).also { len = it } != -1) {
            baos.write(bytes, 0, len)
        }
        return baos.toString("UTF-8")
    }

}