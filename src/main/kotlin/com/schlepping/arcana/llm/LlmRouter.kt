package com.schlepping.arcana.llm

class LlmRouter(private val config: LlmRoutingConfig) {

    fun resolve(tier: UserTier, requestType: RequestType, isFirstReading: Boolean = false): String {
        if (isFirstReading && requestType == RequestType.READING) {
            return config.firstReading
        }

        return when (requestType) {
            RequestType.READING -> when (tier) {
                UserTier.FREE -> config.freeReading
                UserTier.PREMIUM -> config.premiumReading
            }
            RequestType.CHAT -> when (tier) {
                UserTier.FREE -> config.freeChat
                UserTier.PREMIUM -> config.premiumChat
            }
            RequestType.DAILY_CARD -> config.dailyCard
        }
    }
}
