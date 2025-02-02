@file:OptIn(ExperimentalAnimationApi::class)

package com.github.alexzhirkevich.lookandfeel.components.cupertino

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.github.alexzhirkevich.lookandfeel.components.ContextMenuScope
import com.github.alexzhirkevich.lookandfeel.components.CupertinoSection
import com.github.alexzhirkevich.lookandfeel.components.SectionHorizontalPadding
import com.github.alexzhirkevich.lookandfeel.components.SectionMinHeight
import com.github.alexzhirkevich.lookandfeel.theme.AdaptiveTheme
import com.github.alexzhirkevich.lookandfeel.theme.LocalPlatformConfiguration
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch




/**
 * Container of [CupertinoContextMenu].
 * [content] is blurred when any menus inside this container are shown.
 * All types of application already provide this container.
 * */
@Composable
fun ContextMenuContainer(
    content : @Composable () -> Unit
) {
    val provider = remember { ContextMenuProviderImpl() }

    val coroutineScope = rememberCoroutineScope()

    val animatedBlur by animateDpAsState(
        if (provider.visible) ContextMenuBlurRadius else 0.dp
    )

    CompositionLocalProvider(
        LocalContextMenuProvider provides provider
    ) {
        Box(
            modifier = Modifier
                .onGloballyPositioned {
                    provider.layoutCoordinates = it
                }
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .blur(animatedBlur)
                    .then(if (provider.visible)
                        Modifier
                            .pointerInput(provider) {
                                detectTapGestures {
                                    coroutineScope.launch {
                                        provider.wantsToBeDismissed.emit(true)
                                    }
                                }
                            }
                    else Modifier)
            ) {
                content()
                if (provider.visible) {
                    Box(Modifier
                        .zIndex(Float.MAX_VALUE)
                        .width(maxWidth)
                        .height(maxHeight)
                        .pointerInput(0) {
                            // prevent pointer input of content
                        }
                    )
                }
            }

            val density = LocalDensity.current

            val shouldShowMenuContent by remember {
                derivedStateOf {
                    provider.visible || animatedBlur > 0.dp
                }
            }

            Box(
                modifier = Modifier
                    .offset { provider.content.offset.round() }
            ) {
                if (shouldShowMenuContent) {
                    with(density) {
                        Box(
                            modifier = Modifier
                                .size(provider.content.size.toSize().toDpSize()),
                        ) {
                            provider.content.content()
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .offset {
                        provider.content.offset.round() +
                                IntOffset(0, provider.content.size.height)
                    }
                    .fillMaxWidth()
                    .padding(horizontal = SectionHorizontalPadding)
            ) {
                val scope = remember(provider.content.menu) {
                    ContextMenuScopeImpl().apply(provider.content.menu)
                }
                AnimatedVisibility(
                    modifier = Modifier
                        .align(provider.content.alignment),
                    visible = provider.visible,
                    enter = provider.content.enterTransition,
                    exit = provider.content.exitTransition
                ) {
                    scope.Content()
                }
            }
        }
    }
}

/**
 * Cupertino context menu.
 * Must be called inside [ContextMenuContainer] that will be blurred.
 * All types of application already provide such container.
 * */
@Composable
fun CupertinoContextMenu(
    visible : Boolean,
    onDismissRequest : () -> Unit,
    enterTransition : EnterTransition = scaleIn(
        animationSpec = tween(
            durationMillis = 100
        ),
        transformOrigin = TransformOrigin(.5f, 0f)
    ),
    exitTransition: ExitTransition = scaleOut(
        animationSpec = tween(
            durationMillis = 100
        ),
        transformOrigin = TransformOrigin(.5f, 0f)
    ),
    alignment: Alignment.Horizontal = Alignment.End,
    menu : ContextMenuScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val provider = LocalContextMenuProvider.current

    var position by remember {
        mutableStateOf(Offset.Zero)
    }

    var size by remember {
        mutableStateOf(IntSize.Zero)
    }

    LaunchedEffect(provider) {
        provider.wantsToBeDismissed.collect {
            if (it) {
                onDismissRequest()
            }
        }
    }

    val haptic = LocalHapticFeedback.current
    val hapticEnabled = LocalPlatformConfiguration.current?.platformHaptics == true

    LaunchedEffect(size, position, menu, visible, alignment, exitTransition, enterTransition) {
        if (visible) {
            if (!provider.visible && hapticEnabled){
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            provider.show(
                ContextMenuContent(
                    size = size,
                    offset = position,
                    menu = menu,
                    content = content,
                    alignment = alignment,
                    enterTransition = enterTransition,
                    exitTransition = exitTransition
                )
            )
        } else {
            provider.dismiss()
        }
    }

    Box(modifier = modifier
        .onGloballyPositioned {
            val containerInWindow = provider.layoutCoordinates
                ?.positionInWindow() ?: Offset.Zero

            position = it.positionInWindow() - containerInWindow
            size = it.size
        }.onSizeChanged {
            size = it
        }
    ) {
        content()
    }
}

private val ContextMenuBlurRadius = 50.dp
private val ContextMenuWidth = 270.dp


private val LocalContextMenuProvider = staticCompositionLocalOf<ContextMenuProvider> {
    error("Context menu container is not set")
}

private class ContextMenuContent(
    val size : IntSize,
    val offset: Offset,
    val menu : ContextMenuScope.() -> Unit,
    val content : @Composable () -> Unit,
    val alignment: Alignment.Horizontal,
    val enterTransition: EnterTransition,
    val exitTransition: ExitTransition
)

private interface ContextMenuProvider {

    val wantsToBeDismissed : SharedFlow<Boolean>

    val visible : Boolean

    val layoutCoordinates : LayoutCoordinates?

    fun show(content: ContextMenuContent)

    fun dismiss()
}

private class ContextMenuScopeImpl : ContextMenuScope {

    private val items = mutableListOf<@Composable (PaddingValues) -> Unit>()

    override fun item(content: @Composable (PaddingValues) -> Unit) {
        items.add(content)
    }

    override fun label(
        enabled: Boolean,
        onClick: () -> Unit,
        icon: @Composable () -> Unit,
        title: @Composable () -> Unit
    ) = row(
            title = title,
            content = icon,
            onClick = onClick,
            enabled = enabled
        )


    @Composable
    fun Content() {
        CupertinoSection(
            modifier = Modifier.width(ContextMenuWidth)
        ) {
            items.forEach { content ->
                item(
                    dividerPadding = 0.dp
                ) {
                    content.invoke(it)
                }
            }
        }
    }

    private fun row(
        modifier: Modifier = Modifier,
        enabled: Boolean,
        onClick: () -> Unit,
        title: @Composable () -> Unit,
        content: @Composable () -> Unit,
    ) = item {

        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()

        val haptic = LocalHapticFeedback.current
        val hapticEnabled = LocalPlatformConfiguration.current?.platformHaptics == true

        LaunchedEffect(pressed){
            if (pressed && hapticEnabled){
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }

        Row(
            modifier = modifier
                .heightIn(SectionMinHeight)
                .fillMaxWidth()
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                    role = Role.Button,
                    interactionSource = interactionSource,
                    indication = LocalIndication.current
                )
                .padding(it),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ProvideTextStyle(AdaptiveTheme.typography.bodyLarge) {
                title()
            }

            content()
        }
    }
}

private class ContextMenuProviderImpl : ContextMenuProvider {

    override val wantsToBeDismissed = MutableSharedFlow<Boolean>()

    var content : ContextMenuContent by mutableStateOf(ContextMenuContent(
        size = IntSize.Zero,
        offset = Offset.Zero,
        menu = {},
        content = {},
        alignment = Alignment.End,
        enterTransition = EnterTransition.None,
        exitTransition = ExitTransition.None
    ))

    override var visible: Boolean by mutableStateOf(false)

    override var layoutCoordinates: LayoutCoordinates? by mutableStateOf(null)

    override fun show(content: ContextMenuContent) {
        this.content = content
        visible = true
    }

    override fun dismiss() {
        visible = false
    }
}