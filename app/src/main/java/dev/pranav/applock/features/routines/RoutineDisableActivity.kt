package dev.pranav.applock.features.routines

import android.app.Activity
import android.os.Bundle
import dev.pranav.applock.core.utils.appLockRepository
import dev.pranav.applock.services.AppLockManager

/**
 * Headless activity used by Android App Shortcuts / Samsung Modes and Routines.
 * Disables App Lock protection and immediately exits without showing UI.
 */
class RoutineDisableActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applicationContext.appLockRepository().setProtectEnabled(false)

        // Drop any transient lock bookkeeping while protection is disabled.
        AppLockManager.clearTemporarilyUnlockedApp()
        AppLockManager.appUnlockTimes.clear()
        AppLockManager.isLockScreenShown.set(false)

        finish()
        overridePendingTransition(0, 0)
    }
}
