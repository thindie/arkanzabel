package com.thindie.rknzbl.feature.home.ui.newprofiles

/**
 * Which profiles the NewProfiles list shows.
 *
 * [All] shows every fetched profile. [Available] shows only profiles that have a usable
 * background ping result (measured and not unreachable).
 */
enum class FilterMode {
  All,
  Available,
}

/**
 * A profile is available when it has a measured ping result that is not unreachable. A
 * background measurement that is still in progress (null) or failed (negative) is not available
 * yet.
 */
fun isAvailable(ping: Long?): Boolean = ping != null && ping >= 0
