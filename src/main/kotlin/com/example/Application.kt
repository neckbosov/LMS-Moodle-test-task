package com.example

import com.example.plugins.configureMonitoring
import com.example.plugins.configureRouting
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default

fun main(args: Array<String>) {
    val parser = ArgParser("ktor-random-gen")
    val host by parser.option(ArgType.String).default("0.0.0.0")
    val port by parser.option(ArgType.Int).default(8080)
    parser.parse(args)
    embeddedServer(Netty, port = port, host = host) {
        install(ContentNegotiation) {
            json()
        }
        configureMonitoring()
        configureRouting()
    }.start(wait = true)
}
