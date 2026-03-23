package com.v2ray.ang.util

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.v2ray.ang.dto.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppManagerUtil {

  suspend fun loadAppsForPerAppTunneling(context: Context): ArrayList<AppInfo> =
    withContext(Dispatchers.IO) {
      val packageManager = context.packageManager
      val flags = PackageManager.GET_PERMISSIONS.toLong()
      val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags))
      } else {
        packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
      }
      val apps = ArrayList<AppInfo>(packages.size / 4)

      for (pkg in packages) {
        val perms = pkg.requestedPermissions ?: continue
        if (!perms.contains(Manifest.permission.INTERNET)) continue

        val applicationInfo = pkg.applicationInfo ?: continue

        val appName = applicationInfo.loadLabel(packageManager).toString()
        val appIcon = applicationInfo.loadIcon(packageManager) ?: continue
        val isSystemApp = applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0

        apps.add(AppInfo(appName, pkg.packageName, appIcon, isSystemApp, 0))
      }

      apps
    }

  fun getLastUpdateTime(context: Context): Long {
    val pm = context.packageManager
    val pkg = context.packageName
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0L)).lastUpdateTime
    } else {
      pm.getPackageInfo(pkg, 0).lastUpdateTime
    }
  }
}
