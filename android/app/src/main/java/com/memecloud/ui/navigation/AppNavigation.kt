package com.memecloud.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.memecloud.ui.auth.LoginScreen
import com.memecloud.ui.auth.RegisterScreen
import com.memecloud.ui.battle.BattleRoomScreen
import com.memecloud.ui.battle.BattleScreen
import com.memecloud.ui.battle.ImitationContestScreen
import com.memecloud.ui.community.*
import com.memecloud.ui.home.HomeScreen
import com.memecloud.ui.profile.CollectionsScreen
import com.memecloud.ui.profile.EmojiLibraryScreen
import com.memecloud.ui.profile.FollowScreen
import com.memecloud.ui.profile.ProfileScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    isLoggedIn: Boolean,
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val hideBottomBar = currentRoute in listOf(
        Routes.LOGIN, Routes.REGISTER, Routes.MEME_DETAIL,
        Routes.RANKING, Routes.TOPIC_CHALLENGE, Routes.BOUNTY, Routes.PUBLISH,
        Routes.BATTLE_ROOM, Routes.IMITATION_CONTEST, Routes.FOLLOW, Routes.COLLECTIONS, Routes.EMOJI_LIBRARY
    )

    Scaffold(
        bottomBar = {
            if (!hideBottomBar) {
                Column {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        thickness = androidx.compose.ui.unit.Dp(0.5f)
                    )
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        Screen.bottomTabs.forEach { screen ->
                            val selected = currentRoute == screen.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(Screen.Home.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        if (selected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = { Text(screen.title) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor   = MaterialTheme.colorScheme.primary,
                                    selectedTextColor   = MaterialTheme.colorScheme.primary,
                                    indicatorColor      = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) Screen.Home.route else Routes.LOGIN,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ── 登录注册 ──
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onGoRegister = { navController.navigate(Routes.REGISTER) }
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onGoBack = { navController.popBackStack() }
                )
            }

            // ── 四大主页 Tab ──
            composable(Screen.Home.route) { HomeScreen() }

            composable(Screen.Community.route) {
                CommunityScreen(
                    onGoRanking = { navController.navigate(Routes.RANKING) },
                    onGoChallenge = { navController.navigate(Routes.TOPIC_CHALLENGE) },
                    onGoBounty = { navController.navigate(Routes.BOUNTY) },
                    onGoPublish = { navController.navigate(Routes.PUBLISH) },
                    onGoDetail = { postId -> navController.navigate("meme_detail/$postId") }
                )
            }

            composable(Screen.Battle.route) {
                BattleScreen(
                    onEnterRoom = { roomId, roomName ->
                        navController.navigate(Routes.battleRoom(roomId, roomName))
                    },
                    onGoContest = { navController.navigate(Routes.IMITATION_CONTEST) }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onGoFollow = { navController.navigate(Routes.FOLLOW) },
                    onGoCollections = { navController.navigate(Routes.COLLECTIONS) },
                    onGoEmojiLibrary = { navController.navigate(Routes.EMOJI_LIBRARY) }
                )
            }

            // ── 社区子页面 ──
            composable(
                "meme_detail/{postId}",
                arguments = listOf(navArgument("postId") { type = NavType.LongType })
            ) { entry ->
                val postId = entry.arguments?.getLong("postId") ?: 0L
                MemeDetailScreen(
                    postId = postId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.PUBLISH) {
                PublishScreen(
                    onBack = { navController.popBackStack() },
                    onPublishSuccess = {
                        navController.popBackStack()
                        navController.navigate(Screen.Community.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Routes.RANKING) {
                RankingScreen(
                    onBack = { navController.popBackStack() },
                    onGoDetail = { postId -> navController.navigate("meme_detail/$postId") }
                )
            }
            composable(Routes.TOPIC_CHALLENGE) {
                TopicChallengeScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.BOUNTY) {
                BountyScreen(onBack = { navController.popBackStack() })
            }

            // ── 斗图子页面 ──
            composable(
                Routes.BATTLE_ROOM,
                arguments = listOf(
                    navArgument("roomId") { type = NavType.StringType },
                    navArgument("roomName") { type = NavType.StringType }
                )
            ) { entry ->
                val roomId = entry.arguments?.getString("roomId") ?: ""
                val roomName = entry.arguments?.getString("roomName") ?: ""
                BattleRoomScreen(
                    roomId = roomId,
                    roomName = roomName,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.IMITATION_CONTEST) {
                ImitationContestScreen(onBack = { navController.popBackStack() })
            }

            // ── 个人中心子页面 ──
            composable(Routes.FOLLOW) {
                FollowScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.EMOJI_LIBRARY) {
                EmojiLibraryScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.COLLECTIONS) {
                CollectionsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
