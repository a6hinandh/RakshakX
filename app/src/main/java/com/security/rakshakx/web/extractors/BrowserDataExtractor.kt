package com.security.rakshakx.web.extractors

import android.text.InputType
import android.view.accessibility.AccessibilityNodeInfo
import com.security.rakshakx.web.models.BrowserSessionData

class BrowserDataExtractor {
    private val supportedBrowsers = setOf(
        "com.android.chrome",
        "com.microsoft.emmx",
        "com.brave.browser"
    )

    private val urlBarIds = mapOf(
        "com.android.chrome" to "com.android.chrome:id/url_bar",
        "com.microsoft.emmx" to "com.microsoft.emmx:id/url_bar",
        "com.brave.browser" to "com.brave.browser:id/url_bar"
    )

    fun isSupportedBrowser(packageName: String?): Boolean {
        return packageName != null && supportedBrowsers.contains(packageName)
    }

    fun extractSession(
        root: AccessibilityNodeInfo,
        browserPackage: String
    ): BrowserSessionData {
        val url = extractUrl(root, browserPackage)
        val pageTitle = root.window?.title?.toString().orEmpty()

        val visibleText = extractVisibleText(root)
        val fieldSignals = detectInputFields(root)

        return BrowserSessionData(
            timestamp = System.currentTimeMillis(),
            browserPackage = browserPackage,
            url = url,
            pageTitle = pageTitle,
            visibleText = visibleText,
            passwordFieldDetected = fieldSignals.password,
            otpFieldDetected = fieldSignals.otp,
            emailFieldDetected = fieldSignals.email,
            paymentFieldDetected = fieldSignals.payment
        )
    }

    private fun extractUrl(root: AccessibilityNodeInfo, browserPackage: String): String {
        val urlBarId = urlBarIds[browserPackage]
        if (urlBarId.isNullOrEmpty()) {
            return ""
        }

        var urlText = ""
        traverse(root) { node ->
            val id = node.viewIdResourceName
            if (id == urlBarId) {
                val text = node.text?.toString().orEmpty()
                val desc = node.contentDescription?.toString().orEmpty()
                urlText = if (text.isNotBlank()) text else desc
                return@traverse true
            }
            false
        }

        return urlText
    }

    private fun extractVisibleText(root: AccessibilityNodeInfo): String {
        val parts = LinkedHashSet<String>()

        traverse(root) { node ->
            val text = node.text?.toString()?.trim().orEmpty()
            val desc = node.contentDescription?.toString()?.trim().orEmpty()

            if (text.isNotBlank()) {
                parts.add(text)
            } else if (desc.isNotBlank()) {
                parts.add(desc)
            }

            false
        }

        if (parts.isEmpty()) {
            return ""
        }

        val summary = parts.joinToString(" | ")
        return if (summary.length > 800) {
            summary.substring(0, 800)
        } else {
            summary
        }
    }

    private fun detectInputFields(root: AccessibilityNodeInfo): FieldSignals {
        var passwordDetected = false
        var otpDetected = false
        var emailDetected = false
        var paymentDetected = false

        traverse(root) { node ->
            if (node.className == "android.widget.EditText") {
                val hint = node.hintText?.toString()?.lowercase().orEmpty()
                val id = node.viewIdResourceName?.lowercase().orEmpty()
                val inputType = node.inputType

                if (node.isPassword || inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD != 0) {
                    passwordDetected = true
                }

                if (hint.contains("otp") || hint.contains("one time") || hint.contains("verification") || id.contains("otp")) {
                    otpDetected = true
                }

                if (
                    inputType and InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS != 0 ||
                    hint.contains("email") || id.contains("email")
                ) {
                    emailDetected = true
                }

                if (
                    hint.contains("card") ||
                    hint.contains("cvv") ||
                    hint.contains("expiry") ||
                    hint.contains("expiration") ||
                    hint.contains("upi") ||
                    hint.contains("debit") ||
                    hint.contains("credit") ||
                    hint.contains("pin") ||
                    id.contains("card") ||
                    id.contains("cvv") ||
                    id.contains("expiry") ||
                    id.contains("pin")
                ) {
                    paymentDetected = true
                }
            }

            passwordDetected && otpDetected && emailDetected && paymentDetected
        }

        return FieldSignals(passwordDetected, otpDetected, emailDetected, paymentDetected)
    }

    private fun traverse(
        node: AccessibilityNodeInfo,
        visitor: (AccessibilityNodeInfo) -> Boolean
    ) {
        if (visitor(node)) {
            return
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            traverse(child, visitor)
        }
    }

    private data class FieldSignals(
        val password: Boolean,
        val otp: Boolean,
        val email: Boolean,
        val payment: Boolean
    )
}
