package test.controllers

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import ua.ukma.edu.danki.models.CardCollectionDTO
import ua.ukma.edu.danki.models.User
import ua.ukma.edu.danki.models.UserAuthRequest
import ua.ukma.edu.danki.models.UserAuthResponse
import ua.ukma.edu.danki.services.CardCollectionService
import ua.ukma.edu.danki.services.UserService
import ua.ukma.edu.danki.utils.JwtConfig
import java.util.*
import kotlin.test.assertEquals

class CardCollectionsControllerTests {

    @Test
    fun testGetCollectionsWithMocks() = testApplication {
        environment {
            config = ApplicationConfig("application-mock.yaml")
        }
        // Create a mock service for CardCollectionService and UserService
        val cardCollectionService = mockk<CardCollectionService>()
        val userService = mockk<UserService>()

        // Mock responses for the service calls
        val userId = UUID.randomUUID()
        val user = mockk<User>()
        every { user.email } returns "email@email.com"
        every { user.password } returns "pass"
        coEvery { userService.findUser(userId) } returns user
        coEvery { userService.findUser("email@email.com") } returns user
        // This should correspond to application-mock.yaml values
        JwtConfig.init("secret", "dankiTest", 3600000L, userService)
        var jwt = JwtConfig.makeToken(UserAuthRequest(user.email, user.password))
        coEvery { userService.authenticateUser(UserAuthRequest("email@email.com", "pass")) } returns jwt

        val collections = listOf(
            CardCollectionDTO(1, "Collection1", Clock.System.now()),
            CardCollectionDTO(2, "Collection2", Clock.System.now())
        )
        coEvery {
            cardCollectionService.getCollections(any(), any(), any(), any(), any())
        } returns collections

        application {
            mockModule(cardCollectionService, userService)
            jwt = JwtConfig.makeToken(UserAuthRequest(user.email, user.password))
        }

        val loginToken = Json.decodeFromString<UserAuthResponse>(client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(UserAuthRequest(user.email, user.password)))
        }.body<String>()).jwt

        val response = client.get("/collections/") {
            url {
                parameters.append("userId", userId.toString())
            }
            headers {
                append("Authorization", "Bearer $loginToken")
                append("Accept", "application/json")
            }
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
