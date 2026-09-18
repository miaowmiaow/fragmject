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
        maven {
            url = uri("repo")
        }
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
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

rootProject.name = "fragmject"

include(":app")
include(":core:common")
include(":core:data")
include(":core:database")
include(":core:designsystem")
include(":core:domain")
include(":core:model")
include(":core:navigation")
include(":core:network")
include(":core:player")
include(":core:ui")
include(":feature:article:api")
include(":feature:article:impl")
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