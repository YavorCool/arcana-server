package com.schlepping.arcana.daily

import org.koin.dsl.module

fun dailyCardModule() = module {
    single<DailyCardRepository> { DailyCardRepositoryImpl() }
    single { DailyCardService(get(), get(), get(), get()) }
}
