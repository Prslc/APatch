plugins {
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
}

extra.set("kernelPatchVersion", "0.13.4")
extra.set("kernelPatchHash", getKernelPatchHash())
extra.set("androidMinSdkVersion", 26)
extra.set("androidTargetSdkVersion", 37)
extra.set("androidCompileSdkVersion", 37)
extra.set("androidBuildToolsVersion", "36.1.0")
extra.set("androidCompileNdkVersion", "29.0.14206865")
extra.set("managerVersionCode", getVersionCode())
extra.set("managerVersionName", getVersionName())
extra.set("branchName", getBranch())

fun Project.exec(command: String) = providers.exec {
    commandLine(command.split(" "))
}.standardOutput.asText.get().trim()

fun getGitCommitCount(): Int {
    return exec("git rev-list --count HEAD").trim().toInt()
}

fun getGitDescribe(): String {
    return exec("git rev-parse --verify --short HEAD").trim()
}

fun getVersionCode(): Int {
    val commitCount = getGitCommitCount()
    val major = 1
    return major * 10000 + commitCount + 200
}

fun getBranch(): String {
    return exec("git rev-parse --abbrev-ref HEAD").trim()
}

fun getVersionName(): String {
    return getGitDescribe()
}

fun getKernelPatchHash(): String {
    System.getProperty("kpHash")?.let { return it }
    val version = extra.get("kernelPatchVersion") as String
    return try {
        exec("git ls-remote https://github.com/bmax121/KernelPatch.git refs/tags/$version | " +
            "awk '{print substr(\$1,1,7)}'")
    } catch (_: Exception) {
        "unknown"
    }
}

tasks.register("printVersion") {
    group = "help"
    description = "Prints the current project version code and name."
    doLast {
        println("Version code: ${project.extra.get("managerVersionCode")}")
        println("Version name: ${project.extra.get("managerVersionName")}")
    }
}
