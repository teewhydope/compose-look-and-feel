package com.github.alexzhirkevich.lookandfeel.components.cupertino

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.github.alexzhirkevich.lookandfeel.components.TopBarType
import com.github.alexzhirkevich.lookandfeel.util.safeContent

@Composable
@ExperimentalMaterial3Api
fun CupertinoScaffold(
    modifier: Modifier,
    topBarType: TopBarType,
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit,
    floatingActionButtonPosition: FabPosition,
    containerColor: Color,
    contentColor: Color,
    contentWindowInsets: WindowInsets,
    content: @Composable (PaddingValues, @Composable (ScrollableState?) -> Unit) -> Unit
) {

    val inlineTopBar by rememberUpdatedState(topBarType != TopBarType.Large)

    androidx.compose.material3.Scaffold(
        modifier = modifier,
        topBar = {
            AnimatedVisibility(
                visible = inlineTopBar,
                enter = fadeIn(),
                exit = fadeOut()
            ){
                Column {
                    topBar()
                    Divider()
                }
            }
        },
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
        contentWindowInsets = contentWindowInsets
    ) {
        content(it) {

        }
    }
}