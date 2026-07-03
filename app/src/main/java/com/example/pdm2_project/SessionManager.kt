package com.example.pdm2_project

import android.content.Context
import com.example.pdm2_project.data.UserRole

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun salvarSessao(userId: Long, login: String, role: UserRole) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED, true)
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_LOGIN, login)
            .putString(KEY_ROLE, role.name)
            .apply()
    }

    fun limparSessao() {
        prefs.edit().clear().apply()
    }

    fun isLogged(): Boolean = prefs.getBoolean(KEY_IS_LOGGED, false)

    fun getUserId(): Long = prefs.getLong(KEY_USER_ID, -1L)

    fun getLogin(): String = prefs.getString(KEY_LOGIN, "").orEmpty()

    fun getRole(): UserRole {
        val name = prefs.getString(KEY_ROLE, null) ?: return UserRole.CLIENT
        return try {
            UserRole.valueOf(name)
        } catch (_: IllegalArgumentException) {
            UserRole.CLIENT
        }
    }

    fun isAdmin(): Boolean = getRole() == UserRole.ADMIN

    companion object {
        private const val PREFS_NAME = "barber_app_session"
        private const val KEY_IS_LOGGED = "is_logged"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_LOGIN = "login"
        private const val KEY_ROLE = "role"
    }
}
