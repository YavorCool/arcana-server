package com.schlepping.arcana.daily

import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.ext.get as koinGet

fun Application.dailyCardModule() = module {
    single<DailyCardRepository> { DailyCardRepositoryImpl() }
    single { DailyCardService(get(), get(), get(), get(), scope = this@dailyCardModule) }
}.also {
    monitor.subscribe(ApplicationStopped) {
        val service = this@dailyCardModule.koinGet<DailyCardService>()
        service.close()
    }
}
