package com.example.pdm2_project

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.pdm2_project.data.AppDatabase
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class BackupActivity : BaseSidebarActivity() {

    private lateinit var prefs: SharedPreferences

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        val dbFile = getDatabaseFile()
        try {
            AppDatabase.getInstance(this).close()
            AppDatabase.clearInstance()
            FileInputStream(dbFile).use { input ->
                contentResolver.openOutputStream(uri)?.use { output ->
                    input.copyTo(output)
                }
            }
            AppDatabase.getInstance(this)
            registrarBackup()
            atualizarUi()
            Toast.makeText(this, "Backup exportado", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            AppDatabase.getInstance(this)
            Toast.makeText(this, "Erro ao exportar", Toast.LENGTH_SHORT).show()
        }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        val dbFile = getDatabaseFile()
        try {
            AppDatabase.getInstance(this).close()
            AppDatabase.clearInstance()
            apagarArquivosAuxiliaresSqlite(dbFile)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
            AppDatabase.getInstance(this)
            Toast.makeText(this, "Banco restaurado. Reiniciando…", Toast.LENGTH_SHORT).show()
            val i = Intent(this, SplashActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(i)
            exitProcess(0)
        } catch (e: Exception) {
            try {
                AppDatabase.getInstance(this)
            } catch (_: Exception) { }
            Toast.makeText(this, "Erro ao importar", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getContentLayout(): Int = R.layout.activity_backup
    override fun getNavDestination(): NavDestination = NavDestination.BACKUP

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!SessionManager(this).isAdmin()) {
            Toast.makeText(this, "Acesso negado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        super.onCreate(savedInstanceState)
    }

    override fun onContentReady() {
        prefs = getSharedPreferences("backup_meta", MODE_PRIVATE)
        setPageHeader("Backup e Restauração", "Gerencie backups e restaure dados do sistema")

        findViewById<MaterialButton>(R.id.btnExportar).setOnClickListener {
            val nome = "${AppDatabase.DATABASE_NAME.replace(".db", "")}_backup.db"
            exportLauncher.launch(nome)
        }
        findViewById<MaterialButton>(R.id.btnImportar).setOnClickListener {
            importLauncher.launch(arrayOf("*/*"))
        }
        atualizarUi()
    }

    override fun onResume() {
        super.onResume()
        atualizarUi()
    }

    private fun registrarBackup() {
        val now = System.currentTimeMillis()
        val total = prefs.getInt("total_backups", 0) + 1
        prefs.edit()
            .putLong("last_backup", now)
            .putInt("total_backups", total)
            .apply()
    }

    private fun atualizarUi() {
        val last = prefs.getLong("last_backup", 0L)
        val total = prefs.getInt("total_backups", 0)
        val dbFile = getDatabaseFile()
        val sizeBytes = if (dbFile.exists()) dbFile.length() else 0L
        val sizeMb = sizeBytes / (1024.0 * 1024.0)
        val limitMb = 500.0
        val pct = ((sizeMb / limitMb) * 100).toInt().coerceIn(0, 100)

        findViewById<TextView>(R.id.txtUltimoBackupData).text =
            if (last > 0) DateFormats.formatDate(last) else "—"
        findViewById<TextView>(R.id.txtUltimoBackupHora).text =
            if (last > 0) SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(last)) else ""
        findViewById<TextView>(R.id.txtTotalBackups).text = total.toString()
        findViewById<TextView>(R.id.txtEspacoUsado).text =
            String.format(Locale.getDefault(), "%.1f MB", sizeMb)
        findViewById<TextView>(R.id.txtEspacoPct).text = "$pct%"
    }

    private fun getDatabaseFile(): File {
        return applicationContext.getDatabasePath(AppDatabase.DATABASE_NAME)
    }

    private fun apagarArquivosAuxiliaresSqlite(dbFile: File) {
        dbFile.delete()
        File(dbFile.parentFile, dbFile.name + "-wal").delete()
        File(dbFile.parentFile, dbFile.name + "-shm").delete()
    }
}
