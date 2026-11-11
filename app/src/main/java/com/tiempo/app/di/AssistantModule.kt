package com.tiempo.app.di

import com.tiempo.app.domain.assistant.CommandInterpreter
import com.tiempo.app.service.assistant.RoutingInterpreter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AssistantModule {

    @Binds
    abstract fun bindInterpreter(implementation: RoutingInterpreter): CommandInterpreter
}