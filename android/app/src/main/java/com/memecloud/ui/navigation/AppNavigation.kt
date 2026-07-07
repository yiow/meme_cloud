package com.memecloud.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
import com.memecloud.ui.community.BountyScreen
import com.memecloud.ui.community.CommunityScreen
import com.memecloud.ui.community.RankingScreen
import com.memecloud.ui.community.TopicChallengeScreen
import com.memecloud.ui.home.HomeScreen
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

    // 哪些页面不显示底部导航栏
    val hideBottomBar = currentRoute in listOf(
        Routes.LOGIN, Routes.REGISTER, Routes.MEME_DETAIL,
        Routes.RANKING, Routes.TOPIC_CHALLENGE, Routes.BOUNTY,
        Routes.BATTLE_ROOM, Routes.IMITATION_CONTEST, Routes.FOLLOW
    )

    Scaffold(
        bottomBar = {
            if (!hideBottomBar) {
                NavigationBar {
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
                            label = { Text(screen.title) }
                        )
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
                    onGoPublish = { /* TODO: 创作发布页 */ }
                )
            }

            composable(Screen.Battle.route) {
                BattleScreen(
                    onEnterRoom = { roomId, roomName ->
                        navController.navigate("${Routes.BATTLE_ROOM}/$roomId/$roomName")
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
                    onGoFollow = { navController.navigate(Routes.FOLLOW) }
                )
            }

            // ── 社区子页面 ──
            composable(Routes.RANKING) {
                RankingScreen(onBack = { navController.popBackStack() })
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

            // ── 预留 ──
            composable(
                Routes.MEME_DETAIL,
                arguments = listOf(navArgument("memeId") { type = NavType.StringType })
            ) { entry ->
                val memeId = entry.arguments?.getString("memeId") ?: ""
                Text("表情包详情: $memeId")
            }
        }
    }
}
