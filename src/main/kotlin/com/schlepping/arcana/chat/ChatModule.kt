package com.schlepping.arcana.chat

import org.koin.dsl.module

fun chatModule() = module {
    single<ChatRepository> { ChatRepositoryImpl() }
    single { ChatService(get(), get(), get(), get(), get()) }
}
