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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.fragment.library.base.http.HttpResponse
import com.example.fragment.library.base.vm.BaseViewModel
import com.example.fragment.library.base.utils.CacheUtils
import com.example.fragment.library.base.utils.ScreenRecordHelper.startScreenRecord
import com.example.fragment.library.base.utils.ScreenRecordHelper.stopScreenRecord
import com.example.fragment.library.base.utils.getVersionName
import com.example.fragment.library.common.bean.UserBean
import com.example.fragment.library.common.constant.Keys
import com.example.fragment.library.common.constant.Router
import com.example.fragment.library.common.dialog.StandardDialog
import com.example.fragment.library.common.fragment.RouterFragment
import com.example.fragment.module.user.databinding.SettingFragmentBinding
import com.example.fragment.module.user.vm.SettingViewModel
import com.example.fragment.module.user.vm.UpdateViewModel
import com.example.fragment.module.user.vm.UserLoginViewModel
import com.example.fragment.module.user.vm.UserViewModel
import kotlinx.coroutines.launch
import java.io.File

class SettingFragment : RouterFragment() {

    private val settingViewModel: SettingViewModel by activityViewModels()

    private val updateViewModel: UpdateViewModel by viewModels(
        factoryProducer = {
            UpdateViewModel.provideFactory("http://122.51.186.2:8080/update.json")
        }
    )
    private val userViewModel: UserViewModel by activityViewModels()
    private val userLoginViewModel: UserLoginViewModel by activityViewModels()
    private var _binding: SettingFragmentBinding? = null
    private val binding get() = _binding!!
    private var countDownTimer = object : CountDownTimer(5000, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            alwaysShowTips("${(millisUntilFinished / 1000) + 1}s后开始录屏")
        }

        override fun onFinish() {
            dismissTips()
            requireActivity().startScreenRecord { code, message ->
                if (code != Activity.RESULT_OK) {
                    showTips(message)
                    binding.screenRecord.isChecked = false
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer.cancel()
        _binding = null
    }

    override fun initView() {
        binding.black.setOnClickListener { onBackPressed() }
        binding.systemTheme.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.systemTheme.isChecked = true
                binding.darkTheme.isChecked = false
                settingViewModel.updateUiMode("-1")
            } else {
                binding.systemTheme.isChecked = false
                binding.darkTheme.isChecked = false
                settingViewModel.updateUiMode("1")
            }
        }
        binding.darkTheme.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.systemTheme.isChecked = false
                binding.darkTheme.isChecked = true
                settingViewModel.updateUiMode("2")
            } else {
                binding.systemTheme.isChecked = false
                binding.darkTheme.isChecked = false
                settingViewModel.updateUiMode("1")
            }
        }
        binding.screenRecord.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                countDownTimer.start()
                binding.screenRecord.isChecked = true
            } else {
                countDownTimer.cancel()
                buttonView.postDelayed({
                    dismissTips()
                    requireActivity().stopScreenRecord()
                }, 1000)
                binding.screenRecord.isChecked = false
            }
        }
        binding.cacheSize.text = CacheUtils.getTotalSize(requireActivity())
        binding.clearCache.setOnClickListener {
            StandardDialog.newInstance()
                .setContent("确定要清除缓存吗？")
                .setOnDialogClickListener(object : StandardDialog.OnDialogClickListener {
                    override fun onConfirm(dialog: StandardDialog) {
                        CacheUtils.clearAllCache(requireActivity())
                        binding.cacheSize.text = CacheUtils.getTotalSize(requireActivity())
                    }

                    override fun onCancel(dialog: StandardDialog) {
                    }
                })
                .show(childFragmentManager)
        }
        binding.versionName.text = requireActivity().getVersionName()
        binding.update.setOnClickListener {
            updateViewModel.update()
        }
        binding.about.setOnClickListener {
            val url = Uri.encode("https://wanandroid.com")
            navigation(Router.WEB, bundleOf(Keys.URL to url))
        }
        binding.privacyPolicy.setOnClickListener {
            val url = Uri.encode("file:///android_asset/privacy_policy.html")
            navigation(Router.WEB, bundleOf(Keys.URL to url))
        }
        binding.feedback.setOnClickListener {
            val url = Uri.encode("https://github.com/miaowmiaow/fragmject/issues")
            navigation(Router.WEB, bundleOf(Keys.URL to url))
        }
        binding.logout.setOnClickListener {
            val listener = object : StandardDialog.OnDialogClickListener {
                override fun onConfirm(dialog: StandardDialog) {
                    userLoginViewModel.logout()
                }

                override fun onCancel(dialog: StandardDialog) {
                }
            }
            StandardDialog.newInstance()
                .setContent("确定退出登录吗？")
                .setOnDialogClickListener(listener)
                .show(childFragmentManager)
        }
    }

    override fun initViewModel(): BaseViewModel {
        settingViewModel.uiModeResult().observe(viewLifecycleOwner) {
            when (it) {
                "-1" -> {
                    binding.systemTheme.isChecked = true
                    binding.darkTheme.isChecked = false
                }
                "1" -> {
                    binding.systemTheme.isChecked = false
                    binding.darkTheme.isChecked = false
                }
                "2" -> {
                    binding.systemTheme.isChecked = false
                    binding.darkTheme.isChecked = true
                }
            }
        }
        updateViewModel.updateResult.observe(viewLifecycleOwner) { result ->
            httpParseSuccess(result) { bean ->
                bean.data?.let { data ->
                    val listener = object : StandardDialog.OnDialogClickListener {
                        override fun onConfirm(dialog: StandardDialog) {
                            val apkUrl = data.download_url
                            val cachePath = CacheUtils.getDirPath(requireActivity(), "apk")
                            val apkName = apkUrl.substring(apkUrl.lastIndexOf("/") + 1)
                            val filePathName = cachePath + File.separator + apkName
                            val file = File(filePathName)
                            if (!file.exists() || !file.isFile) {
                                updateViewModel.downloadApk(apkUrl, filePathName)
                            } else {
                                updateViewModel.downloadApkResult.postValue(
                                    HttpResponse("0", filePathName)
                                )
                            }
                        }

                        override fun onCancel(dialog: StandardDialog) {
                            updateViewModel.updateResult.postValue(null)
                        }
                    }
                    StandardDialog.newInstance()
                        .setTitle("有新版本更新啦♥~")
                        .setContent("当前版本：${requireActivity().getVersionName()}\n最新版本：${data.versionName}")
                        .setOnDialogClickListener(listener)
                        .show(childFragmentManager)
                }
            }
        }
        updateViewModel.downloadApkResult.observe(viewLifecycleOwner) { result ->
            httpParseSuccess(result) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val uri = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val authority = "${requireActivity().packageName}.FileProvider"
                    FileProvider.getUriForFile(requireActivity(), authority, File(it.errorMsg))
                } else {
                    Uri.parse("file://${it.errorMsg}")
                }
                val type = "application/vnd.android.package-archive"
                intent.setDataAndType(uri, type)
                requireActivity().startActivity(intent)
                updateViewModel.downloadApkResult.postValue(null)
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                userViewModel.uiState.collect {
                    binding.logout.visibility =
                        if (it.getUserId().isNotBlank()) View.VISIBLE else View.GONE
                }
            }
        }
        userLoginViewModel.logoutResult().observe(viewLifecycleOwner) { result ->
            httpParseSuccess(result) {
                userViewModel.updateUserBean(UserBean())
                onBackPressed()
            }
        }
        return settingViewModel
    }

}