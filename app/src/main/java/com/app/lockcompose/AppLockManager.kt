package com.app.lockcompose

import android.content.Context
import android.content.SharedPreferences

class AppLockManager(context: Context) {


    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("AppLockPrefs", Context.MODE_PRIVATE)

    fun getSelectedPackages(): Set<String> {
        return sharedPreferences.getStringSet("selected_package_names", emptySet()) ?: emptySet()
    }
    fun addPackage(packageName:  Set<String>) {
        with(sharedPreferences.edit()) {
            putStringSet("selected_package_names", packageName)
            apply()
        }
    }
    fun removePackage(packageName: String) {
        val packageNames = getSelectedPackages().toMutableSet()
        packageNames.remove(packageName)
        with(sharedPreferences.edit()) {
            putStringSet("selected_package_names", packageNames)
            apply()
        }
    }
    fun updateAccessList(packageName: String) {
        val selectedPackageNames = sharedPreferences.getStringSet("selected_package_names", emptySet())?.toMutableSet() ?: mutableSetOf()

        if (!selectedPackageNames.contains(packageName)) {
            val accessList = sharedPreferences.getStringSet("access_list", emptySet())?.toMutableSet() ?: mutableSetOf()
            accessList.remove(packageName)

            with(sharedPreferences.edit()) {
                putStringSet("access_list", accessList)
                apply()
            }
        }
    }
    fun removePackageFromAccessList(packageName: String) {
        val selectedPackageNames = sharedPreferences.getStringSet("selected_package_names", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (selectedPackageNames.contains(packageName)) {
            selectedPackageNames.remove(packageName)
            with(sharedPreferences.edit()) {
                putStringSet("selected_package_names", selectedPackageNames)
                apply()
            }

            // Update access list in SharedPreferences
            val accessList = sharedPreferences.getStringSet("access_list", emptySet())?.toMutableSet() ?: mutableSetOf()
            accessList.remove(packageName)
            with(sharedPreferences.edit()) {
                putStringSet("access_list", accessList)
                apply()
            }
        }
    }
    
}