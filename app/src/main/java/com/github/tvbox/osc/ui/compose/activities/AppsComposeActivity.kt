package com.github.tvbox.osc.ui.compose.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.github.tvbox.osc.bean.AppInfo
import com.github.tvbox.osc.ui.compose.screens.AppsScreen
import com.github.tvbox.osc.ui.compose.theme.TVBoxTheme
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import com.github.tvbox.osc.base.App
import java.util.ArrayList

class AppsComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TVBoxTheme {
                var apps by remember { mutableStateOf(emptyList<AppInfo>()) }

                LaunchedEffect(Unit) {
                    val appList = getInstallApps(this@AppsComposeActivity).toMutableList()
                    AppInfo.Sorter.sort(appList)
                    apps = appList
                }

                AppsScreen(
                    apps = apps,
                    onAppClick = { appInfo ->
                        try {
                            startActivity(packageManager.getLaunchIntentForPackage(appInfo.pack))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
            }
        }
    }

    private fun getInstallApps(context: Context): MutableList<AppInfo> {
        val items: MutableList<AppInfo> = ArrayList()
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (app in apps) {
            if (pm.getLaunchIntentForPackage(app.packageName) != null) {
                if (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0) {
                } else if (app.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
                } else {
                    if (app.packageName != context.packageName) {
                        items.add(AppInfo.get(app))
                    }
                }
            }
        }
        return items
    }
}
