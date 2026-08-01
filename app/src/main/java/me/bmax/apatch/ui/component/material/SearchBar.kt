package me.bmax.apatch.ui.component.material

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * A layout container for a [SearchBar] in the MD3 Expressive style.
 *
 * The [inputField] is placed in a row; when [expanded], the optional [outsideEndAction]
 * slides in at the end of the row and [content] is revealed below the field.
 *
 * @param inputField the input component of the search bar.
 * @param onExpandedChange callback invoked when the expanded/collapsed state changes.
 * @param modifier the [Modifier] to be applied to the container.
 * @param expanded whether the search bar is currently expanded.
 * @param outsideEndAction optional action displayed at the end of the field row while
 *   expanded (e.g. a "Cancel" text button).
 * @param content optional content shown below the field while expanded (e.g. search results
 *   or suggestions).
 */
@Composable
fun ExpressiveSearchBar(
    inputField: @Composable () -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    outsideEndAction: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(ExpressiveSearchBarDefaults.ContentPadding)
            ) {
                inputField()
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandHorizontally() + slideInHorizontally(initialOffsetX = { it }),
                exit = shrinkHorizontally() + slideOutHorizontally(targetOffsetX = { it })
            ) {
                outsideEndAction?.invoke()
            }
        }

        AnimatedVisibility(
            visible = expanded
        ) {
            content()
        }
    }
}

/**
 * A text input field for the MD3 Expressive style search bar.
 *
 * Renders a [BasicTextField] in a fully rounded (capsule) [Surface], with a search icon as
 * the default leading icon and a clear button that appears while [query] is non-empty.
 * Focusing the field triggers [onExpandedChange] with `true`; collapsing clears the query
 * with a brief fade-out.
 *
 * @param query the current search text.
 * @param onQueryChange callback invoked when the text changes.
 * @param onSearch callback invoked when the IME search action is triggered.
 * @param expanded whether the search bar is currently expanded.
 * @param onExpandedChange callback invoked when the expanded/collapsed state changes.
 * @param modifier the [Modifier] to be applied to the input field.
 * @param placeholder hint text shown while [query] is empty and the bar is not expanded.
 * @param enabled whether the field accepts input.
 * @param textStyle style merged over [MaterialTheme.typography.bodyLarge] for the input text.
 * @param leadingIcon optional leading icon; defaults to a search icon.
 * @param trailingIcon optional trailing icon; defaults to a clear button.
 * @param interactionSource optional hoisted [MutableInteractionSource] for observing focus
 *   interactions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveInputField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    textStyle: TextStyle? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null
) {
    val currentOnQueryChange by rememberUpdatedState(onQueryChange)
    val currentOnSearch by rememberUpdatedState(onSearch)
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    val internalInteractionSource = interactionSource ?: remember { MutableInteractionSource() }

    val shape = CircleShape

    val actualLeadingIcon = leadingIcon ?: {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = ExpressiveSearchBarDefaults.LeadingIconStartPadding,
                end = ExpressiveSearchBarDefaults.LeadingIconEndPadding
            )
        )
    }

    val actualTrailingIcon = trailingIcon ?: {
        AnimatedVisibility(
            visible = query.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier.padding(
                    start = ExpressiveSearchBarDefaults.TrailingIconStartPadding,
                    end = ExpressiveSearchBarDefaults.TrailingIconEndPadding
                ),
                contentAlignment = Alignment.CenterStart
            ) {
                IconButton(
                    onClick = { currentOnQueryChange("") },
                    modifier = Modifier.clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    val focused = internalInteractionSource.collectIsFocusedAsState().value
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val textAlpha = remember { Animatable(1f) }

    val textColor = MaterialTheme.colorScheme.onSurface
    val inputTextStyle = MaterialTheme.typography.bodyLarge
        .copy(fontWeight = FontWeight.Normal)
        .merge(textStyle)
        .copy(color = textColor)

    val cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
    val placeholderText by remember(query, expanded, placeholder) {
        derivedStateOf { if (query.isEmpty() && !expanded) placeholder else "" }
    }

    BasicTextField(
        value = query,
        onValueChange = currentOnQueryChange,
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused) currentOnExpandedChange(true) }
            .semantics {
                onClick {
                    focusRequester.requestFocus()
                    true
                }
            },
        enabled = enabled,
        singleLine = true,
        textStyle = inputTextStyle,
        cursorBrush = cursorBrush,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { currentOnSearch(query) }),
        interactionSource = internalInteractionSource,
        decorationBox = { innerTextField ->
            Surface(
                shape = shape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    actualLeadingIcon()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = ExpressiveSearchBarDefaults.InputFieldMinHeight),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (placeholderText.isNotEmpty()) {
                            Text(
                                text = placeholderText,
                                style = MaterialTheme.typography.bodyLarge.merge(textStyle),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(modifier = Modifier.graphicsLayer { alpha = textAlpha.value }) {
                            innerTextField()
                        }
                    }
                    actualTrailingIcon()
                }
            }
        }
    )

    LaunchedEffect(expanded) {
        if (expanded) {
            focusRequester.requestFocus()
        } else if (focused) {
            delay(100.milliseconds)
            if (query.isNotEmpty()) {
                textAlpha.animateTo(0f)
                currentOnQueryChange("")
                textAlpha.snapTo(1f)
            }
            focusManager.clearFocus()
        }
    }
}

/** Default padding and size values for the [ExpressiveSearchBar] components. */
object ExpressiveSearchBarDefaults {
    val ContentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    val InputFieldMinHeight = 48.dp
    val LeadingIconStartPadding = 16.dp
    val LeadingIconEndPadding = 8.dp
    val TrailingIconStartPadding = 8.dp
    val TrailingIconEndPadding = 12.dp
}