package io.oversec.one.common

import android.os.SystemClock
import android.support.v4.util.LruCache

import java.util.HashMap

/**
 * @param maxSize for caches that do not override [.sizeOf], this is
 * the maximum number of entries in the cache. For all other caches,
 * this is the maximum sum of the sizes of the entries in this cache.
 * @param expireTime the amount of time in milliseconds that any particular
 * cache entry is valid.
 */
class ExpiringLruCache<K, V>
    (maxSize: Int, private val mExpireTime: Long) {
    private val mCache: LruCache<K, V>
    private val mExpirationTimes: MutableMap<K, Long>

    init {
        mExpirationTimes = HashMap(maxSize)
        mCache = MyLruCache(maxSize)
    }

    @Synchronized
    operator fun get(key: K): V? {
        val value = mCache.get(key)
        if (value != null && elapsedRealtime() >= getExpiryTime(key)) {
            remove(key)
            return null
        }
        return value
    }

    @Synchronized
    fun put(key: K, value: V): V? {
        val oldValue = mCache.put(key, value)
        mExpirationTimes[key] = elapsedRealtime() + mExpireTime
        return oldValue
    }

    internal fun elapsedRealtime(): Long { // With Bill Maher
        return SystemClock.elapsedRealtime()
    }

    internal fun getExpiryTime(key: K): Long {
        return mExpirationTimes[key] ?: return 0
    }

    internal fun removeExpiryTime(key: K) {
        mExpirationTimes.remove(key)
    }

    fun remove(key: K): V? {
        return mCache.remove(key)
    }

    fun snapshot(): Map<K, V> {
        return mCache.snapshot()
    }

    fun trimToSize(maxSize: Int) {
        mCache.trimToSize(maxSize)
    }

    fun createCount(): Int {
        return mCache.createCount()
    }

    fun evictAll() {
        mCache.evictAll()
    }

    fun evictionCount(): Int {
        return mCache.evictionCount()
    }

    fun hitCount(): Int {
        return mCache.hitCount()
    }

    fun maxSize(): Int {
        return mCache.maxSize()
    }

    fun missCount(): Int {
        return mCache.missCount()
    }

    fun putCount(): Int {
        return mCache.putCount()
    }

    fun size(): Int {
        return mCache.size()
    }

    /**
     * Extended the LruCache to override the [.entryRemoved] method
     * so we can remove expiration time entries when things are evicted from the cache.
     *
     * Gotta love some of those Google engineers making things difficult with paranoid
     * usage of the `final` keyword.
     */
    private inner class MyLruCache(maxSize: Int) : LruCache<K, V>(maxSize) {

        override fun entryRemoved(evicted: Boolean, key: K, oldValue: V, newValue: V?) {
            this@ExpiringLruCache.removeExpiryTime(key) // dirty hack
        }

        override fun sizeOf(key: K, value: V): Int {
            return this@ExpiringLruCache.sizeOf(key, value)
        }
    }

    /**
     * Returns the size of the entry for `key` and `value` in
     * user-defined units.  The default implementation returns 1 so that size
     * is the number of entries and max size is the maximum number of entries.
     *
     *
     * An entry's size must not change while it is in the cache.
     */
    protected fun sizeOf(key: K, value: V): Int {
        return 1
    }
}