package com.artf.shopinglistcompose.ui.view.layout

import androidx.compose.*
import com.artf.data.database.model.ShoppingList
import java.lang.Exception
import java.util.ArrayDeque

sealed class Screen {
    object ShoppingListCurrent : Screen()
    object ShoppingListArchived : Screen()
    class ProductListCurrent(val shoppingList: ShoppingList) : Screen()
    class ProductListArchived(val shoppingList: ShoppingList) : Screen()
}

@Model
class ScreenBackStack {
    var currentScreen: Screen = Screen.ShoppingListCurrent
    private val backStack = ArrayDeque<Screen>()

    init {
        push(Screen.ShoppingListCurrent)
    }

    fun pop(): Screen? {
        return try {
            backStack.pop()
            backStack.peekFirst()?.also { currentScreen = it }
        } catch (e: Exception) {
            null
        }
    }

    fun push(screen: Screen) {
        backStack.push(screen)
        currentScreen = screen
    }
}

val ScreenBackStackAmbient = ambientOf<ScreenBackStack> {
    throw IllegalStateException("backPressHandler is not initialized")
}