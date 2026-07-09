package com.memecloud.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 底部导航项定义 — 对应 PRD 线框图底栏四个 Tab
 */
sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen(
        route = "home",
        title = "首页",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Community : Screen(
        route = "community",
        title = "社区",
        selectedIcon = Icons.Filled.Public,
        unselectedIcon = Icons.Outlined.Public
    )

    data object Battle : Screen(
        route = "battle",
        title = "斗图",
        selectedIcon = Icons.Filled.Bolt,
        unselectedIcon = Icons.Outlined.Bolt
    )

    data object Profile : Screen(
        route = "profile",
        title = "我的",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )

    companion object {
        val bottomTabs = listOf(Home, Community, Battle, Profile)
    }
}

/** 非底部 Tab 的子页面路由 */
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MEME_DETAIL = "meme_detail/{memeId}"
    const val PUBLISH = "publish"
    const val SEARCH_RESULT = "search_result"

    // 社区子页
    const val RANKING = "ranking"
    const val TOPIC_CHALLENGE = "topic_challenge"
    const val BOUNTY = "bounty"

    // 斗图子页
    const val BATTLE_ROOM_BASE = "battle_room"
    const val BATTLE_ROOM = "battle_room/{roomId}/{roomName}"
    const val IMITATION_CONTEST = "imitation_contest"

    // 个人中心子页
    /** 拼装带参数的斗图室路由 */
    fun battleRoom(roomId: String, roomName: String): String =
        "battle_room/${java.net.URLEncoder.encode(roomId, "UTF-8")}/${java.net.URLEncoder.encode(roomName, "UTF-8")}"
    const val FOLLOW = "follow"
}
