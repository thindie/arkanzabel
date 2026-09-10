package com.thindie.rknzbl.appversion

/**
 * Minimal semantic version (major.minor.patch) used to compare the app's local build against the
 * version published on the remote source.
 *
 * Parsing is lenient: non-numeric prefixes are stripped and missing components default to 0, so
 * "2", "2.1" and "2.1.0" all parse sensibly and compare in numeric (not lexical) order.
 */
data class AppVersion(val major: Int, val minor: Int = 0, val patch: Int = 0) : Comparable<AppVersion> {
  override fun compareTo(other: AppVersion): Int = compareBy<AppVersion>({ it.major }, { it.minor }, { it.patch }).compare(this, other)

  companion object {
    /** Parses a version from arbitrary text (e.g. a file line). Returns null when no number is found. */
    fun parse(raw: String?): AppVersion? {
      val numbers =
        raw
          ?.split(Regex("[^0-9]+"))
          ?.mapNotNull { it.toIntOrNull() }
          .orEmpty()
      if (numbers.isEmpty()) return null
      return AppVersion(
        major = numbers[0],
        minor = numbers.getOrNull(1) ?: 0,
        patch = numbers.getOrNull(2) ?: 0,
      )
    }
  }
}
