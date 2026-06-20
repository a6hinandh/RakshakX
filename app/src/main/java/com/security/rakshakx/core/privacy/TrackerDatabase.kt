package com.security.rakshakx.core.privacy

import android.content.Context
import android.content.pm.PackageManager

data class TrackerSignature(
    val name: String,
    val category: TrackerCategory,
    val classPatterns: List<String>,
    val domains: List<String>
)

data class TrackerDetection(
    val signature: TrackerSignature,
    val evidenceType: String, // "Package ID" or "Manifest Component"
    val matchedComponent: String // e.g. the package name or specific service/receiver/provider class
)

enum class TrackerCategory {
    ANALYTICS, ADVERTISING, CRASH_REPORTING, FINGERPRINTING, SOCIAL, PROFILING
}

object TrackerDatabase {

    val trackers: List<TrackerSignature> = listOf(
        TrackerSignature(
            name = "Google Analytics",
            category = TrackerCategory.ANALYTICS,
            classPatterns = listOf("com.google.android.gms.analytics", "com.google.analytics"),
            domains = listOf("google-analytics.com", "analytics.google.com", "www.google-analytics.com")
        ),
        TrackerSignature(
            name = "Firebase Analytics",
            category = TrackerCategory.ANALYTICS,
            classPatterns = listOf("com.google.firebase.analytics", "com.google.firebase.perf"),
            domains = listOf("firebase.googleapis.com", "firebaseinstallations.googleapis.com", "firebaselogging.googleapis.com")
        ),
        TrackerSignature(
            name = "Facebook Analytics",
            category = TrackerCategory.SOCIAL,
            classPatterns = listOf("com.facebook.appevents", "com.facebook.analytics", "com.facebook.core"),
            domains = listOf("graph.facebook.com", "www.facebook.com", "connect.facebook.net")
        ),
        TrackerSignature(
            name = "Crashlytics",
            category = TrackerCategory.CRASH_REPORTING,
            classPatterns = listOf("com.crashlytics", "io.fabric.sdk", "com.google.firebase.crashlytics"),
            domains = listOf("reports.crashlytics.com", "firebase-settings.crashlytics.com", "e.crashlytics.com")
        ),
        TrackerSignature(
            name = "Adjust",
            category = TrackerCategory.ANALYTICS,
            classPatterns = listOf("com.adjust.sdk", "com.adjust.nativemodule"),
            domains = listOf("app.adjust.com", "app.adjust.io", "s2s.adjust.com")
        ),
        TrackerSignature(
            name = "AppsFlyer",
            category = TrackerCategory.ANALYTICS,
            classPatterns = listOf("com.appsflyer", "com.af.devkey"),
            domains = listOf("appsflyer.com", "t.appsflyer.com", "launches.appsflyer.com")
        ),
        TrackerSignature(
            name = "Branch.io",
            category = TrackerCategory.ANALYTICS,
            classPatterns = listOf("io.branch", "io.branch.referral"),
            domains = listOf("api.branch.io", "app.link", "bnc.lt")
        ),
        TrackerSignature(
            name = "Amplitude",
            category = TrackerCategory.ANALYTICS,
            classPatterns = listOf("com.amplitude", "com.amplitude.api"),
            domains = listOf("api.amplitude.com", "api2.amplitude.com")
        ),
        TrackerSignature(
            name = "Mixpanel",
            category = TrackerCategory.ANALYTICS,
            classPatterns = listOf("com.mixpanel", "com.mixpanel.android"),
            domains = listOf("api.mixpanel.com", "decide.mixpanel.com", "engage.mixpanel.com")
        ),
        TrackerSignature(
            name = "Segment",
            category = TrackerCategory.ANALYTICS,
            classPatterns = listOf("com.segment.analytics", "com.segment.analytics.android"),
            domains = listOf("api.segment.io", "cdn.segment.com", "api.segment.com")
        ),
        TrackerSignature(
            name = "Hotjar",
            category = TrackerCategory.ANALYTICS,
            classPatterns = listOf("com.hotjar", "hotjar"),
            domains = listOf("static.hotjar.com", "insights.hotjar.com", "vc.hotjar.io")
        ),
        TrackerSignature(
            name = "OneSignal",
            category = TrackerCategory.ADVERTISING,
            classPatterns = listOf("com.onesignal", "com.onesignal.shortcutbadger"),
            domains = listOf("onesignal.com", "api.onesignal.com", "fcm.googleapis.com")
        ),
        TrackerSignature(
            name = "Braze (Appboy)",
            category = TrackerCategory.PROFILING,
            classPatterns = listOf("com.appboy", "com.braze", "bo.app"),
            domains = listOf("sdk.iad-01.braze.com", "sdk.fra-01.braze.eu", "sdk.iad-02.braze.com")
        ),
        TrackerSignature(
            name = "MoPub",
            category = TrackerCategory.ADVERTISING,
            classPatterns = listOf("com.mopub", "com.mopub.mobileads"),
            domains = listOf("ads.mopub.com", "api.mopub.com")
        ),
        TrackerSignature(
            name = "AdMob",
            category = TrackerCategory.ADVERTISING,
            classPatterns = listOf("com.google.android.gms.ads", "com.google.ads"),
            domains = listOf(
                "googleads.g.doubleclick.net",
                "pagead2.googlesyndication.com",
                "mobileads.google.com",
                "admob.googleapis.com"
            )
        ),
        TrackerSignature(
            name = "IronSource",
            category = TrackerCategory.ADVERTISING,
            classPatterns = listOf("com.ironsource", "com.ironsource.mediationsdk"),
            domains = listOf("sdk.serving.ironsrc.com", "outcome-ssp.supersonicads.com")
        ),
        TrackerSignature(
            name = "Vungle",
            category = TrackerCategory.ADVERTISING,
            classPatterns = listOf("com.vungle", "com.vungle.warren"),
            domains = listOf("ads.api.vungle.com", "events.api.vungle.com")
        ),
        TrackerSignature(
            name = "Unity Ads",
            category = TrackerCategory.ADVERTISING,
            classPatterns = listOf("com.unity3d.ads", "com.unity3d.services"),
            domains = listOf(
                "auction.unityads.unity3d.com",
                "config.unityads.unity3d.com",
                "publisher-event.unityads.unity3d.com"
            )
        ),
        TrackerSignature(
            name = "Flurry",
            category = TrackerCategory.ANALYTICS,
            classPatterns = listOf("com.flurry", "com.flurry.android"),
            domains = listOf("data.flurry.com", "analytics.yahoo.com")
        ),
        TrackerSignature(
            name = "Comscore",
            category = TrackerCategory.ANALYTICS,
            classPatterns = listOf("com.comscore", "com.scorecardresearch"),
            domains = listOf("sb.scorecardresearch.com", "b.scorecardresearch.com")
        ),
        TrackerSignature(
            name = "Taboola",
            category = TrackerCategory.ADVERTISING,
            classPatterns = listOf("com.taboola", "com.taboola.android"),
            domains = listOf("trc.taboola.com", "cdn.taboola.com", "api.taboola.com")
        ),
        TrackerSignature(
            name = "InMobi",
            category = TrackerCategory.ADVERTISING,
            classPatterns = listOf("com.inmobi", "com.inmobi.ads"),
            domains = listOf("api.w.inmobi.com", "i.w.inmobi.com", "cf.w.inmobi.com")
        ),
        TrackerSignature(
            name = "Kochava",
            category = TrackerCategory.ANALYTICS,
            classPatterns = listOf("com.kochava", "com.kochava.base"),
            domains = listOf("control.kochava.com", "tracking.kochava.com")
        ),
        TrackerSignature(
            name = "Singular",
            category = TrackerCategory.ANALYTICS,
            classPatterns = listOf("com.singular.sdk", "com.singular.sdk.internal"),
            domains = listOf("sdk-api.singular.net", "api.singular.net")
        ),
        TrackerSignature(
            name = "Bugsnag",
            category = TrackerCategory.CRASH_REPORTING,
            classPatterns = listOf("com.bugsnag", "com.bugsnag.android"),
            domains = listOf("sessions.bugsnag.com", "notify.bugsnag.com")
        ),
        TrackerSignature(
            name = "Sentry",
            category = TrackerCategory.CRASH_REPORTING,
            classPatterns = listOf("io.sentry", "io.sentry.android"),
            domains = listOf("sentry.io", "o0.ingest.sentry.io")
        ),
        TrackerSignature(
            name = "DataDog",
            category = TrackerCategory.ANALYTICS,
            classPatterns = listOf("com.datadog", "com.datadog.android"),
            domains = listOf("browser-intake-datadoghq.com", "rum.browser-intake-datadoghq.com")
        ),
        TrackerSignature(
            name = "TikTok / ByteDance",
            category = TrackerCategory.PROFILING,
            classPatterns = listOf("com.bytedance.sdk", "com.bytedance.applog", "com.tiktok"),
            domains = listOf("analytics.tiktok.com", "log.tiktok.com", "mon.snssdk.com")
        ),
        TrackerSignature(
            name = "Twitter / X Ads",
            category = TrackerCategory.ADVERTISING,
            classPatterns = listOf("com.twitter.mopub", "com.twitter.sdk.android", "com.twitter.android"),
            domains = listOf("ads.twitter.com", "analytics.twitter.com", "t.co")
        ),
        TrackerSignature(
            name = "Chartboost",
            category = TrackerCategory.ADVERTISING,
            classPatterns = listOf("com.chartboost.sdk", "com.chartboost"),
            domains = listOf("live.chartboost.com", "banner.chartboost.com")
        ),
        TrackerSignature(
            name = "AppLovin",
            category = TrackerCategory.ADVERTISING,
            classPatterns = listOf("com.applovin", "com.applovin.sdk"),
            domains = listOf("ms.applovin.com", "rt.applovin.com", "d.applovin.com")
        ),
        TrackerSignature(
            name = "Snap Analytics",
            category = TrackerCategory.SOCIAL,
            classPatterns = listOf("com.snap.analytics", "com.snapchat.kit.sdk"),
            domains = listOf("tr.snapchat.com", "sc-analytics.appspot.com")
        ),
        TrackerSignature(
            name = "Pinterest Analytics",
            category = TrackerCategory.SOCIAL,
            classPatterns = listOf("com.pinterest", "com.pinterest.sdk"),
            domains = listOf("api.pinterest.com", "log.pinterest.com", "trk.pinterest.com")
        ),
        TrackerSignature(
            name = "CleverTap",
            category = TrackerCategory.PROFILING,
            classPatterns = listOf("com.clevertap", "com.clevertap.android.sdk"),
            domains = listOf("in1.api.clevertap.com", "eu1.api.clevertap.com")
        ),
        TrackerSignature(
            name = "MoEngage",
            category = TrackerCategory.PROFILING,
            classPatterns = listOf("com.moengage", "com.moe.pushlibrary"),
            domains = listOf("sdk-01.moengage.com", "sdk-02.moengage.com")
        ),
        TrackerSignature(
            name = "Optimizely",
            category = TrackerCategory.ANALYTICS,
            classPatterns = listOf("com.optimizely", "com.optimizely.ab"),
            domains = listOf("api.optimizely.com", "logx.optimizely.com", "cdn.optimizely.com")
        ),
        TrackerSignature(
            name = "New Relic",
            category = TrackerCategory.ANALYTICS,
            classPatterns = listOf("com.newrelic", "com.newrelic.agent"),
            domains = listOf("mobile-collector.newrelic.com", "crash-collector.mobile.newrelic.com")
        ),
        TrackerSignature(
            name = "Intercom",
            category = TrackerCategory.PROFILING,
            classPatterns = listOf("io.intercom", "io.intercom.android"),
            domains = listOf("api.intercom.io", "nexus-websocket-a.intercom.io")
        ),
        TrackerSignature(
            name = "Localytics",
            category = TrackerCategory.ANALYTICS,
            classPatterns = listOf("com.localytics", "com.localytics.android"),
            domains = listOf("analytics.localytics.com", "profile.localytics.com")
        ),
        TrackerSignature(
            name = "Apptimize",
            category = TrackerCategory.ANALYTICS,
            classPatterns = listOf("com.apptimize", "com.apptimize.apptimize"),
            domains = listOf("apptimize.com", "sdk.apptimize.com")
        ),
        TrackerSignature(
            name = "Leanplum",
            category = TrackerCategory.PROFILING,
            classPatterns = listOf("com.leanplum", "com.leanplum.internal"),
            domains = listOf("api.leanplum.com", "www.leanplum.com")
        ),
        TrackerSignature(
            name = "Rollbar",
            category = TrackerCategory.CRASH_REPORTING,
            classPatterns = listOf("com.rollbar", "com.rollbar.android"),
            domains = listOf("api.rollbar.com")
        ),
        TrackerSignature(
            name = "Heap Analytics",
            category = TrackerCategory.ANALYTICS,
            classPatterns = listOf("io.heap", "com.heapanalytics"),
            domains = listOf("heapanalytics.com", "api.heapanalytics.com")
        ),
        TrackerSignature(
            name = "FullStory",
            category = TrackerCategory.FINGERPRINTING,
            classPatterns = listOf("com.fullstory", "com.fullstory.android"),
            domains = listOf("fullstory.com", "rs.fullstory.com", "edge.fullstory.com")
        ),
        TrackerSignature(
            name = "Countly",
            category = TrackerCategory.ANALYTICS,
            classPatterns = listOf("ly.count", "ly.count.android"),
            domains = listOf("cloud.count.ly")
        ),
        TrackerSignature(
            name = "Nielsen",
            category = TrackerCategory.FINGERPRINTING,
            classPatterns = listOf("com.nielsen", "com.nielsen.android"),
            domains = listOf("secure-us.imrworldwide.com", "cdn-gl.imrworldwide.com")
        ),
        TrackerSignature(
            name = "Doubleclick",
            category = TrackerCategory.ADVERTISING,
            classPatterns = listOf("com.google.doubleclick", "com.doubleclick"),
            domains = listOf("doubleclick.net", "ad.doubleclick.net", "stats.g.doubleclick.net")
        ),
        TrackerSignature(
            name = "Appsee",
            category = TrackerCategory.FINGERPRINTING,
            classPatterns = listOf("com.appsee", "com.appsee.api"),
            domains = listOf("api.appsee.com", "cdn.appsee.com")
        ),
        TrackerSignature(
            name = "Instabug",
            category = TrackerCategory.CRASH_REPORTING,
            classPatterns = listOf("com.instabug", "com.instabug.library"),
            domains = listOf("api.instabug.com", "inst.bug")
        ),
        TrackerSignature(
            name = "Salesforce Marketing Cloud",
            category = TrackerCategory.PROFILING,
            classPatterns = listOf("com.salesforce.marketingcloud", "com.exacttarget"),
            domains = listOf("exacttarget.com", "push.exacttarget.com")
        ),
        TrackerSignature(
            name = "Adobe Analytics",
            category = TrackerCategory.ANALYTICS,
            classPatterns = listOf("com.adobe.analytics", "com.adobe.mobile"),
            domains = listOf("sc.omtrdc.net", "adobedtm.com", "omtrdc.net")
        )
    )

    fun getBlockDomains(): List<String> =
        trackers.flatMap { it.domains }.distinct()

    fun detectTrackers(
        context: Context,
        installedPackages: List<String>,
        deepScan: Boolean = false,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): Map<String, List<TrackerDetection>> {
        val result = mutableMapOf<String, MutableList<TrackerDetection>>()
        val pm = context.packageManager

        installedPackages.forEachIndexed { index, pkg ->
            onProgress(index, pkg)

            // 1. Quick Scan - Check package name contains pattern
            val matchedByPkg = trackers.filter { tracker ->
                tracker.classPatterns.any { pattern -> pkg.contains(pattern, ignoreCase = true) }
            }

            if (matchedByPkg.isNotEmpty()) {
                val detections = matchedByPkg.map {
                    TrackerDetection(
                        signature = it,
                        evidenceType = "Package ID",
                        matchedComponent = pkg
                    )
                }
                result.getOrPut(pkg) { mutableListOf() }.addAll(detections)
            }

            // 2. Deep Scan - Scan services, receivers, providers
            if (deepScan) {
                try {
                    val packageInfo = pm.getPackageInfo(
                        pkg,
                        PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS
                    )

                    val services = packageInfo.services?.map { "Service: ${it.name}" } ?: emptyList()
                    val receivers = packageInfo.receivers?.map { "Receiver: ${it.name}" } ?: emptyList()
                    val providers = packageInfo.providers?.map { "Provider: ${it.name}" } ?: emptyList()

                    val allComponents = services + receivers + providers

                    if (allComponents.isNotEmpty()) {
                        val matchedByComp = trackers.filter { tracker ->
                            tracker.classPatterns.any { pattern ->
                                allComponents.any { comp -> comp.contains(pattern, ignoreCase = true) }
                            }
                        }

                        val detections = matchedByComp.mapNotNull { tracker ->
                            // Find the specific component that matched
                            val firstMatch = allComponents.firstOrNull { comp ->
                                tracker.classPatterns.any { pattern -> comp.contains(pattern, ignoreCase = true) }
                            }
                            if (firstMatch != null) {
                                TrackerDetection(
                                    signature = tracker,
                                    evidenceType = "Manifest Component",
                                    matchedComponent = firstMatch
                                )
                            } else {
                                null
                            }
                        }

                        if (detections.isNotEmpty()) {
                            val existing = result.getOrPut(pkg) { mutableListOf() }
                            for (det in detections) {
                                if (existing.none { it.signature.name == det.signature.name }) {
                                    existing.add(det)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore package not found or security exceptions
                }
            }
        }
        return result
    }

    fun countByCategory(trackers: List<TrackerSignature>): Map<TrackerCategory, Int> =
        trackers.groupingBy { it.category }.eachCount()

    fun countDetectionsByCategory(detections: List<TrackerDetection>): Map<TrackerCategory, Int> =
        detections.map { it.signature }.groupingBy { it.category }.eachCount()
}
