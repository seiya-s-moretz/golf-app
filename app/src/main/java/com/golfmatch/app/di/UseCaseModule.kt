package com.golfmatch.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * UseCase生成モジュール（技術設計書 4章）。
 *
 * `domain/usecase` 配下の各UseCaseクラスは `@Inject constructor` を持つため、
 * Hiltが自動的にインスタンス化できる（Repository実装への依存もRepositoryModuleの束縛経由で解決される）。
 * そのため本モジュールに明示的な `@Provides` は不要であり、現時点では空のモジュールとして
 * ディレクトリ構成（技術設計書4章）との対応のみを示す。
 *
 * 参考実装（`D:\勉強\golf\設計書.md` 7-2章）はUseCaseごとに明示的な `@Provides` を記載しているが、
 * `@Inject constructor` を持つクラスに対して同じ型を重複して提供するとHiltのビルドエラーになるため、
 * 本プロジェクトでは `@Inject constructor` 方式に統一した（設計からの意図的な逸脱、DeveloperAgent判断）。
 * 将来、複数実装の切り替え等でUseCase生成に手動ワイヤリングが必要になった場合はここに `@Provides` を追加する。
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule
