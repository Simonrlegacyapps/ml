package com.example.demomlkit.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.demomlkit.DemoMLKit
import java.util.HashSet

object PrefManager {
    private var sharedPreferences: SharedPreferences? = null

    fun putString(key: String?, `val`: String?) {
        init()
        val editor = sharedPreferences!!.edit()
        editor.putString(key, `val`)
        editor.apply()
    }

    fun putSet(key: String?, `val`: HashSet<String?>?) {
        init()
        val editor = sharedPreferences!!.edit()
        editor.putStringSet(key, `val`)
        editor.apply()
    }

    fun getString(key: String?): String? {
        init()
        return sharedPreferences!!.getString(key, "")
    }

    fun isKeyExist(key: String?): Boolean {
        init()
        return sharedPreferences!!.contains(key)
    }

    fun getSet(key: String?): Set<String>? {
        init()
        return sharedPreferences!!.getStringSet(key, HashSet())
    }

    fun putBoolean(key: String?, `val`: Boolean) {
        init()
        val editor = sharedPreferences!!.edit()
        editor.putBoolean(key, `val`)
        editor.apply()
    }

    fun getBoolean(key: String?): Boolean {
        init()
        return sharedPreferences!!.getBoolean(key, false)
    }

    fun isFirstTimeOpen(key: String?): Boolean {
        init()
        return sharedPreferences!!.getBoolean(key, true)
    }

    private fun init() {
        sharedPreferences =
            DemoMLKit.demoMLKit.getSharedPreferences("Demo_ML_Kit", Context.MODE_PRIVATE)
    }

    fun clear() {
        init()
        val editor = sharedPreferences!!.edit()
        editor.clear()
        editor.apply()
    }

    fun remove(value: String?) {
        init()
        val editor = sharedPreferences!!.edit()
        editor.remove(value)
        editor.apply()
    }
}