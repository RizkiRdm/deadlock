# Device Management Architecture

This document outlines the proposed architecture for Deadlock's enforcement and security mechanisms, specifically focusing on Device Owner (DO) capabilities.

## 1. Provisioning Flow
Because Deadlock requires Device Owner privileges to enforce immutable long-term locks and prevent its own removal, it cannot be installed and activated like a normal app. 

**Required Flow:**
1. User installs Deadlock APK.
2. User must use a computer to run an ADB command:
   `adb shell dpm set-device-owner com.example/.device.DeadlockAdminReceiver`
   *(Alternatively, the device must be provisioned from a factory-reset state).*
3. Once DO is activated, Deadlock can exercise `DevicePolicyManager` APIs.

## 2. Enforcement Mechanisms
To lock an application:
* **Hiding/Suspending:** Deadlock will use `DevicePolicyManager.setApplicationHidden(admin, packageName, true)`. This makes the app disappear from the launcher and prevents its execution.
* **Uninstall Blocking:** Deadlock will call `DevicePolicyManager.setUninstallBlocked(admin, packageName, true)` to ensure the user cannot bypass the lock by uninstalling and trying to find a loophole.
* **Reinstall Prevention:** A `BroadcastReceiver` listening for `Intent.ACTION_PACKAGE_ADDED` will detect if a locked package ID is installed. Upon detection, Deadlock will immediately re-apply `setApplicationHidden`.

## 3. Policy Persistence & Security
* **Storage:** Policies will be stored in a Room Database (`AppDatabase`).
* **Integrity:** Each `LockedAppPolicy` will have an HMAC signature generated using a key stored in the **Android Keystore**. The Keystore ensures that the cryptographic material cannot be extracted. If a user manages to root the device and manually edits the SQLite database to change the `expiresAt` date, the HMAC verification will fail, and Deadlock will treat the policy as tampered (and fallback to a safe state or refuse to unlock).
* **Self-Protection:** Being a Device Owner inherently prevents Deadlock from being uninstalled.

## 4. Clock / Time Security
**Problem:** A user could manually change the system clock forward to instantly expire a 1-year lock.
**Solution:**
1. Deadlock will rely on `SystemClock.elapsedRealtime()` for short-term offset tracking.
2. Deadlock will enforce a global `UserManager.DISALLOW_CONFIG_DATE_TIME` restriction while a lock is active, preventing the user from manually changing the wall-clock time in Settings.
3. Deadlock will periodically sync with a trusted NTP server (if network is available) to verify the true passage of time, storing the cryptographically signed `lastKnownTrueTime`.

## 5. Recovery Mechanism
* **No Software Escape Hatch:** By design, there is no UI button, hidden menu, or password to bypass an active lock.
* **Catastrophic Recovery:** If the user absolutely must escape the lock (e.g., severe emergency or application bug), they must perform a **Hardware Factory Reset** via the Android Recovery Menu. This wipes all data on the device, providing a massive friction barrier that fulfills the app's core self-control philosophy while preventing permanent device bricking.
