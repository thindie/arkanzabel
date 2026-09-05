package com.thindie.rknzbl.appfeatures.home

import com.thindie.engine.core.Section

/**
 * Sections that share the bottom navigation bar.
 *
 * Routes tagged with one of these get a [com.thindie.rknzbl.appfeatures.home.BottomNavigationBar]
 * rendered below their content. Legacy routes use [Section.Leaf] and are unaffected.
 */
sealed interface HomeSection : Section {
  data object Home : HomeSection

  data object Profiles : HomeSection

  data object Settings : HomeSection

  data object Logs : HomeSection
}
