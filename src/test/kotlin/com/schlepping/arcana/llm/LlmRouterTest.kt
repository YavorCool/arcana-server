package com.schlepping.arcana.llm

import kotlin.test.Test
import kotlin.test.assertEquals

class LlmRouterTest {

    private val config = LlmRoutingConfig(
        premiumReading = "gpt-5",
        freeReading = "gpt-5-mini",
        premiumChat = "gpt-5-mini",
        freeChat = "gpt-5-nano",
        dailyCard = "gpt-5-mini",
        firstReading = "gpt-5",
    )

    private val router = LlmRouter(config)

    @Test
    fun `free tier reading routes to gpt5-mini`() {
        val model = router.resolve(UserTier.FREE, RequestType.READING, isFirstReading = false)
        assertEquals("gpt-5-mini", model)
    }

    @Test
    fun `premium tier reading routes to gpt5`() {
        val model = router.resolve(UserTier.PREMIUM, RequestType.READING, isFirstReading = false)
        assertEquals("gpt-5", model)
    }

    @Test
    fun `free tier chat routes to gpt5-nano`() {
        val model = router.resolve(UserTier.FREE, RequestType.CHAT, isFirstReading = false)
        assertEquals("gpt-5-nano", model)
    }

    @Test
    fun `premium tier chat routes to gpt5-mini`() {
        val model = router.resolve(UserTier.PREMIUM, RequestType.CHAT, isFirstReading = false)
        assertEquals("gpt-5-mini", model)
    }

    @Test
    fun `free tier daily card routes to gpt5-mini`() {
        val model = router.resolve(UserTier.FREE, RequestType.DAILY_CARD, isFirstReading = false)
        assertEquals("gpt-5-mini", model)
    }

    @Test
    fun `premium tier daily card routes to gpt5-mini`() {
        val model = router.resolve(UserTier.PREMIUM, RequestType.DAILY_CARD, isFirstReading = false)
        assertEquals("gpt-5-mini", model)
    }

    @Test
    fun `first reading overrides to gpt5 for free tier`() {
        val model = router.resolve(UserTier.FREE, RequestType.READING, isFirstReading = true)
        assertEquals("gpt-5", model)
    }

    @Test
    fun `first reading keeps gpt5 for premium tier`() {
        val model = router.resolve(UserTier.PREMIUM, RequestType.READING, isFirstReading = true)
        assertEquals("gpt-5", model)
    }
}
