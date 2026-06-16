package com.security.rakshakx.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.security.rakshakx.MainActivity
import com.security.rakshakx.R
import com.security.rakshakx.core.SettingsStore
import com.security.rakshakx.web.utils.VpnStatusStore

class SecurityWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "SecurityWidget"

        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, SecurityWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                val intent = Intent(context, SecurityWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_security_status)

            val settings = SettingsStore.getInstance(context)
            val smsEnabled = settings.smsEnabled.value
            val callEnabled = settings.callEnabled.value
            val emailEnabled = settings.emailEnabled.value
            val vpnRunning = VpnStatusStore.isRunning.value

            val activeCount = listOf(smsEnabled, callEnabled, emailEnabled, vpnRunning).count { it }
            val score = 60 + (activeCount * 10)

            views.setTextViewText(R.id.tvWidgetScore, "$score")
            views.setTextViewText(R.id.tvWidgetStatus,
                when {
                    activeCount == 4 -> "All Channels Active"
                    activeCount >= 2 -> "$activeCount/4 Channels Active"
                    activeCount == 1 -> "Limited Protection"
                    else -> "Protection Disabled"
                }
            )
            views.setTextViewText(R.id.tvWidgetChannels,
                buildString {
                    if (smsEnabled) append("SMS ")
                    if (callEnabled) append("CALL ")
                    if (emailEnabled) append("EMAIL ")
                    if (vpnRunning) append("WEB")
                }.trim()
            )

            val launchIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)

            manager.updateAppWidget(widgetId, views)
        } catch (e: Exception) {
            Log.e(TAG, "Widget update failed", e)
        }
    }
}
