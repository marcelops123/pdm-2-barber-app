package com.example.pdm2_project

import android.app.Application
import com.example.pdm2_project.data.AppDatabase
import com.example.pdm2_project.data.User
import com.example.pdm2_project.data.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class BarberApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        runBlocking(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@BarberApplication)
            if (db.userDao().count() == 0L) {
                val login = getString(R.string.default_admin_login)
                val pass = getString(R.string.default_admin_password)
                val user = User(
                    login = login,
                    passwordHash = PasswordHasher.hash(pass),
                    role = UserRole.ADMIN
                )
                db.userDao().insert(user)
            }
        }
    }
}

fun android.content.Context.appDatabase(): AppDatabase {
    return AppDatabase.getInstance(applicationContext)
}
