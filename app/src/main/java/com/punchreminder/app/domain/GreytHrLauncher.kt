package com.punchreminder.app.domain

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import com.punchreminder.app.R

/**
 * Launching greytHR is intentionally behind an interface so a future authorized API integration
 * can be added without touching the reminder engine.
 *
 * The MVP launches the installed greytHR app when present; otherwise it opens the greytHR listing
 * in Google Play so the user can install it. It never stores passwords and never calls unofficial
 * or scraped endpoints.
 */
interface GreytHrLauncher {
    /**
     * Attempts to open greytHR. Returns true when a launch was delegated to the system.
     * The caller should surface a clear failure message when this returns false.
     */
    fun openGreytHR(context: Context): Boolean
}

/**
 * Default implementation: launches a known greytHR app package when installed, otherwise opens the
 * Play Store listing so the user can install it.
 */
class DefaultGreytHrLauncher : GreytHrLauncher {

    companion object {
        /** The canonical greytHR app package, used for the Play Store listing. */
        const val PRIMARY_PACKAGE = "com.greytip.ghress"

        /** Known greytHR Android package names. The app tries each until one resolves. */
        private val GREYTHR_PACKAGES = listOf(
            PRIMARY_PACKAGE,
            "com.greyythr.employee",
            "com.greyythr",
            "com.greyythr.ess"
        )
    }

    override fun openGreytHR(context: Context): Boolean {
        val pm = context.packageManager

        // 1. Try launching an installed greytHR app.
        for (pkg in GREYTHR_PACKAGES) {
            if (!isPackageInstalled(pm, pkg)) continue

            // Try the standard launch intent first.
            var launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent == null) {
                // Fallback: explicitly target the MainActivity component.
                launchIntent = Intent().apply {
                    component = android.content.ComponentName(pkg, "${pkg}.MainActivity")
                    action = android.content.Intent.ACTION_MAIN
                    addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                }
            }
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                try {
                    context.startActivity(launchIntent)
                    return true
                } catch (e: Exception) {
                    // Try next package.
                }
            }
        }

        // 2. greytHR is not installed: open its Play Store listing so the user can install it.
        if (openPlayStoreListing(context, PRIMARY_PACKAGE)) return true

        Toast.makeText(
            context,
            context.getString(R.string.greythr_open_failed),
            Toast.LENGTH_LONG
        ).show()
        return false
    }

    /**
     * Opens the Play Store details page for [packageName]. Prefers the Play Store app via the
     * market:// scheme and falls back to the web listing when the Play Store app is unavailable.
     */
    private fun openPlayStoreListing(context: Context, packageName: String): Boolean {
        val pm = context.packageManager

        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$packageName")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (marketIntent.resolveActivity(pm) != null) {
            try {
                context.startActivity(marketIntent)
                return true
            } catch (e: Exception) {
                // Fall through to the web listing.
            }
        }

        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (webIntent.resolveActivity(pm) != null) {
            try {
                context.startActivity(webIntent)
                return true
            } catch (e: Exception) {
                // Nothing more we can do.
            }
        }

        return false
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
