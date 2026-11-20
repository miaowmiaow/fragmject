pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        // Maven Central 的 Google 托管镜像，纯兜底：只有前面的仓库拿不到构件时才会请求。
        // 起因：CI（GitHub Actions）上 lint 曾因 `com.google.errorprone:error_prone_annotations:2.30.0`
        // （espresso-core 3.7.0 的传递依赖）在 repo.maven.apache.org 上解析不到而整条链路失败，
        // 并进一步被配置缓存序列化放大成 BUILD FAILED。该构件在 Central 与这个镜像上都存在，
        // 属于出口偶发缺失，因此不改版本、不排除依赖，只增加一个可获取的来源。
        maven {
            url = uri("https://maven-central.storage-download.googleapis.com/maven2/")
        }
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

rootProject.name = "fragmject"

include(":app")
// Macrobenchmark / Baseline Profile 采集模块：只跑仪器测试，不参与生产打包
include(":benchmark")
// DEX 壳的壳代码（H6/S1）：纯 Java、零依赖，只作为构建产物被打进 APK，
// **不进入任何 runtime classpath**（:app 不依赖它，避免壳类混进业务 dex）
include(":shell")
// 运行期安全防护库（可独立交付给其他项目）
include(":security")
include(":core:android-platform")
include(":core:data-repository")
include(":core:data-contract")
include(":core:database")
include(":core:designsystem")
include(":core:domain")
include(":core:image-editor")
include(":core:model")
include(":core:navigation-runtime")
include(":core:navigation-contract")
include(":core:network")
// 漂浮宠物：跨全部页面的全局装饰层，由组合根挂载，feature 不得使用（见 core/pet 与守卫矩阵）
include(":core:pet")
include(":core:qrscanner")
include(":core:testing")
include(":core:ui")
include(":core:webview")
include(":feature:article:api")
include(":feature:article:impl")
include(":feature:authenticator:api")
include(":feature:authenticator:impl")
include(":feature:auth:api")
include(":feature:auth:impl")
include(":feature:collection:api")
include(":feature:collection:impl")
include(":feature:demo:api")
include(":feature:demo:impl")
include(":feature:home:api")
include(":feature:home:impl")
include(":feature:picture:api")
include(":feature:picture:impl")
include(":feature:search:api")
include(":feature:search:impl")
include(":feature:user:api")
include(":feature:user:impl")