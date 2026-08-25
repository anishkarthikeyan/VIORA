package com.viora.app.core.navigation

sealed class NavRoute(val route: String) {
    object Home : NavRoute("home")
    object Scanner : NavRoute("scanner")
    object Result : NavRoute("result")
    object History : NavRoute("history")
}
