package com.security.rakshakx.web.analyzers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.LinkedList

data class DnsQueryRecord(
    val id: Long,
    val domain: String,
    val timestamp: Long = System.currentTimeMillis(),
    var isBlocked: Boolean = false,
    var blockedBy: String? = null
)

object LiveTrafficStream {
    private const val MAX_HISTORY = 1000

    private val _recentQueries = MutableStateFlow<List<DnsQueryRecord>>(emptyList())
    val recentQueries: StateFlow<List<DnsQueryRecord>> = _recentQueries.asStateFlow()

    private val queryList = LinkedList<DnsQueryRecord>()
    private var nextId = 0L

    @Synchronized
    fun addQuery(domain: String, isBlocked: Boolean = false, blockedBy: String? = null) {
        val record = DnsQueryRecord(nextId++, domain, System.currentTimeMillis(), isBlocked, blockedBy)
        queryList.addFirst(record)
        
        if (queryList.size > MAX_HISTORY) {
            queryList.removeLast()
        }
        
        _recentQueries.update { queryList.toList() }
    }
    
    @Synchronized
    fun clear() {
        queryList.clear()
        _recentQueries.update { emptyList() }
    }
}
