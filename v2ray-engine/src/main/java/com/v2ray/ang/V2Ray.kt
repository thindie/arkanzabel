package com.v2ray.ang

import com.thindie.engine.core.WorkState
import com.v2ray.ang.dto.ConnectionProfile
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

interface CoreEventSink {
  fun send(event: CoreEvent)

  val message: Flow<CoreEvent>
}

interface CoreState {
  val vpnState: WorkState
}

sealed interface CoreEvent {
  sealed interface Vpn : CoreEvent {
    sealed interface Request : Vpn {
      data object Start : Request

      data object Stop : Request

      data object Restart : Request

      data object Measure : Request
    }

    sealed interface Response : Vpn {
      data class Started(val profile: ConnectionProfile) : Response

      data object Stopped : Response

      data class Error(val e: Throwable) : Response
    }
  }
}

class V2Ray : CoreEventSink, CoreState {
  private val stateInternal = MutableStateFlow(State())
  private val eventInternal =
    MutableSharedFlow<CoreEvent>(
      replay = 0,
      extraBufferCapacity = 3,
      onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
  override val message: Flow<CoreEvent> = eventInternal

  override fun send(event: CoreEvent) {
    when (event) {
      CoreEvent.Vpn.Request.Measure -> TODO()
      CoreEvent.Vpn.Request.Restart -> TODO()
      CoreEvent.Vpn.Request.Start -> TODO()
      CoreEvent.Vpn.Request.Stop -> TODO()
      is CoreEvent.Vpn.Response -> {
        asVpnState(event)
      }
    }
    eventInternal.tryEmit(event)
  }

  override val vpnState: WorkState
    get() = stateInternal.value.vpnState

  private fun asVpnState(response: CoreEvent.Vpn.Response) {
    when (response) {
      is CoreEvent.Vpn.Response.Error -> {
        stateInternal.update {
          it.copy(
            vpnState =
              WorkState.Error(
                message = response.e.message.orEmpty(),
                response.e,
              ),
            profile = null,
          )
        }
      }
      is CoreEvent.Vpn.Response.Started ->
        stateInternal.update {
          it.copy(
            vpnState = WorkState.Running,
            profile = response.profile,
          )
        }
      CoreEvent.Vpn.Response.Stopped ->
        stateInternal.update {
          it.copy(
            vpnState = WorkState.Idle,
            profile = null,
          )
        }
    }
  }
}

private data class State(
  val vpnState: WorkState = WorkState.Idle,
  val profile: ConnectionProfile? = null,
)
