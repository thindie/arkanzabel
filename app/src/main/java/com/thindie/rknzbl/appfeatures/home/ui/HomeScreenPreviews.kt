package com.thindie.rknzbl.appfeatures.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.thindie.engine.uikit.AppTheme

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun HomeScreenDisconnectedPreview() {
  AppTheme {
    ConnectButton(isConnected = false)
  }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun HomeScreenConnectedPreview() {
  AppTheme {
    ConnectButton(isConnected = true)
  }
}
