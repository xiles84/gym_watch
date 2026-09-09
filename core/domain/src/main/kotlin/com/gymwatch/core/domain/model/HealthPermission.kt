package com.gymwatch.core.domain.model

/**
 * Runtime permissions the app needs, named in domain terms.
 *
 * Deliberately not Android's permission strings: those differ by API level
 * (BODY_SENSORS below 36, android.permission.health.READ_HEART_RATE at 36+),
 * and that split is the adapter's problem, not the core's.
 */
enum class HealthPermission { HEART_RATE, ACTIVITY_RECOGNITION }
