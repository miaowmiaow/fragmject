package com.example.fragmject.core.data.repository.media

import android.graphics.Bitmap
import com.example.fragmject.core.domain.media.ImageHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * 平台位图的 [ImageHandle] 实现：位图本体只在本模块内持有。
 *
 * 设计为 public 而非 internal：UI 侧（如图片编辑/裁剪画布）持有真实
 * [android.graphics.Bitmap]，需要把合成结果交给 [com.example.fragmject.core.domain.media.MediaEditor]，
 * 因此必须能构造本句柄。除此之外 UI 不接触 data 层任何类型。
 *
 * **例外声明（不得扩散）**：这是全项目**唯一**允许 feature 直接引用的 data 层类型，
 * 由三重机制锁定：
 * - Gradle 守卫：`FEATURE_EXTRA_CORE_DEPENDENCIES` 仅放行 `:feature:picture:impl`；
 * - 架构测试规则 12：feature 侧仅放行本类的全限定名；
 * - 架构测试规则 16：本模块的 `media` 包对外只暴露本类，其余实现必须 `internal`。
 *
 * 上移本类到 `core:domain` **不可行**：它实现了 [ImageHandle.obtainPngBytes]
 * （`Bitmap.compress` + 线程切换），属于平台行为，进 domain 会破坏「领域层无平台实现」
 * 这一规则 8 的实质理由。
 */
class BitmapImageHandle(val bitmap: Bitmap) : ImageHandle {

    override val width: Int get() = bitmap.width

    override val height: Int get() = bitmap.height

    override suspend fun obtainPngBytes(): ByteArray? = withContext(Dispatchers.Default) {
        runCatching {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            baos.toByteArray()
        }.getOrNull()
    }
}
