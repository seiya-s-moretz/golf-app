package com.golfmatch.app.ui.navigation

/**
 * 画面遷移のRoute定義（技術設計書 3章：既存4画面 + 新規追加画面）。
 *
 * フッターメニュー5タブ化（メッセージ一覧をトップレベルタブにするか）は
 * 技術設計書3-3章の要確認事項だが、Route定義自体はどちらの構成でも利用できる形にしてある。
 */
sealed class Route(val route: String) {

    // --- 既存4画面（技術設計書 3-1章） ---
    data object Home : Route("home")
    data object Recommend : Route("recommend")
    data object Board : Route("board")
    data object MyPage : Route("mypage")

    // --- ホーム関連（新規） ---
    data object CreateRound : Route("round/create")

    data object RoundDetail : Route("round/{eventId}") {
        const val ARG_EVENT_ID = "eventId"
        fun createRoute(eventId: String) = "round/$eventId"
    }

    data object RoundJoinRequestList : Route("round/{eventId}/join-requests") {
        const val ARG_EVENT_ID = "eventId"
        fun createRoute(eventId: String) = "round/$eventId/join-requests"
    }

    // --- おすすめユーザー関連（新規） ---
    data object MatchRequestList : Route("match-requests")

    // --- 掲示板関連（新規） ---
    data object CreateBoardPost : Route("board/create")

    // --- マイページ関連（新規） ---
    data object BlockedUsers : Route("mypage/blocked-users")

    // --- メッセージ（新規） ---
    data object MessageList : Route("messages")

    data object MessageThread : Route("messages/{partnerId}") {
        const val ARG_PARTNER_ID = "partnerId"
        fun createRoute(partnerId: String) = "messages/$partnerId"
    }

    // --- 通報（新規、ダイアログ destination） ---
    data object Report : Route("report/{targetType}/{targetId}") {
        const val ARG_TARGET_TYPE = "targetType"
        const val ARG_TARGET_ID = "targetId"
        fun createRoute(targetType: String, targetId: String) = "report/$targetType/$targetId"
    }

    // --- 本人確認・新規登録フロー（新規、ADR-0003） ---
    data object PhoneNumberInput : Route("auth/phone")

    data object OtpVerification : Route("auth/phone/{phoneNumber}/otp") {
        const val ARG_PHONE_NUMBER = "phoneNumber"
        fun createRoute(phoneNumber: String) = "auth/phone/$phoneNumber/otp"
    }

    data object InitialProfile : Route("auth/register/{registrationToken}") {
        const val ARG_REGISTRATION_TOKEN = "registrationToken"
        fun createRoute(registrationToken: String) = "auth/register/$registrationToken"
    }
}
