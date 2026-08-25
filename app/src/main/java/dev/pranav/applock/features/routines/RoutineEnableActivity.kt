package dev.pranav.applock.features.routines

import android.app.Activity
import android.os.Bundle
import dev.pranav.applock.core.utils.appLockRepository
import dev.pranav.applock.services.AppLockManager

/**
 * Headless activity used by Android App Shortcuts / Samsung Modes and Routines.
 * Enables App Lock protection and immediately exits without showing UI.
 */
class RoutineEnableActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applicationContext.appLockRepository().setProtectEnabled(true)

        // Force the next protected app launch to require authentication again.
        AppLockManager.clearTemporarilyUnlockedApp()
        AppLockManager.appUnlockTimes.clear()
        AppLockManager.isLockScreenShown.set(false)

        finish()
        overridePendingTransition(0, 0)
    }
}
