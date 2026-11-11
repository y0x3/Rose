package com.tiempo.app.service.assistant

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class DeviceCapabilities(
    val totalRamMb: Long,
    val availableRamMb: Long,
    val freeStorageMb: Long,
    val is64Bit: Boolean,
    val androidSdk: Int,
    val lowRamDevice: Boolean
) {
    /** Clasificación orientativa; la decisión final la toma la prueba de velocidad. */
    val tier: LocalTier
        get() = when {
            !is64Bit || freeStorageMb < 1_500 || totalRamMb < 3_000 -> LocalTier.NONE
            totalRamMb < 6_000 || lowRamDevice -> LocalTier.SMALL
            else -> LocalTier.MEDIUM
        }
}

enum class LocalTier { NONE, SMALL, MEDIUM }

@Singleton
class DeviceCapabilityChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun check(): DeviceCapabilities {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val stat = StatFs(Environment.getDataDirectory().path)

        return DeviceCapabilities(
            totalRamMb = info.totalMem / MB,
            availableRamMb = info.availMem / MB,
            freeStorageMb = stat.availableBytes / MB,
            is64Bit = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty(),
            androidSdk = Build.VERSION.SDK_INT,
            lowRamDevice = am.isLowRamDevice
        )
    }

    private companion object {
        const val MB = 1024L * 1024L
    }
}