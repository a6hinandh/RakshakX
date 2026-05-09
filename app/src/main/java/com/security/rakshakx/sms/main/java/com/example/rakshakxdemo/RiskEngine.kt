package com.example.rakshakxdemo

import android.content.Context
import android.util.Log

/**
 * RiskEngine — rule-based multilingual fraud scorer.
 *
 * Uses keyword/pattern matching across English, Hindi, Kannada, Tamil, Telugu
 * to detect SMS fraud. Fully on-device, no ML model required.
 *
 * ALERT_THRESHOLD = 40
 */
object RiskEngine {

    private const val TAG = "RakshakX_RISK"

    const val ALERT_THRESHOLD = 40

    /**
     * Primary scorer — used by all detection paths.
     */
    fun calculate(text: String, context: Context): Int {
        val score = calculateRules(text)
        Log.d(TAG, "Rule score: $score")
        return score
    }

    /**
     * Overload without context — used by SmsReceiver (BroadcastReceiver).
     */
    fun calculate(text: String): Int = calculateRules(text)

    // -----------------------------------------------------------------------
    // Rule-based engine
    // -----------------------------------------------------------------------

    private fun calculateRules(text: String): Int {
        if (text.isBlank()) return 0
        val msg = text.lowercase()
        var risk = 0

        // ── Urgency / threat language — additive per keyword ──
        val urgency = listOf(
            // English
            "urgent", "immediately", "action required", "blocked",
            "suspended", "deactivated", "terminated", "last chance",
            "final notice", "your account will be", "will be deactivated",
            "will be blocked", "will be suspended", "will be closed",
            // Hindi
            "जरूरी", "तुरंत", "ब्लॉक", "बंद", "खाता", "निलंबित",
            "अंतिम चेतावनी", "कार्रवाई",
            // Kannada
            "ತುರ್ತು", "ನಿರ್ಬಂಧಿಸಲಾಗಿದೆ", "ಮುಚ್ಚಲಾಗಿದೆ", "ನಿಮ್ಮ ಖಾತೆ",
            "ಅಮಾನತು", "ರದ್ದು",
            // Tamil
            "அவசரம்", "தடை செய்யப்பட்டது", "மூடப்பட்டது", "உங்கள் கணக்கு",
            "இடைநிறுத்தப்பட்டது", "உடனடியாக",
            // Telugu
            "అత్యవసరం", "బ్లాక్", "మూసివేయబడింది", "మీ ఖాతా",
            "నిలిపివేయబడింది", "వెంటనే"
        )
        urgency.forEach { if (msg.contains(it)) risk += 20 }

        // ── Credential / OTP harvesting — additive per keyword ──
        val creds = listOf(
            // English
            "verify", "otp", "one time password", "kyc", "pin",
            "password", "credentials", "authenticate", "confirm your",
            "update your", "enter your", "submit your",
            // Hindi
            "ओटीपी", "सत्यापित", "अपडेट", "पासवर्ड", "सत्यापन",
            "केवाईसी", "दर्ज करें",
            // Kannada
            "ಪರಿಶೀಲಿಸಿ", "ಪಾಸ್‌ವರ್ಡ್", "ನವೀಕರಿಸಿ", "ದೃಢೀಕರಿಸಿ",
            "ನಿಮ್ಮ ಒಟಿಪಿ",
            // Tamil
            "சரிபார்க்கவும்", "கடவுச்சொல்", "புதுப்பிக்கவும்",
            "உறுதிப்படுத்தவும்", "ஒடிபி",
            // Telugu
            "ధృవీకరించండి", "పాస్‌వర్డ్", "నవీకరించండి",
            "నిర్ధారించండి", "ఓటీపీ"
        )
        creds.forEach { if (msg.contains(it)) risk += 20 }

        // ── Financial institution impersonation ──
        val banks = listOf(
            // English — bank names & financial terms
            "bank", "sbi", "hdfc", "icici", "axis", "kotak", "pnb", "canara",
            "union bank", "rbi", "irdai", "income tax", "epfo", "uidai", "aadhaar",
            "credit card", "debit card", "account number", "ifsc", "upi",
            "gpay", "phonepe", "paytm", "account",
            // Hindi
            "बैंक", "आधार", "पैन", "क्रेडिट कार्ड", "डेबिट कार्ड", "खाता संख्या",
            // Kannada
            "ಬ್ಯಾಂಕ್", "ಆಧಾರ್", "ಕ್ರೆಡಿಟ್ ಕಾರ್ಡ್", "ಖಾತೆ",
            // Tamil
            "வங்கி", "ஆதார்", "கணக்கு", "கிரெடிட் கார்டு",
            // Telugu
            "బ్యాంకు", "ఆధార్", "ఖాతా", "క్రెడిట్ కార్డు"
        )
        if (banks.any { msg.contains(it) }) risk += 25

        // ── Suspicious URLs ──
        val urls = listOf(
            "http://", "https://", ".xyz", ".tk", ".ml", ".ga", ".cf",
            "bit.ly", "tinyurl", "t.co", "click here", "tap here",
            "verify now", "open link", "visit now",
            // Hindi
            "यहाँ क्लिक करें", "लिंक खोलें",
            // Kannada
            "ಇಲ್ಲಿ ಕ್ಲಿಕ್ ಮಾಡಿ", "ಲಿಂಕ್ ತೆರೆಯಿರಿ",
            // Tamil
            "இங்கே கிளிக் செய்யவும்", "லிங்கை திறக்கவும்",
            // Telugu
            "ఇక్కడ క్లిక్ చేయండి", "లింక్ తెరవండి"
        )
        if (urls.any { msg.contains(it) }) risk += 30

        // ── Prize / lottery scams ──
        val prize = listOf(
            // English
            "congratulations", "you have won", "prize", "lottery",
            "reward", "cashback", "selected", "lucky winner", "free gift",
            // Hindi
            "बधाई हो", "आपने जीता", "इनाम", "लॉटरी", "कैशबैक",
            // Kannada
            "ಅಭಿನಂದನೆಗಳು", "ನೀವು ಗೆದ್ದಿದ್ದೀರಿ", "ಬಹುಮಾನ", "ಲಾಟರಿ",
            // Tamil
            "வாழ்த்துக்கள்", "நீங்கள் வென்றீர்கள்", "பரிசு", "லாட்டரி",
            // Telugu
            "అభినందనలు", "మీరు గెలిచారు", "బహుమతి", "లాటరీ"
        )
        if (prize.any { msg.contains(it) }) risk += 15

        // ── Government / authority impersonation ──
        val gov = listOf(
            // English
            "trai", "police", "cyber crime", "court", "legal notice",
            "fir", "arrested", "warrant", "it department",
            // Hindi
            "पुलिस", "साइबर अपराध", "गिरफ्तार", "कानूनी नोटिस", "अदालत",
            // Kannada
            "ಪೊಲೀಸ್", "ಸೈಬರ್ ಅಪರಾಧ", "ಬಂಧಿಸಲಾಗಿದೆ", "ನ್ಯಾಯಾಲಯ",
            // Tamil
            "காவல்துறை", "சைபர் குற்றம்", "கைது", "நீதிமன்றம்",
            // Telugu
            "పోలీసులు", "సైబర్ నేరం", "అరెస్ట్", "కోర్టు"
        )
        if (gov.any { msg.contains(it) }) risk += 25

        return risk.coerceIn(0, 100)
    }

    fun severity(score: Int) = when {
        score >= 80 -> "CRITICAL"
        score >= 60 -> "HIGH"
        score >= 40 -> "MEDIUM"
        else        -> "LOW"
    }
}