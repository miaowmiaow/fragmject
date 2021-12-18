package com.example.fragment.module.user.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.base.model.BaseViewModel
import com.example.fragment.library.base.utils.CacheUtils
import com.example.fragment.library.base.utils.ScreenRecordHelper.startScreenRecord
import com.example.fragment.library.base.utils.ScreenRecordHelper.stopScreenRecord
import com.example.fragment.library.base.utils.SystemUtil
import com.example.fragment.library.base.view.SwitchButton
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.dialog.StandardDialog
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.library.common.utils.WanHelper
import com.example.fragment.module.user.databinding.FragmentSettingBinding
import com.example.fragment.module.user.model.SettingViewModel
import java.io.File

class SettingFragment : RouterFragment() {

    private var countDownTimer: CountDownTimer? = null

    private val viewModel: SettingViewModel by viewModels()
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
        binding.systemTheme.setOnChangeListener(object : SwitchButton.OnChangeListener {

            override fun onOpen(view: SwitchButton) {
                binding.darkTheme.setChecked(false)
                WanHelper.setUIMode("-1")
            }

            override fun onClose(view: SwitchButton) {
                WanHelper.setUIMode("1")
            }
        })
        binding.darkTheme.setOnChangeListener(object : SwitchButton.OnChangeListener {

            override fun onOpen(view: SwitchButton) {
                binding.systemTheme.setChecked(false)
                WanHelper.setUIMode("2")
            }

            override fun onClose(view: SwitchButton) {
                WanHelper.setUIMode("1")
            }
        })
        binding.screenRecord.setOnChangeListener(object : SwitchButton.OnChangeListener {

            override fun onOpen(view: SwitchButton) {
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
                WanHelper.setScreenRecord("1")
            }

            override fun onClose(view: SwitchButton) {
                countDownTimer?.cancel()
                view.postDelayed({
                    activity.dismissTips()
                    activity.stopScreenRecord()
                }, 1000)
                WanHelper.setScreenRecord("0")
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
        binding.versionName.text = SystemUtil.getVersionName()
        binding.update.setOnClickListener {
            viewModel.update()
        }
        binding.about.setOnClickListener {
            val args = bundleOf(Keys.URL to "https://wanandroid.com")
            activity.navigation(Router.WEB, args)
        }
        binding.privacyPolicy.setOnClickListener {
            val args = bundleOf(Keys.URL to "file:///android_asset/privacy_policy.html")
            activity.navigation(Router.WEB, args)
        }
        binding.feedback.setOnClickListener {
            val args = bundleOf(Keys.URL to "https://github.com/miaowmiaow/fragmject/issues")
            activity.navigation(Router.WEB, args)
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
        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            when (result.errorCode) {
                "0" -> result.data?.let { data ->
                    StandardDialog.newInstance()
                        .setTitle("有新版本更新啦♥~")
                        .setContent("当前版本：${SystemUtil.getVersionName()}\n最新版本：${data.versionName}")
                        .setOnDialogClickListener(object : StandardDialog.OnDialogClickListener {
                            override fun onConfirm(dialog: StandardDialog) {
                                val apkUrl = data.download_url
                                val cachePath = CacheUtils.getCacheDirPath(activity, "apk")
                                val apkName = apkUrl.substring(apkUrl.lastIndexOf("/") + 1)
                                val filePathName = cachePath + File.separator + apkName
                                val file = File(filePathName)
                                if (!file.exists() || !file.isFile) {
                                    viewModel.downloadApk(apkUrl, filePathName)
                                } else {
                                    viewModel.downloadApkResult.postValue(
                                        HttpResponse("0", filePathName)
                                    )
                                }
                            }

                            override fun onCancel(dialog: StandardDialog) {
                            }
                        }).show(childFragmentManager)
                }
                else -> activity.showTips(result.errorMsg)
            }
        }
        viewModel.downloadApkResult.observe(viewLifecycleOwner) { result ->
            when (result.errorCode) {
                "0" -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    val uri = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        val authority = "${SystemUtil.getPackageName()}.FileProvider"
                        FileProvider.getUriForFile(activity, authority, File(result.errorMsg))
                    } else {
                        Uri.parse("file://" + result.errorMsg)
                    }
                    val type = "application/vnd.android.package-archive"
                    intent.setDataAndType(uri, type)
                    activity.startActivity(intent)
                }
                else -> activity.showTips(result.errorMsg)
            }
        }
        return viewModel
    }

    override fun initLoad() {
        WanHelper.registerScreenRecord(viewLifecycleOwner) { eventBean ->
            if (eventBean.key == WanHelper.SCREEN_RECORD) {
                when (eventBean.value) {
                    "0" -> binding.screenRecord.setChecked(false)
                    "1" -> binding.screenRecord.setChecked(true)
                }
            }
        }
        WanHelper.getScreenRecord()
        WanHelper.registerUIMode(viewLifecycleOwner) { eventBean ->
            if (eventBean.key == WanHelper.UI_MODE) {
                when (eventBean.value) {
                    "1" -> {
                        binding.systemTheme.setChecked(false)
                        binding.darkTheme.setChecked(false)
                    }
                    "2" -> {
                        binding.systemTheme.setChecked(false)
                        binding.darkTheme.setChecked(true)
                    }
                    else -> {
                        binding.systemTheme.setChecked(true)
                        binding.darkTheme.setChecked(false)
                    }
                }
            }
        }
        WanHelper.getUIMode()
        WanHelper.registerUser(viewLifecycleOwner) { userBean ->
            binding.logout.visibility = if (userBean.id.isNotBlank()) View.VISIBLE else View.GONE
        }
        WanHelper.getUser()
    }
}