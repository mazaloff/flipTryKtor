package coin

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.pipeline.PipelineContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.event.Level
import java.security.SecureRandom

import settings.Settings

fun Application.main() {
    val secureRandom = SecureRandom()
    val booleanRandom = { secureRandom.nextBoolean() }

    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
        }
    }
    install(CallLogging) {
        level = Level.TRACE
    }
    install(Routing) {
        get("") {
            call.respondText("Hello Try Ktor", ContentType.Text.Html)
        }
        get("/flip") {
            handleFlipRequest(booleanRandom)
            //call.respondText("Hello" , ContentType.Text.Html)
        }

        get("/outcomes") {
            handleGetOutcomesRequest()
        }
    }
}

fun main(args: Array<String>) {
    embeddedServer(
        Netty, port = 8888, watchPaths = listOf("MainKt"),
        module = Application::main
    ).start()
    connect()
    transaction { create(RESULTS) }
}


private suspend fun PipelineContext<Unit, ApplicationCall>.handleFlipRequest(booleanRandom: () -> Boolean) {

    // Create a random coin.Face
    val result = booleanRandom.invoke()
    val faceValue: Face = if (result) Face.HEADS else Face.TAILS

    // Insert the result in the database
    transaction {
        RESULTS.insert { it[face] = faceValue.name }
    }.apply {
        // Pass the response back to the client
        call.respond(Coin(faceValue))
    }

}

private suspend fun
        PipelineContext<Unit, ApplicationCall>.handleGetOutcomesRequest() {
    var allOutcomes: List<Coin> = ArrayList()

    // Select all the entries in the DB, mapping the "face" value in each row to a coin
    transaction {
        allOutcomes = RESULTS.selectAll().map { resultRow ->
            Coin(Face.valueOf(resultRow[RESULTS.face]))
        }
    }
        // This is a coroutine so we can call .apply to handle what happens next, in this case replying to the client
        .apply {
            call.respond(allOutcomes) // Gson turns the list into a nice JSON array
        }
}

data class Coin(var face: Face)

enum class Face { HEADS, TAILS }

object RESULTS : Table() {
    val face = varchar("face", 5)
}

fun connect(): Database {
    return Database.connect(
        "jdbc:postgresql://localhost:5432/productREST",
        user = "postgres", password = Settings.passwordPostgresql, driver = "org.postgresql.Driver"
    )
}