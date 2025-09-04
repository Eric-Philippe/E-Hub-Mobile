package com.ericp.e_hub.cache

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class ApiCache(context: Context) {

    companion object {
        private const val CACHE_DIR = "api_cache"
        private const val CACHE_INDEX_FILE = "cache_index.json"
        private const val CACHE_TTL_HOURS = 24L // 24 hours
    }

    data class CacheEntry(
        val timestamp: Long,
        val data: String,
        val ttl: Long = CACHE_TTL_HOURS * 60 * 60 * 1000 // TTL in milliseconds
    )

    private val gson = Gson()
    private val cacheDir = File(context.cacheDir, CACHE_DIR)
    private val indexFile = File(cacheDir, CACHE_INDEX_FILE)

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    private fun getCacheIndex(): MutableMap<String, CacheEntry> {
        return try {
            if (indexFile.exists()) {
                val type = object : TypeToken<MutableMap<String, CacheEntry>>() {}.type
                gson.fromJson(FileReader(indexFile), type) ?: mutableMapOf()
            } else {
                mutableMapOf()
            }
        } catch (_: Exception) {
            mutableMapOf()
        }
    }

    private fun saveCacheIndex(index: MutableMap<String, CacheEntry>) {
        try {
            FileWriter(indexFile).use {
                gson.toJson(index, it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun put(key: String, data: String, customTtlHours: Long? = null) {
        val index = getCacheIndex()
        val ttl = (customTtlHours ?: CACHE_TTL_HOURS) * 60L * 60L * 1000L

        val entry = CacheEntry(
            timestamp = System.currentTimeMillis(),
            data = data,
            ttl = ttl
        )

        index[key] = entry
        saveCacheIndex(index)
    }

    fun get(key: String): String? {
        val index = getCacheIndex()
        val entry = index[key] ?: return null

        // Check if the cache entry is still valid
        if (System.currentTimeMillis() - entry.timestamp > entry.ttl) {
            // Entry expired
            index.remove(key)
            saveCacheIndex(index)
            return null
        }

        return entry.data
    }

    fun contains(key: String): Boolean {
        return get(key) != null
    }

    fun remove(key: String) {
        val index = getCacheIndex()
        index.remove(key)
        saveCacheIndex(index)
    }

    fun clear() {
        try {
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
