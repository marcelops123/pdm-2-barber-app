package com.example.pdm2_project

enum class NavDestination(
    val activityClass: Class<*>,
    val iconRes: Int,
    val contentDescription: String,
    val adminOnly: Boolean = false
) {
    DASHBOARD(MainActivity::class.java, R.drawable.ic_nav_dashboard, "Dashboard"),
    APPOINTMENTS(AppointmentsActivity::class.java, R.drawable.ic_nav_calendar, "Agendamentos"),
    CLIENTS(ClientsActivity::class.java, R.drawable.ic_nav_people, "Clientes"),
    USERS(UsersActivity::class.java, R.drawable.ic_nav_user, "Usuários", adminOnly = true),
    FINANCE(FinanceActivity::class.java, R.drawable.ic_nav_money, "Financeiro", adminOnly = true),
    REPORTS(ReportsActivity::class.java, R.drawable.ic_nav_chart, "Relatórios"),
    BACKUP(BackupActivity::class.java, R.drawable.ic_nav_save, "Backup", adminOnly = true);

    companion object {
        val mainNavItems = entries.filter { it != BACKUP || true }
    }
}
