package com.golfmatch.app.di

import com.golfmatch.app.data.repository.impl.AreaRepositoryImpl
import com.golfmatch.app.data.repository.impl.AuthRepositoryImpl
import com.golfmatch.app.data.repository.impl.BoardRepositoryImpl
import com.golfmatch.app.data.repository.impl.MatchRepositoryImpl
import com.golfmatch.app.data.repository.impl.MessageRepositoryImpl
import com.golfmatch.app.data.repository.impl.ReportRepositoryImpl
import com.golfmatch.app.data.repository.impl.RoundRepositoryImpl
import com.golfmatch.app.data.repository.impl.UserRepositoryImpl
import com.golfmatch.app.domain.repository.AreaRepository
import com.golfmatch.app.domain.repository.AuthRepository
import com.golfmatch.app.domain.repository.BoardRepository
import com.golfmatch.app.domain.repository.MatchRepository
import com.golfmatch.app.domain.repository.MessageRepository
import com.golfmatch.app.domain.repository.ReportRepository
import com.golfmatch.app.domain.repository.RoundRepository
import com.golfmatch.app.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * domain/repositoryのインターフェースをdata/repository/implの実装へ束縛するモジュール
 * （技術設計書 4章）。各Impl内部のFirebase接続処理・API通信ロジックの本実装は次フェーズで行う。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindRoundRepository(impl: RoundRepositoryImpl): RoundRepository

    @Binds
    @Singleton
    abstract fun bindBoardRepository(impl: BoardRepositoryImpl): BoardRepository

    @Binds
    @Singleton
    abstract fun bindMatchRepository(impl: MatchRepositoryImpl): MatchRepository

    @Binds
    @Singleton
    abstract fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository

    @Binds
    @Singleton
    abstract fun bindReportRepository(impl: ReportRepositoryImpl): ReportRepository

    @Binds
    @Singleton
    abstract fun bindAreaRepository(impl: AreaRepositoryImpl): AreaRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
