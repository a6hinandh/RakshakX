package com.security.rakshakx.core.family

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FamilyProtectionManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "FamilyProtection"
        private const val PREFS_NAME = "rakshakx_family"
        private const val KEY_ENABLED = "family_mode_enabled"
        private const val KEY_SIMPLIFIED_UI = "simplified_ui"
        private const val KEY_MEMBERS = "family_members"
        private const val KEY_ROLE = "user_role"

        @Volatile
        private var instance: FamilyProtectionManager? = null

        fun getInstance(context: Context): FamilyProtectionManager {
            return instance ?: synchronized(this) {
                instance ?: FamilyProtectionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isEnabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    val isEnabled: StateFlow<Boolean> = _isEnabled

    private val _simplifiedUi = MutableStateFlow(prefs.getBoolean(KEY_SIMPLIFIED_UI, false))
    val simplifiedUi: StateFlow<Boolean> = _simplifiedUi

    private val _userRole = MutableStateFlow(
        FamilyRole.valueOf(prefs.getString(KEY_ROLE, FamilyRole.SELF.name) ?: FamilyRole.SELF.name)
    )
    val userRole: StateFlow<FamilyRole> = _userRole

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _isEnabled.value = enabled
        Log.d(TAG, "Family mode: $enabled")
    }

    fun setSimplifiedUi(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SIMPLIFIED_UI, enabled).apply()
        _simplifiedUi.value = enabled
    }

    fun setRole(role: FamilyRole) {
        prefs.edit().putString(KEY_ROLE, role.name).apply()
        _userRole.value = role
    }

    fun addFamilyMember(member: FamilyMember) {
        val members = getMembers().toMutableList()
        members.removeAll { it.id == member.id }
        members.add(member)
        saveMembers(members)
        Log.d(TAG, "Added family member: ${member.name}")
    }

    fun removeFamilyMember(memberId: String) {
        val members = getMembers().toMutableList()
        members.removeAll { it.id == memberId }
        saveMembers(members)
    }

    fun getMembers(): List<FamilyMember> {
        val raw = prefs.getString(KEY_MEMBERS, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("||").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size >= 3) {
                FamilyMember(
                    id = parts[0],
                    name = parts[1],
                    role = FamilyRole.valueOf(parts[2]),
                    alertOnCritical = parts.getOrNull(3)?.toBooleanStrictOrNull() ?: true
                )
            } else null
        }
    }

    private fun saveMembers(members: List<FamilyMember>) {
        val raw = members.joinToString("||") { "${it.id}|${it.name}|${it.role.name}|${it.alertOnCritical}" }
        prefs.edit().putString(KEY_MEMBERS, raw).apply()
    }
}

enum class FamilyRole(val label: String) {
    ADMIN("Family Admin"),
    ELDER("Elder (Monitored)"),
    CHILD("Child (Protected)"),
    SELF("Self (Solo)")
}

data class FamilyMember(
    val id: String,
    val name: String,
    val role: FamilyRole,
    val alertOnCritical: Boolean = true
)
