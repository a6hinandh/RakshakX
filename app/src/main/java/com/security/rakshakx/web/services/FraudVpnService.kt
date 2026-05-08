package com.security.rakshakx.web.services

import android.app.PendingIntent
import android.content.pm.ServiceInfo
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.security.rakshakx.MainActivity
import com.security.rakshakx.web.analyzers.FraudRiskAnalyzer
import com.security.rakshakx.web.analyzers.ScamLanguageAnalyzer
import com.security.rakshakx.web.analyzers.BrowserNetworkCorrelationEngine
import com.security.rakshakx.web.analyzers.DomainRiskAnalyzer
import com.security.rakshakx.web.analyzers.ThreatBlockingEngine
import com.security.rakshakx.web.analyzers.ThreatIntelRepository
import com.security.rakshakx.web.analyzers.ThreatScoringEngine
import com.security.rakshakx.web.extractors.DnsTrafficAnalyzer
import com.security.rakshakx.web.extractors.PacketParser
import com.security.rakshakx.web.extractors.RedirectChainTracker
import com.security.rakshakx.web.models.FraudAction
import com.security.rakshakx.web.models.ThreatAssessment
import com.security.rakshakx.web.models.VpnTrafficData
import com.security.rakshakx.web.notifications.VpnProtectionNotifier
import com.security.rakshakx.web.utils.BrowserSessionCache
import com.security.rakshakx.web.utils.VpnThreatLogger
import com.security.rakshakx.web.utils.VpnStatusStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FraudVpnService : VpnService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var vpnInterface: ParcelFileDescriptor? = null
    private var dnsRelay: DnsVpnRelay? = null

    private lateinit var notifier: VpnProtectionNotifier
    private lateinit var threatLogger: VpnThreatLogger

    private val parser = PacketParser()
    private val dnsAnalyzer = DnsTrafficAnalyzer()
    private val redirectTracker = RedirectChainTracker()
    private lateinit var riskAnalyzer: DomainRiskAnalyzer
    private lateinit var correlationEngine: BrowserNetworkCorrelationEngine
    private val blockingEngine = ThreatBlockingEngine()
    private lateinit var fraudRiskAnalyzer: FraudRiskAnalyzer
    private lateinit var threatScoringEngine: ThreatScoringEngine
    private lateinit var intelRepository: ThreatIntelRepository
    private val scamLanguageAnalyzer = ScamLanguageAnalyzer()

    override fun onCreate() {
        super.onCreate()
        intelRepository = ThreatIntelRepository(this)
        threatScoringEngine = ThreatScoringEngine(intelRepository)
        riskAnalyzer = DomainRiskAnalyzer(intelRepository)
        correlationEngine = BrowserNetworkCorrelationEngine(intelRepository)
        fraudRiskAnalyzer = FraudRiskAnalyzer(intelRepository, scamLanguageAnalyzer, threatScoringEngine)
        notifier = VpnProtectionNotifier(this)
        notifier.createChannel()
        threatLogger = VpnThreatLogger(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopVpn()
            else -> {
                // Must call startForeground early – Android kills the process if a
                // foreground service doesn't promote itself within ~5 seconds.
                if (Build.VERSION.SDK_INT >= 34) {
                    startForeground(
                        NOTIFICATION_ID,
                        notifier.buildForegroundNotification(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notifier.buildForegroundNotification())
                }
                startVpn()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun startVpn() {
        if (vpnInterface != null) {
            return
        }

        val prepareIntent = prepare(this)
        if (prepareIntent != null) {
            Log.w(TAG, "VPN permission not granted")
            stopSelf()
            return
        }

        val config = Builder()
            .setSession("RakshakX Protection")
            .setMtu(1500)
            .addAddress("10.0.0.2", 24)
            // Use a virtual DNS address on the TUN subnet. Android sends DNS
            // queries here; our relay intercepts and forwards to real servers.
            // This avoids DNS-over-TLS timeouts that cause VPN disconnections.
            .addDnsServer("10.0.0.1")
            .addRoute("10.0.0.1", 32)
            .setConfigureIntent(buildConfigureIntent())

        vpnInterface = config.establish()
        if (vpnInterface == null) {
            Log.e(TAG, "Failed to establish VPN")
            VpnStatusStore.setRunning(false)
            stopSelf()
            return
        }

        // Start the pure-Kotlin DNS relay (replaces tun2socks)
        val relay = DnsVpnRelay(this, vpnInterface!!, serviceScope)
        relay.onDnsPacket = { packet, length -> analyzePacket(packet, length) }
        relay.start()
        dnsRelay = relay

        VpnStatusStore.setRunning(true)
        Log.i(TAG, "VPN started – DNS relay active")
    }

    private fun stopVpn() {
        dnsRelay?.stop()
        dnsRelay = null
        vpnInterface?.close()
        vpnInterface = null
        VpnStatusStore.setRunning(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.i(TAG, "VPN stopped")
    }

    /**
     * Called by the DNS relay for every packet flowing through the TUN.
     * Runs threat analysis on DNS queries inline.
     */
    private fun analyzePacket(packet: ByteArray, length: Int) {
        val parsed = parser.parse(packet, length) ?: return

        val dnsResult = dnsAnalyzer.analyze(parsed)
        val domain = dnsResult?.domain.orEmpty()
        if (domain.isBlank()) return

        val redirects = redirectTracker.track(domain)

        val traffic = VpnTrafficData(
            timestamp = System.currentTimeMillis(),
            protocol = parsed.protocol,
            sourceIp = parsed.sourceIp,
            destinationIp = parsed.destinationIp,
            sourcePort = parsed.sourcePort,
            destinationPort = parsed.destinationPort,
            dnsQuery = domain,
            sniHost = "",
            redirectChain = redirects
        )

        serviceScope.launch {
            threatLogger.logTraffic(traffic)
        }

        val session = BrowserSessionCache.latest()
        val tlsMismatch = traffic.sniHost.isNotBlank() &&
            !traffic.sniHost.equals(domain, ignoreCase = true)

        val riskAssessment = riskAnalyzer.assess(
            domain = domain,
            redirectCount = redirects.size,
            tlsMismatch = tlsMismatch,
            dnsFlags = dnsResult?.reasons ?: emptyList(),
            url = session?.url,
            visibleText = session?.visibleText,
            passwordField = session?.passwordFieldDetected ?: false,
            otpField = session?.otpFieldDetected ?: false,
            emailField = session?.emailFieldDetected ?: false,
            paymentField = session?.paymentFieldDetected ?: false
        )

        val correlation = correlationEngine.correlate(session, domain)
        val finalAssessment = pickHighestRisk(riskAssessment, correlation)

        val fraudResult = if (session != null && finalAssessment != null) {
            fraudRiskAnalyzer.analyze(session, traffic, finalAssessment)
        } else {
            null
        }

        if (finalAssessment != null) {
            serviceScope.launch {
                threatLogger.logThreat(finalAssessment, traffic, session, fraudResult)
            }

            val action = fraudResult?.action ?: if (finalAssessment.action == com.security.rakshakx.web.models.ThreatAction.BLOCK) {
                FraudAction.BLOCK
            } else {
                FraudAction.ALLOW
            }

            val displayDomain = pickDisplayDomain(finalAssessment.domain, session)
            if (blockingEngine.shouldBlock(finalAssessment.domain, action)) {
                val score = fraudResult?.score ?: 0
                val category = formatCategory(fraudResult?.category?.name ?: "UNKNOWN")
                notifier.notifyThreat("Blocked $displayDomain | $category | Score $score")
            } else if (action == FraudAction.WARN) {
                val score = fraudResult?.score ?: 0
                val category = formatCategory(fraudResult?.category?.name ?: "SUSPICIOUS")
                notifier.notifyThreat("Warning $displayDomain | $category | Score $score")
            }
        }
    }

    private fun pickDisplayDomain(domain: String, session: com.security.rakshakx.web.models.BrowserSessionData?): String {
        val visible = extractHost(session?.url.orEmpty())
        return if (looksLikeToken(domain) && visible.isNotBlank()) {
            visible
        } else {
            domain
        }
    }

    private fun looksLikeToken(value: String): Boolean {
        if (value.isBlank()) return true
        if (!value.contains('.')) return true
        val normalized = value.replace("-", "").replace("_", "")
        val longAlphaNum = normalized.length > 32 && normalized.all { it.isLetterOrDigit() }
        return longAlphaNum
    }

    private fun extractHost(url: String): String {
        val trimmed = url.trim()
        val noScheme = trimmed.substringAfter("//", trimmed)
        return noScheme.substringBefore("/")
            .substringBefore(":")
            .lowercase()
    }

    private fun formatCategory(raw: String): String {
        return raw.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    private fun pickHighestRisk(
        primary: ThreatAssessment?,
        secondary: ThreatAssessment?
    ): ThreatAssessment? {
        if (primary == null) return secondary
        if (secondary == null) return primary
        return if (primary.level.ordinal >= secondary.level.ordinal) primary else secondary
    }

    private fun buildConfigureIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        private const val TAG = "RakshakX-VPN"
        const val ACTION_START = "com.security.rakshakx.web.services.START_VPN"
        const val ACTION_STOP = "com.security.rakshakx.web.services.STOP_VPN"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, FraudVpnService::class.java).apply { action = ACTION_START }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, FraudVpnService::class.java).apply { action = ACTION_STOP }
            context.startForegroundService(intent)
        }
    }
}
