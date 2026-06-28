package com.thindie.rknzbl.feature.managegate.gatelist

import android.content.Context
import com.thindie.rknzbl.engine.Route
import com.thindie.rknzbl.engine.Router
import com.thindie.rknzbl.engine.ScreenFlow

class SelectSourceFlow(
  private val router: Router,
  val appContext: Context,
) : ScreenFlow<Route, SelectSourceFlow.Result>(router) {
  override fun start() {
    go(main())
  }

  sealed interface Result {
    val sourceUrl: String?

    data object FullBlackShadowSocks : Result {
      override val sourceUrl: String =
        "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/BLACK_SS%2BAll_RUS.txt"
    }

    data object FullBlackVless : Result {
      override val sourceUrl: String =
        "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/BLACK_VLESS_RUS.txt"
    }

    data object MobileBlackVless : Result {
      override val sourceUrl: String =
        "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/BLACK_VLESS_RUS_mobile.txt"
    }

    data object WhiteListMobile : Result {
      override val sourceUrl: String =
        "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/Vless-Reality-White-Lists-Rus-Mobile.txt"
    }

    data object WhiteListMobileV2 : Result {
      override val sourceUrl: String =
        "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/Vless-Reality-White-Lists-Rus-Mobile-2.txt"
    }

    data object WhiteListAll : Result {
      override val sourceUrl: String =
        "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/WHITE-CIDR-RU-all.txt"
    }

    data object WhiteListRussian : Result {
      override val sourceUrl: String =
        "https://github.com/igareck/vpn-configs-for-russia/blob/main/WHITE-CIDR-RU-checked.txt"
    }

    data object NotSelected : Result {
      override val sourceUrl: String? = null
    }

    data object StoredProfiles : Result {
      override val sourceUrl: String? get() = null
    }

    data class CustomSource(val url: String) : Result {
      override val sourceUrl: String? = url
    }
  }
}
