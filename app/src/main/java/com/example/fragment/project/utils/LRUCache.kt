package com.example.fragment.project.utils

class LRUCache<K, V>(private val capacity: Int) {

    private val cache = LinkedHashMap<K, V?>()

    // 将访问的元素移动到队尾
    private fun moveToTail(key: K): V? {
        val value = cache.remove(key)
        cache[key] = value
        return value
    }

    // 移除队首元素
    private fun removeHead(): V? {
        return cache.remove(cache.keys.first())
    }

    fun get(key: K): V? {
        if (!cache.containsKey(key)) {
            return null
        }
        return moveToTail(key)
    }

    fun put(key: K, value: V): V? {
        var head: V? = null
        if (cache.size >= capacity) {
            head = removeHead()
        }
        cache[key] = value
        return head
    }

    fun remove(key: K) {
        cache.remove(key)
    }

    fun clear() {
        cache.clear()
    }

}