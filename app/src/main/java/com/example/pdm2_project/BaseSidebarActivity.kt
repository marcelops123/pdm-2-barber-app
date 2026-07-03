package com.example.pdm2_project

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

abstract class BaseSidebarActivity : AppCompatActivity() {

    protected lateinit var sessionManager: SessionManager

    abstract fun getContentLayout(): Int
    abstract fun getNavDestination(): NavDestination

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_shell)

        sessionManager = SessionManager(this)
        if (!sessionManager.isLogged()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val shellRoot = findViewById<View>(R.id.shellRoot)
        ViewCompat.setOnApplyWindowInsetsListener(shellRoot) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        LayoutInflater.from(this).inflate(getContentLayout(), findViewById(R.id.content_container), true)
        setupSidebar()
        onContentReady()
    }

    protected open fun onContentReady() {}

    private fun setupSidebar() {
        val navContainer = findViewById<LinearLayout>(R.id.sidebarNavContainer)
        val current = getNavDestination()
        val isAdmin = sessionManager.isAdmin()

        NavDestination.entries.forEach { dest ->
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_sidebar_nav, navContainer, false)
            val icon = itemView.findViewById<ImageView>(R.id.imgNavIcon)

            icon.setImageResource(dest.iconRes)
            icon.contentDescription = dest.contentDescription

            if (dest.adminOnly && !isAdmin) {
                itemView.visibility = View.GONE
            }

            if (dest == current) {
                itemView.setBackgroundResource(R.drawable.bg_sidebar_active)
            }

            itemView.setOnClickListener {
                if (dest == current) return@setOnClickListener
                startActivity(Intent(this, dest.activityClass))
                finish()
            }

            navContainer.addView(itemView)
        }

        findViewById<View>(R.id.btnSidebarSettings).setOnClickListener {
            Toast.makeText(this, "Configurações em breve", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btnSidebarLogout).setOnClickListener {
            sessionManager.limparSessao()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    protected fun setPageHeader(title: String, subtitle: String, actionText: String? = null, onAction: (() -> Unit)? = null) {
        findViewById<android.widget.TextView>(R.id.txtPageTitle)?.text = title
        findViewById<android.widget.TextView>(R.id.txtPageSubtitle)?.text = subtitle
        val btnAction = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPageAction)
        if (actionText != null && onAction != null) {
            btnAction.visibility = View.VISIBLE
            btnAction.text = actionText
            btnAction.setOnClickListener { onAction() }
        } else {
            btnAction?.visibility = View.GONE
        }
    }
}
