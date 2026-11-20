# Android Capabilities & Limitations

This document outlines the technical capabilities and limitations of Android's device management APIs as they apply to Deadlock's requirements.

## 1. Can a Device Owner block a specific package from being installed?
**PARTIALLY SUPPORTED**
Android's `DevicePolicyManager` (DPM) does not provide a direct API to block the installation of a *specific* package ID (allowlist/blocklist for installs) for a local device owner without relying on Managed Google Play (which requires a backend). 
However, Deadlock can achieve the functional equivalent by:
1. Blocking uninstall of the app using `DPM.setUninstallBlocked()`. If an app is locked, the user simply cannot uninstall it, rendering the "reinstall" flow moot.
2. If the app *is* uninstalled before the lock, Deadlock can listen for `ACTION_PACKAGE_ADDED` and immediately apply `DPM.setApplicationHidden()` or `DPM.setPackagesSuspended()` to disable the app before it can be launched.

## 2. Does that restriction cover Google Play?
**SUPPORTED**
When a package is suspended or hidden via DPM, it is blocked regardless of whether it was installed via Google Play. Furthermore, if `setUninstallBlocked` is active, Google Play cannot uninstall the app.

## 3. Does it cover APK installation?
**SUPPORTED**
Sideloading an APK triggers the same package manager broadcasts and is subject to the same DPM restrictions (suspension/hiding).

## 4. Does it cover third-party app stores?
**SUPPORTED**
Third-party app stores use standard Android package management APIs, which are fully regulated by DPM.

## 5. Can an already-installed target application be disabled?
**SUPPORTED**
A Device Owner can use `DevicePolicyManager.setApplicationHidden(admin, packageName, true)` to completely hide the app from the launcher and prevent it from running. Alternatively, `setPackagesSuspended` can be used to render the app unusable.

## 6. Can Deadlock prevent its own removal?
**SUPPORTED**
As a registered Device Owner, Deadlock cannot be uninstalled by the user through normal system settings or launchers. Only the Device Owner application itself can relinquish its status, or the user must perform a factory reset.

## 7. What provisioning requirements exist?
**NOT POSSIBLE (for standard consumer adoption without setup)**
A Device Owner *cannot* be silently or easily enabled on an already-configured consumer device. 
Provisioning requires either:
1. **ADB Command:** The user must connect their device to a computer and run `adb shell dpm set-device-owner com.example/.security.DeadlockAdminReceiver`. (Note: This often fails if there are existing accounts on the device).
2. **Factory Reset (Provisioning):** The device must be factory reset, and the user must provision Deadlock via NFC, QR code, or ADB during the initial setup wizard.

## 8. What device reset path exists?
**SUPPORTED**
A hardware factory reset (via recovery mode) will always wipe the device and remove Deadlock, serving as the ultimate emergency escape hatch.

---

## Conclusion on Enforcement Strategy
To meet the PRD's requirement of "Uninstall target app -> Attempt reinstall later -> Blocked", the actual implementation will rely on `ACTION_PACKAGE_ADDED` interception. When a locked package is detected as installed, Deadlock will instantly suspend/hide it. To provide stronger protection, Deadlock will also use `setUninstallBlocked` to prevent the user from uninstalling the target app in the first place.
