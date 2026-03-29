package com.v2ray.ang.dto

import com.google.gson.annotations.SerializedName
import com.v2ray.ang.AppConfig
import com.v2ray.ang.enums.Protocol

data class V2rayConfig(
    var remarks: String? = null,
    var stats: Any? = null,
    val log: Log,
    var policy: Policy? = null,
    val inbounds: ArrayList<Inbound>,
    var outbounds: ArrayList<Outbound>,
    var dns: Dns? = null,
    val routing: Routing,
    val api: Any? = null,
    val transport: Any? = null,
    val reverse: Any? = null,
    var fakedns: Any? = null,
    val browserForwarder: Any? = null,
    var observatory: Any? = null,
    var burstObservatory: Any? = null
) {

    data class Log(
        val access: String? = null,
        val error: String? = null,
        var loglevel: String? = null,
        val dnsLog: Boolean = false
    )

    data class Inbound(
        var tag: String,
        var port: Int,
        var protocol: String,
        var listen: String? = null,
        var settings: InSettings? = null,
        var sniffing: Sniffing? = null,
        val streamSettings: Any? = null,
        val allocate: Any? = null
    ) {

        data class InSettings(
            var auth: String? = null,
            var udp: Boolean = false,
            var userLevel: Int? = null,
            var name: String? = null,
            @SerializedName("MTU")
            var mtu: Int? = null
        )

        data class Sniffing(
            var enabled: Boolean,
            val destOverride: ArrayList<String>,
            val metadataOnly: Boolean = false,
            var routeOnly: Boolean = false
        )
    }

    data class Outbound(
        var tag: String = "proxy",
        var protocol: String,
        var settings: OutSettings? = null,
        var streamSettings: StreamSettings? = null,
        val proxySettings: Any? = null,
        val sendThrough: String? = null,
        var mux: Mux? = Mux(false)
    ) {
        data class OutSettings(
            var vnext: List<Vnext>? = null,
            var fragment: Fragment? = null,
            var noises: List<Noise>? = null,
            var servers: List<Servers>? = null,
            /*Blackhole*/
            var response: Response? = null,
            /*DNS*/
            val network: String? = null,
            var address: Any? = null,
            var port: Int? = null,
            /*Freedom*/
            var domainStrategy: String? = null,
            val redirect: String? = null,
            val userLevel: Int? = null,
            /*Loopback*/
            val inboundTag: String? = null,
            /*Wireguard*/
            var secretKey: String? = null,
            val peers: List<WireGuard>? = null,
            var reserved: List<Int>? = null,
            var mtu: Int? = null,
            var obfsPassword: String? = null,
            var version: Int? = null,
        ) {

            data class Vnext(
                var address: String = "",
                var port: Int = AppConfig.DEFAULT_PORT,
                var users: List<Users>
            ) {

                data class Users(
                    var id: String = "",
                    var alterId: Int? = null,
                    var security: String? = null,
                    var level: Int = AppConfig.DEFAULT_LEVEL,
                    var encryption: String? = null,
                    var flow: String? = null
                )
            }

            data class Fragment(
                var packets: String? = null,
                var length: String? = null,
                var interval: String? = null
            )

            data class Noise(
                var type: String? = null,
                var packet: String? = null,
                var delay: String? = null
            )

            data class Servers(
                var address: String = "",
                var method: String? = null,
                var ota: Boolean = false,
                var password: String? = null,
                var port: Int = AppConfig.DEFAULT_PORT,
                var level: Int = AppConfig.DEFAULT_LEVEL,
                val email: String? = null,
                var flow: String? = null,
                val ivCheck: Boolean = false,
                var users: List<SocksUsers>? = null
            ) {
                data class SocksUsers(
                    var user: String = "",
                    var pass: String = "",
                    var level: Int = AppConfig.DEFAULT_LEVEL
                )
            }

            data class Response(var type: String)

            data class WireGuard(
                var publicKey: String = "",
                var preSharedKey: String? = null,
                var endpoint: String = ""
            )
        }

        data class StreamSettings(
            var network: String = AppConfig.DEFAULT_NETWORK,
            var security: String? = null,
            var tcpSettings: TcpSettings? = null,
            var kcpSettings: KcpSettings? = null,
            var wsSettings: WsSettings? = null,
            var httpupgradeSettings: HttpupgradeSettings? = null,
            var xhttpSettings: XhttpSettings? = null,
            var httpSettings: HttpSettings? = null,
            var tlsSettings: TlsSettings? = null,
            var quicSettings: QuicSetting? = null,
            var realitySettings: TlsSettings? = null,
            var grpcSettings: GrpcSettings? = null,
            var hysteriaSettings: HysteriaSettings? = null,
            var finalmask: FinalMask? = null,
            val dsSettings: Any? = null,
            var sockopt: Sockopt? = null
        ) {

            data class TcpSettings(
                var header: Header = Header(),
                val acceptProxyProtocol: Boolean = false
            ) {
                data class Header(
                    var type: String = "none",
                    var request: Request? = null,
                    var response: Any? = null
                ) {
                    data class Request(
                        var path: List<String> = emptyList(),
                        var headers: Headers = Headers(),
                        val version: String? = null,
                        val method: String? = null
                    ) {
                        data class Headers(
                            @SerializedName("Host")
                            var host: List<String>? = null,
                            @SerializedName("User-Agent")
                            val userAgent: List<String>? = null,
                            @SerializedName("Accept-Encoding")
                            val acceptEncoding: List<String>? = null,
                            val Connection: List<String>? = null,
                            val Pragma: String? = null
                        )
                    }
                }
            }

            data class KcpSettings(
                var mtu: Int = 1350,
                var tti: Int = 50,
                var uplinkCapacity: Int = 12,
                var downlinkCapacity: Int = 100,
                var congestion: Boolean = false,
                var readBufferSize: Int = 1,
                var writeBufferSize: Int = 1
            )

            data class WsSettings(
                var path: String? = null,
                var headers: Headers = Headers(),
                val maxEarlyData: Int? = null,
                val useBrowserForwarding: Boolean = false,
                val acceptProxyProtocol: Boolean = false
            ) {
                data class Headers(
                    @SerializedName("Host")
                    var host: String = ""
                )
            }

            data class HttpupgradeSettings(
                var path: String? = null,
                var host: String? = null,
                val acceptProxyProtocol: Boolean = false
            )

            data class XhttpSettings(
                var path: String? = null,
                var host: String? = null,
                var mode: String? = null,
                var extra: Any? = null,
            )

            data class HttpSettings(
                var host: List<String> = emptyList(),
                var path: String? = null
            )

            data class Sockopt(
                @SerializedName("TcpNoDelay")
                var tcpNoDelay: Boolean = false,
                var tcpKeepAliveIdle: Int? = null,
                var tcpFastOpen: Boolean = false,
                var tproxy: String? = null,
                var mark: Int? = null,
                var dialerProxy: String? = null,
                var domainStrategy: String? = null,
                var happyEyeballs: HappyEyeballs? = null,
            )

            data class HappyEyeballs(
                var prioritizeIPv6: Boolean = false,
                var maxConcurrentTry: Int? = 4,
                var tryDelayMs: Int? = 250, // ms
                var interleave: Int? = null,
            )

            data class TlsSettings(
                var allowInsecure: Boolean = false,
                var serverName: String? = null,
                val alpn: List<String>? = null,
                val minVersion: String? = null,
                val maxVersion: String? = null,
                val preferServerCipherSuites: Boolean = false,
                val cipherSuites: String? = null,
                val fingerprint: String? = null,
                val certificates: List<Any>? = null,
                val disableSystemRoot: Boolean = false,
                val enableSessionResumption: Boolean = false,
                var echConfigList: String? = null,
                var echForceQuery: String? = null,
                var pinnedPeerCertSha256: String? = null,
                // REALITY settings
                val show: Boolean = false,
                var publicKey: String? = null,
                /** Xray REALITY client: same as [publicKey]; unmarshals to `password` in Go (alias for pbk). */
                @SerializedName("password")
                var realityPublicKeyPassword: String? = null,
                var shortId: String? = null,
                var spiderX: String? = null,
                var mldsa65Verify: String? = null
            )

            data class QuicSetting(
                var security: String = "none",
                var key: String = "",
                var header: Header = Header()
            ) {
                data class Header(var type: String = "none")
            }

            data class GrpcSettings(
                var serviceName: String = "",
                var authority: String? = null,
                var multiMode: Boolean = false,
                var idle_timeout: Int? = null,
                var health_check_timeout: Int? = null
            )

            data class HysteriaSettings(
                var version: Int,
                var auth: String? = null,
                var up: String? = null,
                var down: String? = null,
                var udphop: HysteriaUdpHop? = null
            ) {
                data class HysteriaUdpHop(
                    var port: String? = null,
                    var interval: Int? = null
                )
            }

            data class FinalMask(
                var tcp: List<Mask>? = null,
                var udp: List<Mask>? = null
            ) {
                data class Mask(
                    var type: String,
                    var settings: MaskSettings? = null
                ) {
                    data class MaskSettings(
                        var password: String? = null,
                        var domain: String? = null
                    )
                }
            }
        }

        data class Mux(
            var enabled: Boolean,
            var concurrency: Int? = null,
            var xudpConcurrency: Int? = null,
            var xudpProxyUDP443: String? = null,
        )

        fun getServerAddress(): String? {
            if (protocol.equals(Protocol.Vmess.name, true)
                || protocol.equals(Protocol.Vless.name, true)
            ) {
                return settings?.vnext?.first()?.address
            } else if (protocol.equals(Protocol.ShadowSocks.name, true)
                || protocol.equals(Protocol.Socks.name, true)
                || protocol.equals(Protocol.Http.name, true)
                || protocol.equals(Protocol.Trojan.name, true)
            ) {
                return settings?.servers?.first()?.address
            } else if (protocol.equals(Protocol.WireGuard.name, true)) {
                return settings?.peers?.first()?.endpoint?.substringBeforeLast(":")
            } else if (protocol.equals(Protocol.Hysteria2.name, true)
                || protocol.equals(Protocol.Hysteria.name, true)
            ) {
                return settings?.address as String?
            }
            return null
        }

        fun getServerPort(): Int? {
            if (protocol.equals(Protocol.Vmess.name, true)
                || protocol.equals(Protocol.Vless.name, true)
            ) {
                return settings?.vnext?.first()?.port
            } else if (protocol.equals(Protocol.ShadowSocks.name, true)
                || protocol.equals(Protocol.Socks.name, true)
                || protocol.equals(Protocol.Http.name, true)
                || protocol.equals(Protocol.Trojan.name, true)
            ) {
                return settings?.servers?.first()?.port
            } else if (protocol.equals(Protocol.WireGuard.name, true)) {
                return settings?.peers?.first()?.endpoint?.substringAfterLast(":")?.toInt()
            } else if (protocol.equals(Protocol.Hysteria2.name, true)
                || protocol.equals(Protocol.Hysteria.name, true)
            ) {
                return settings?.port
            }
            return null
        }

        fun ensureSockopt(): StreamSettings.Sockopt {
            val stream = streamSettings ?: StreamSettings().also {
                streamSettings = it
            }

            val sockopt = stream.sockopt ?: StreamSettings.Sockopt().also {
                stream.sockopt = it
            }

            return sockopt
        }
    }

    /**
     * Xray `dns` JSON: [servers] is a heterogeneous array (plain address [String] or [Servers] object).
     * [hosts] map values are likewise untyped in JSON. Gson maps those nodes to [Any]; config builder mutates
     * [servers] and [hosts] in place — hence [var] only on those two.
     */
    data class Dns(
        var servers: ArrayList<Any>? = null,
        var hosts: Map<String, Any>? = null,
        val clientIp: String? = null,
        val disableCache: Boolean = false,
        val queryStrategy: String? = null,
        val tag: String? = null,
    ) {
        data class Servers(
            var address: String = "",
            var port: Int? = null,
            var domains: List<String>? = null,
            var expectIPs: List<String>? = null,
            val clientIp: String? = null,
            val skipFallback: Boolean = false,
            val tag: String? = null,
        )
    }

    data class Routing(
        var domainStrategy: String,
        var domainMatcher: String? = null,
        var rules: ArrayList<Rules>,
        var balancers: List<Balancer>? = null
    ) {

        data class Rules(
            var type: String = "field",
            var ip: ArrayList<String>? = null,
            var domain: ArrayList<String>? = null,
            var outboundTag: String? = null,
            var balancerTag: String? = null,
            var port: String? = null,
            val sourcePort: String? = null,
            val network: String? = null,
            val source: List<String>? = null,
            val user: List<String>? = null,
            var inboundTag: List<String>? = null,
            val protocol: List<String>? = null,
            val attrs: String? = null,
            val domainMatcher: String? = null
        )

        data class Balancer(
            val tag: String,
            val selector: List<String>,
            val fallbackTag: String? = null,
            val strategy: StrategyObject? = null
        )

        data class StrategyObject(
            val type: String = "random", // "random" | "roundRobin" | "leastPing" | "leastLoad"
            val settings: StrategySettingsObject? = null
        )

        data class StrategySettingsObject(
            val expected: Int? = null,
            val maxRTT: String? = null,
            val tolerance: Double? = null,
            val baselines: List<String>? = null,
            val costs: List<CostObject>? = null
        )

        data class CostObject(
            val regexp: Boolean = false,
            val match: String,
            val value: Double
        )
    }

    data class Policy(
        var levels: Map<String, Level>,
        var system: Any? = null
    ) {
        data class Level(
            var handshake: Int? = null,
            var connIdle: Int? = null,
            var uplinkOnly: Int? = null,
            var downlinkOnly: Int? = null,
            val statsUserUplink: Boolean = false,
            val statsUserDownlink: Boolean = false,
            var bufferSize: Int? = null
        )
    }

    data class ObservatoryObject(
        val subjectSelector: List<String>,
        val probeUrl: String,
        val probeInterval: String,
        val enableConcurrency: Boolean = false
    )

    data class BurstObservatoryObject(
        val subjectSelector: List<String>,
        val pingConfig: PingConfigObject
    ) {
        data class PingConfigObject(
            val destination: String,
            val connectivity: String? = null,
            val interval: String,
            val sampling: Int,
            val timeout: String? = null
        )
    }

    data class Fakedns(
        var ipPool: String = "198.18.0.0/15",
        var poolSize: Int = 10000
    ) // roughly 10 times smaller than total ip pool

    fun getProxyOutbound(): Outbound? {
        outbounds.forEach { outbound ->
            Protocol.entries.forEach {
                if (outbound.protocol.equals(it.name, true)) {
                    return outbound
                }
            }
        }
        return null
    }

    fun getAllProxyOutbound(): List<Outbound> {
        return outbounds.filter { outbound ->
            Protocol.entries.any { it.name.equals(outbound.protocol, ignoreCase = true) }
        }
    }
}
