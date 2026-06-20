package com.security.rakshakx.web.analyzers

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object GlobalBlocklistManager {
    // Domains explicitly blocked by the user
    private val blockedDomains = ConcurrentHashMap.newKeySet<String>()
    
    // Domains explicitly allowed by the user (whitelisted, bypassing AI blocking)
    private val allowedDomains = ConcurrentHashMap.newKeySet<String>()
    
    // StateFlow for UI to observe rules
    private val _rulesChanged = MutableStateFlow(0)
    val rulesChanged: StateFlow<Int> = _rulesChanged.asStateFlow()

    fun blockDomain(domain: String) {
        allowedDomains.remove(domain.lowercase())
        blockedDomains.add(domain.lowercase())
        _rulesChanged.update { it + 1 }
    }

    fun allowDomain(domain: String) {
        blockedDomains.remove(domain.lowercase())
        allowedDomains.add(domain.lowercase())
        _rulesChanged.update { it + 1 }
    }
    
    fun removeRule(domain: String) {
        blockedDomains.remove(domain.lowercase())
        allowedDomains.remove(domain.lowercase())
        _rulesChanged.update { it + 1 }
    }

    fun isBlocked(domain: String): Boolean {
        return blockedDomains.contains(domain.lowercase())
    }

    fun isAllowed(domain: String): Boolean {
        return allowedDomains.contains(domain.lowercase())
    }
}
