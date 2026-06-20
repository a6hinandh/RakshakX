package com.security.rakshakx.core.correlation

data class AttackTechnique(
    val techniqueId: String,
    val name: String,
    val tactic: AttackTactic,
    val description: String,
    val mitigations: List<String>
)

enum class AttackTactic {
    INITIAL_ACCESS, EXECUTION, PERSISTENCE, PRIVILEGE_ESCALATION,
    DEFENSE_EVASION, CREDENTIAL_ACCESS, DISCOVERY, LATERAL_MOVEMENT,
    COLLECTION, COMMAND_AND_CONTROL, EXFILTRATION, IMPACT,
    RESOURCE_DEVELOPMENT
}

object MitreAttackMapper {

    private val techniqueMap: Map<String, AttackTechnique> = mapOf(
        "SMS_PHISHING" to AttackTechnique(
            techniqueId = "T1660",
            name = "Phishing",
            tactic = AttackTactic.INITIAL_ACCESS,
            description = "Adversary sends malicious SMS to trick users into clicking links or revealing credentials",
            mitigations = listOf(
                "Enable spam/phishing filters on your carrier",
                "Do not click links from unknown senders",
                "Verify sender identity through official channels",
                "Report phishing SMS to your carrier (forward to 7726)"
            )
        ),
        "VOICE_PHISHING" to AttackTechnique(
            techniqueId = "T1660.001",
            name = "Phishing: Spearphishing Voice",
            tactic = AttackTactic.INITIAL_ACCESS,
            description = "Adversary uses voice calls to impersonate trusted entities such as banks, government agencies, or tech support",
            mitigations = listOf(
                "Never share OTPs or passwords over the phone",
                "Hang up and call back using the official number",
                "Enable call screening features on your device",
                "Register with the National Do Not Call Registry"
            )
        ),
        "EMAIL_PHISHING" to AttackTechnique(
            techniqueId = "T1566",
            name = "Phishing",
            tactic = AttackTactic.INITIAL_ACCESS,
            description = "Email-based phishing to steal credentials or install malware on the target device",
            mitigations = listOf(
                "Enable email spam and phishing filters",
                "Verify sender domain before clicking links",
                "Use email clients with built-in phishing protection",
                "Enable multi-factor authentication on all accounts"
            )
        ),
        "MALICIOUS_URL" to AttackTechnique(
            techniqueId = "T1659",
            name = "Content Injection",
            tactic = AttackTactic.INITIAL_ACCESS,
            description = "Adversary injects malicious content into legitimate-looking web pages to steal information or deliver malware",
            mitigations = listOf(
                "Use a browser with built-in phishing protection",
                "Verify SSL certificate and domain before entering credentials",
                "Enable safe browsing in Chrome or Firefox",
                "Use a DNS-level content filter"
            )
        ),
        "CREDENTIAL_HARVEST" to AttackTechnique(
            techniqueId = "T1417",
            name = "Input Capture: Keylogging",
            tactic = AttackTactic.COLLECTION,
            description = "Adversary captures keystrokes on the device to steal credentials and sensitive information",
            mitigations = listOf(
                "Review and audit keyboard app permissions",
                "Use a trusted, reputable keyboard application",
                "Enable Google Play Protect scanning",
                "Change passwords after any suspected compromise"
            )
        ),
        "OTP_FRAUD" to AttackTechnique(
            techniqueId = "T1430",
            name = "Location Tracking",
            tactic = AttackTactic.COLLECTION,
            description = "Adversary intercepts or captures authentication codes via screen reading or SMS interception",
            mitigations = listOf(
                "Use app-based authenticators instead of SMS OTP",
                "Review which apps have accessibility service access",
                "Audit apps with READ_SMS permission",
                "Enable SIM lock / SIM PIN to prevent SIM swap"
            )
        ),
        "DNS_HIJACK" to AttackTechnique(
            techniqueId = "T1584.002",
            name = "Compromise Infrastructure: DNS Server",
            tactic = AttackTactic.RESOURCE_DEVELOPMENT,
            description = "Adversary hijacks DNS resolution to redirect traffic to malicious servers",
            mitigations = listOf(
                "Use encrypted DNS (DNS-over-HTTPS or DNS-over-TLS)",
                "Configure trusted DNS servers (1.1.1.1, 8.8.8.8)",
                "Use a VPN with DNS leak protection",
                "Monitor for unexpected network certificate warnings"
            )
        ),
        "APP_OVERLAY" to AttackTechnique(
            techniqueId = "T1665",
            name = "Hide Artifacts: Suppress Application Icon",
            tactic = AttackTactic.DEFENSE_EVASION,
            description = "Overlay attack hides malicious activity behind legitimate UI elements to capture user input",
            mitigations = listOf(
                "Revoke 'Display over other apps' permission from untrusted apps",
                "Only install apps from trusted sources",
                "Keep Android OS updated for security patches",
                "Review accessibility service permissions regularly"
            )
        ),
        "COORDINATED_ATTACK" to AttackTechnique(
            techniqueId = "T1460",
            name = "Manipulate Device Communication",
            tactic = AttackTactic.COLLECTION,
            description = "Multi-channel coordinated attack using SMS, calls, and email simultaneously to manipulate the user",
            mitigations = listOf(
                "Be suspicious of simultaneous multi-channel contact",
                "Verify identities independently through official channels",
                "Take time to verify before acting on urgent requests",
                "Contact your bank or institution directly if suspicious"
            )
        ),
        "MALWARE_APP" to AttackTechnique(
            techniqueId = "T1476",
            name = "Deliver Malicious App via Other Means",
            tactic = AttackTactic.INITIAL_ACCESS,
            description = "Sideloaded malicious application installed outside official app stores",
            mitigations = listOf(
                "Disable 'Install unknown apps' for all sources",
                "Enable Google Play Protect",
                "Only install apps from the official Play Store",
                "Perform regular security scans with trusted AV software"
            )
        ),
        "SPYWARE" to AttackTechnique(
            techniqueId = "T1418",
            name = "Software Discovery",
            tactic = AttackTactic.DISCOVERY,
            description = "Adversary enumerates installed applications and device capabilities for targeting and profiling",
            mitigations = listOf(
                "Audit app permissions regularly",
                "Remove unused applications",
                "Use Privacy Dashboard to monitor permission usage",
                "Enable Enhanced privacy mode if available"
            )
        ),
        "PRIVILEGE_ABUSE" to AttackTechnique(
            techniqueId = "T1404",
            name = "Exploitation for Privilege Escalation",
            tactic = AttackTactic.PRIVILEGE_ESCALATION,
            description = "Application exploits device vulnerabilities or permissions to gain unauthorized elevated access",
            mitigations = listOf(
                "Keep Android OS and apps updated",
                "Apply security patches promptly",
                "Do not root/jailbreak your device",
                "Review and limit app permissions to minimum required"
            )
        ),
        "DATA_EXFIL" to AttackTechnique(
            techniqueId = "T1437",
            name = "Application Layer Protocol: Web Protocols",
            tactic = AttackTactic.EXFILTRATION,
            description = "Sensitive data exfiltration via HTTP/HTTPS channels disguised as normal traffic",
            mitigations = listOf(
                "Use a firewall app to monitor outgoing connections",
                "Review network permissions granted to apps",
                "Monitor data usage per app for anomalies",
                "Use a VPN to inspect encrypted traffic"
            )
        ),
        "BEACONING" to AttackTechnique(
            techniqueId = "T1571",
            name = "Non-Standard Port",
            tactic = AttackTactic.COMMAND_AND_CONTROL,
            description = "Malware beaconing to C2 server using non-standard ports or intervals to evade detection",
            mitigations = listOf(
                "Use network monitoring tools to detect unusual connections",
                "Block outbound traffic on non-standard ports via firewall",
                "Enable DNS filtering to block known C2 domains",
                "Perform regular network traffic audits"
            )
        ),
        "CRYPTOMINING" to AttackTechnique(
            techniqueId = "T1496",
            name = "Resource Hijacking",
            tactic = AttackTactic.IMPACT,
            description = "Unauthorized use of device CPU/GPU resources for cryptocurrency mining without user consent",
            mitigations = listOf(
                "Monitor battery and CPU usage for unusual spikes",
                "Uninstall apps with unexplained high resource usage",
                "Enable Google Play Protect to detect mining apps",
                "Check app reviews for reports of battery drain"
            )
        )
    )

    fun mapThreat(category: String): AttackTechnique? = techniqueMap[category.uppercase()]

    fun mapMultiple(categories: List<String>): List<AttackTechnique> =
        categories.mapNotNull { mapThreat(it) }.distinctBy { it.techniqueId }

    fun getAllTechniques(): List<AttackTechnique> = techniqueMap.values.toList()

    fun getTacticColor(tactic: AttackTactic): Long = when (tactic) {
        AttackTactic.INITIAL_ACCESS -> 0xFFE53935L
        AttackTactic.EXECUTION -> 0xFFD81B60L
        AttackTactic.PERSISTENCE -> 0xFF8E24AAL
        AttackTactic.PRIVILEGE_ESCALATION -> 0xFF5E35B1L
        AttackTactic.DEFENSE_EVASION -> 0xFF1E88E5L
        AttackTactic.CREDENTIAL_ACCESS -> 0xFF00ACC1L
        AttackTactic.DISCOVERY -> 0xFF00897BL
        AttackTactic.LATERAL_MOVEMENT -> 0xFF43A047L
        AttackTactic.COLLECTION -> 0xFFC0CA33L
        AttackTactic.COMMAND_AND_CONTROL -> 0xFFFB8C00L
        AttackTactic.EXFILTRATION -> 0xFFE53935L
        AttackTactic.IMPACT -> 0xFFB71C1CL
        AttackTactic.RESOURCE_DEVELOPMENT -> 0xFF546E7AL
    }
}
