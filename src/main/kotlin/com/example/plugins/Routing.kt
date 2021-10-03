package com.example.plugins

import com.example.dto.RandomLong
import com.example.dto.RandomString
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.cio.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.time.LocalDateTime
import kotlin.random.Random
import kotlin.random.nextLong

private val fileWriteMutex = Mutex()

@Suppress("BlockingMethodInNonBlockingContext")
fun Application.configureRouting(seed: Int) {
    val random = Random(seed)
    val symbols = ('A'..'Z') + ('a'..'z') + ('0'..'9') + ".,;:><?!@#%$^*()-_+=/ ".toList()
    // Starting point for a Ktor app:
    routing {

        get("/string") {
            val lenStr = call.request.queryParameters["length"]
            if (lenStr != null) {
                val len = lenStr.toIntOrNull()
                if (len != null) {
                    val randomString = (1..len)
                        .map { random.nextInt(0, symbols.size) }
                        .map { symbols[it] }
                        .joinToString("")
                    val now = LocalDateTime.now()
                    val file = File("results.txt")
                    call.respond(RandomString(randomString))
                    // Ktor still does not have I/O subsystem for file operations, because of that we use blocking I/O
                    fileWriteMutex.withLock {
                        file.appendText("$now $randomString\n")
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest, "String length should be an integer")
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, "Missing string length")
            }
        }
        get("/integer") {
            val fromLimit = call.request.queryParameters["from"]?.let {
                val num = it.toLongOrNull()
                if (num != null) {
                    num
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Lower limit should be integer")
                    return@get
                }
            } ?: Long.MIN_VALUE
            val toLimit = call.request.queryParameters["to"]?.let {
                val num = it.toLongOrNull()
                if (num != null) {
                    num
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Upper limit should be integer")
                    return@get
                }
            } ?: Long.MAX_VALUE
            if (fromLimit > toLimit) {
                call.respond(HttpStatusCode.BadRequest, "Upper limit is lower than lower limit")
                return@get
            }
            val randomNumber = random.nextLong(fromLimit..toLimit)
            call.respond(RandomLong(randomNumber))
            val now = LocalDateTime.now()
            val file = File("results.txt")
            file.writeChannel()
            fileWriteMutex.withLock {
                file.appendText("$now $randomNumber\n")
            }
        }
        get("/download_cache") {
            val file = File("results.txt")
            if (!file.exists()) {
                fileWriteMutex.withLock {
                    file.createNewFile()
                }
            }
            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "results.txt")
                    .toString()
            )
            call.respondFile(file)
        }
    }

}
