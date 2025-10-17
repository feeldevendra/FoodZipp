package com.foodzipp.app.auth

interface AnalyticsTracker {
    fun track(event: String, attributes: Map<String, String> = emptyMap())
}

class InMemoryAnalyticsTracker : AnalyticsTracker {
    private val _events = mutableListOf<Pair<String, Map<String, String>>>()
    val events: List<Pair<String, Map<String, String>>> get() = _events

    override fun track(event: String, attributes: Map<String, String>) {
        _events += event to attributes
    }
}
