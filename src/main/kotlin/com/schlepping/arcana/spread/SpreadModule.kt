package com.schlepping.arcana.spread

import org.koin.dsl.module

fun spreadModule() = module {
    single<SpreadRepository> { SpreadRepositoryImpl() }
    single { SpreadService(get(), get(), get(), get()) }
}
