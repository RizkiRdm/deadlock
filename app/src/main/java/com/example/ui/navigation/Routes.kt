package com.example.ui.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val APP_SELECTOR = "app_selector"
    const val LOCK_DETAIL = "lock_detail/{policyId}"
    const val DEVICE_SETTINGS = "device_settings"

    fun lockDetail(policyId: String) = "lock_detail/$policyId"
}
