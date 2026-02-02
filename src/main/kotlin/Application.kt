package com.github.ousmane_hamadou

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    routing {
        get("/") {
            call.respondText(text = "Hello, world!", contentType = ContentType.Text.Plain)
        }
    }
}
