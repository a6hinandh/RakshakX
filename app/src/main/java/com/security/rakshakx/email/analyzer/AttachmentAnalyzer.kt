package com.security.rakshakx.email.analyzer

object AttachmentAnalyzer {

    private val dangerousExtensions = listOf(

        ".apk",
        ".exe",
        ".bat",
        ".cmd",
        ".scr",
        ".zip",
        ".rar"
    )

    fun hasDangerousAttachment(text: String): Boolean {

        return dangerousExtensions.any {
            text.contains(it, true)
        }
    }
}