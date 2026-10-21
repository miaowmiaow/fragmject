package com.example.fragmject.core.image.editor

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 把默认引擎绑定到 [ImageEditorEngine] 接口，消费方只依赖接口。 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ImageEditorModule {

    @Binds
    @Singleton
    abstract fun bindImageEditorEngine(engine: DefaultImageEditorEngine): ImageEditorEngine
}
