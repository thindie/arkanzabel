package com.v2ray.ang.ipc

interface BroadcastersHolder {
  val daemonBroadcastReceiver: IpcDaemonBroadcastReceiver
  val mainBroadcastReceiver: IpcMainBroadcastReceiver
}
