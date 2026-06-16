package com.security.rakshakx.sms

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.util.Calendar

object RiskEngine {

    private const val TAG = "RakshakX_RISK"
    private const val PREFS_NAME = "rakshakx_sender_reputation"

    const val ALERT_THRESHOLD = 40

    fun calculate(text: String, context: Context): Int {
        return calculate(text, sender = null, context = context)
    }

    fun calculate(text: String): Int = calculateRules(text, null, null)

    fun calculate(text: String, sender: String?, context: Context?): Int {
        val score = calculateRules(text, sender, context)
        Log.d(TAG, "Rule score: $score (sender=$sender)")
        return score
    }

    fun recordFlaggedSender(context: Context, sender: String) {
        val prefs = getReputationPrefs(context)
        val key = normalizeSender(sender)
        val count = prefs.getInt(key, 0)
        prefs.edit().putInt(key, count + 1).apply()
    }

    fun getSenderFlagCount(context: Context, sender: String): Int {
        return getReputationPrefs(context).getInt(normalizeSender(sender), 0)
    }

    // ── Core scoring ────────────────────────────────────────────────

    private fun calculateRules(text: String, sender: String?, context: Context?): Int {
        if (text.isBlank()) return 0
        val msg = text.lowercase()
        var risk = 0

        // ── Category 1: Urgency / threat language (weight: 12-18 each) ──
        val urgencyKeywords = mapOf(
            "urgent" to 15, "immediately" to 15, "action required" to 18,
            "blocked" to 12, "suspended" to 14, "deactivated" to 14,
            "terminated" to 16, "last chance" to 18, "final notice" to 18,
            "your account will be" to 18, "will be deactivated" to 16,
            "will be blocked" to 16, "will be suspended" to 16, "will be closed" to 16,
            // Hindi
            "जरूरी" to 15, "तुरंत" to 15, "ब्लॉक" to 12, "बंद" to 10,
            "निलंबित" to 14, "अंतिम चेतावनी" to 18, "कार्रवाई" to 12,
            // Kannada
            "ತುರ್ತು" to 15, "ನಿರ್ಬಂಧಿಸಲಾಗಿದೆ" to 14, "ಮುಚ್ಚಲಾಗಿದೆ" to 14,
            "ಅಮಾನತು" to 14, "ರದ್ದು" to 12,
            // Tamil
            "அவசரம்" to 15, "தடை செய்யப்பட்டது" to 14, "மூடப்பட்டது" to 14,
            "இடைநிறுத்தப்பட்டது" to 14, "உடனடியாக" to 15,
            // Telugu
            "అత్యవసరం" to 15, "బ్లాక్" to 12, "మూసివేయబడింది" to 14,
            "నిలిపివేయబడింది" to 14, "వెంటనే" to 15
        )
        var urgencyHits = 0
        urgencyKeywords.forEach { (kw, weight) ->
            if (msg.contains(kw)) { risk += weight; urgencyHits++ }
        }

        // ── Category 2: Credential / OTP harvesting (weight: 15-22) ──
        val credKeywords = mapOf(
            "otp" to 18, "one time password" to 20, "kyc" to 22, "pin" to 15,
            "password" to 18, "credentials" to 20, "authenticate" to 18,
            "confirm your" to 16, "update your" to 16, "enter your" to 18,
            "submit your" to 16, "verify" to 12,
            // Hindi
            "ओटीपी" to 18, "सत्यापित" to 12, "पासवर्ड" to 18, "केवाईसी" to 22,
            "दर्ज करें" to 16,
            // Kannada
            "ಪರಿಶೀಲಿಸಿ" to 12, "ಪಾಸ್‌ವರ್ಡ್" to 18, "ನಿಮ್ಮ ಒಟಿಪಿ" to 18,
            // Tamil
            "சரிபார்க்கவும்" to 12, "கடவுச்சொல்" to 18, "ஒடிபி" to 18,
            // Telugu
            "ధృవీకరించండి" to 12, "పాస్‌వర్డ్" to 18, "ఓటీపీ" to 18
        )
        var credHits = 0
        credKeywords.forEach { (kw, weight) ->
            if (msg.contains(kw)) { risk += weight; credHits++ }
        }

        // ── Category 3: Financial institution mentions (weight: contextual) ──
        val bankNames = listOf(
            "sbi", "hdfc", "icici", "axis", "kotak", "pnb", "canara",
            "union bank", "rbi", "irdai", "epfo", "uidai",
            "gpay", "phonepe", "paytm",
            "बैंक", "ಬ್ಯಾಂಕ್", "வங்கி", "బ్యాంకు"
        )
        val financialTerms = listOf(
            "credit card", "debit card", "account number", "ifsc", "upi",
            "aadhaar", "income tax", "account", "bank",
            "आधार", "पैन", "क्रेडिट कार्ड", "खाता संख्या",
            "ಆಧಾರ್", "ಕ್ರೆಡಿಟ್ ಕಾರ್ಡ್", "ಖಾತೆ",
            "ஆதார்", "கணக்கு", "கிரெடிட் கார்டு",
            "ఆధార్", "ఖాతా", "క్రెడిట్ కార్డు"
        )

        val hasBankName = bankNames.any { msg.contains(it) }
        val hasFinancialTerm = financialTerms.any { msg.contains(it) }

        if (hasBankName || hasFinancialTerm) {
            // Banking false-positive reduction: legit transaction alerts
            // have patterns like "debited Rs X from A/c XX1234" without urgency/cred harvesting
            val isLikelyLegitAlert = isLegitimateTransactionAlert(msg)
            if (isLikelyLegitAlert && urgencyHits == 0 && credHits == 0) {
                risk += 5
            } else if (isLikelyLegitAlert && credHits == 0) {
                risk += 10
            } else {
                risk += if (hasBankName) 25 else 20
            }
        }

        // ── Category 4: Suspicious URLs (weight: 20-30) ──
        val suspiciousTlds = listOf(".xyz", ".tk", ".ml", ".ga", ".cf", ".top", ".buzz", ".icu")
        val shorteners = listOf("bit.ly", "tinyurl", "t.co", "rb.gy", "cutt.ly", "is.gd")
        val clickBait = listOf(
            "click here", "tap here", "verify now", "open link", "visit now",
            "यहाँ क्लिक करें", "लिंक खोलें",
            "ಇಲ್ಲಿ ಕ್ಲಿಕ್ ಮಾಡಿ", "ಲಿಂಕ್ ತೆರೆಯಿರಿ",
            "இங்கே கிளிக் செய்யவும்", "லிங்கை திறக்கவும்",
            "ఇక్కడ క్లిక్ చేయండి", "లింక్ తెరవండి"
        )

        if (suspiciousTlds.any { msg.contains(it) }) risk += 30
        if (shorteners.any { msg.contains(it) }) risk += 25
        if (clickBait.any { msg.contains(it) }) risk += 20
        val hasUrl = msg.contains("http://") || msg.contains("https://")
        if (hasUrl && !suspiciousTlds.any { msg.contains(it) } && !shorteners.any { msg.contains(it) }) {
            risk += 15
        }

        // ── Category 5: Prize / lottery scams (weight: 12-18) ──
        val prizeKeywords = mapOf(
            "congratulations" to 15, "you have won" to 18, "prize" to 12,
            "lottery" to 18, "reward" to 10, "cashback" to 8,
            "selected" to 8, "lucky winner" to 18, "free gift" to 15,
            "बधाई हो" to 15, "आपने जीता" to 18, "इनाम" to 12, "लॉटरी" to 18, "कैशबैक" to 8,
            "ಅಭಿನಂದನೆಗಳು" to 15, "ನೀವು ಗೆದ್ದಿದ್ದೀರಿ" to 18, "ಬಹುಮಾನ" to 12, "ಲಾಟರಿ" to 18,
            "வாழ்த்துக்கள்" to 15, "நீங்கள் வென்றீர்கள்" to 18, "பரிசு" to 12, "லாட்டரி" to 18,
            "అభినందనలు" to 15, "మీరు గెలిచారు" to 18, "బహుమతి" to 12, "లాటరీ" to 18
        )
        prizeKeywords.forEach { (kw, weight) ->
            if (msg.contains(kw)) risk += weight
        }

        // ── Category 6: Government / authority impersonation (weight: 18-25) ──
        val govKeywords = mapOf(
            "trai" to 20, "police" to 18, "cyber crime" to 22, "court" to 18,
            "legal notice" to 25, "fir" to 20, "arrested" to 25, "warrant" to 25,
            "it department" to 20,
            "पुलिस" to 18, "साइबर अपराध" to 22, "गिरफ्तार" to 25, "कानूनी नोटिस" to 25, "अदालत" to 18,
            "ಪೊಲೀಸ್" to 18, "ಸೈಬರ್ ಅಪರಾಧ" to 22, "ಬಂಧಿಸಲಾಗಿದೆ" to 25, "ನ್ಯಾಯಾಲಯ" to 18,
            "காவல்துறை" to 18, "சைபர் குற்றம்" to 22, "கைது" to 25, "நீதிமன்றம்" to 18,
            "పోలీసులు" to 18, "సైబర్ నేరం" to 22, "అరెస్ట్" to 25, "కోర్టు" to 18
        )
        govKeywords.forEach { (kw, weight) ->
            if (msg.contains(kw)) risk += weight
        }

        // ── Category 7: UPI / Payment fraud ──
        val upiPattern = Regex("""upi://pay\?[^\s]+""", RegexOption.IGNORE_CASE)
        if (upiPattern.containsMatchIn(msg)) risk += 25
        val upiScamKeywords = listOf(
            "collect request", "payment request", "send money now",
            "transfer immediately", "pay now", "pending payment",
            "पैसे भेजो", "भुगतान करो", "ಹಣ ಕಳುಹಿಸಿ", "பணம் அனுப்பு", "డబ్బు పంపండి"
        )
        if (upiScamKeywords.any { msg.contains(it) }) risk += 20

        // ── Category 8: Job / investment scams ──
        val jobScam = listOf(
            "work from home", "earn money", "part time job", "daily income",
            "investment", "double your money", "guaranteed returns", "crypto",
            "trading", "stock tip", "mutual fund offer",
            "घर बैठे कमाएं", "पार्ट टाइम", "निवेश", "दोगुना",
            "ಮನೆಯಿಂದ ಕೆಲಸ", "ಹೂಡಿಕೆ", "ದ್ವಿಗುಣ",
            "வீட்டிலிருந்து வேலை", "முதலீடு",
            "ఇంటి నుండి పని", "పెట్టుబడి"
        )
        if (jobScam.any { msg.contains(it) }) risk += 20

        // ── Contextual modifiers ────────────────────────────────────

        // Time-based weighting: higher risk at unusual hours
        risk = applyTimeWeighting(risk)

        // Sender reputation bonus
        if (sender != null && context != null) {
            val flagCount = getSenderFlagCount(context, sender)
            if (flagCount >= 3) risk += 15
            else if (flagCount >= 1) risk += 8
        }

        // Combination amplifier: urgency + credential harvesting together
        if (urgencyHits >= 2 && credHits >= 1) risk += 10

        return risk.coerceIn(0, 100)
    }

    // ── Banking false-positive detection ────────────────────────────

    private fun isLegitimateTransactionAlert(msg: String): Boolean {
        val txnPatterns = listOf(
            "debited", "credited", "transaction", "txn", "a/c",
            "available balance", "avl bal", "rs.", "rs ", "inr",
            "upi ref", "ref no", "imps", "neft", "rtgs"
        )
        val matchCount = txnPatterns.count { msg.contains(it) }
        return matchCount >= 2
    }

    // ── Time-based weighting ────────────────────────────────────────

    private fun applyTimeWeighting(baseScore: Int): Int {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val multiplier = when (hour) {
            in 0..5 -> 1.2f   // Late night messages are more suspicious
            in 22..23 -> 1.15f // Late evening
            in 9..17 -> 0.95f  // Business hours — slightly lower
            else -> 1.0f
        }
        return (baseScore * multiplier).toInt()
    }

    // ── Utility ─────────────────────────────────────────────────────

    fun severity(score: Int) = when {
        score >= 80 -> "CRITICAL"
        score >= 60 -> "HIGH"
        score >= 40 -> "MEDIUM"
        else        -> "LOW"
    }

    private fun getReputationPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun normalizeSender(sender: String): String {
        return sender.replace(Regex("[^0-9a-zA-Z]"), "").lowercase()
    }
}
