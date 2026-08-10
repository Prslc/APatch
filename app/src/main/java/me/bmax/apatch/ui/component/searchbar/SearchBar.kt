package me.bmax.apatch.ui.component.searchbar

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode

/**
 * Shared search bar that dispatches to the Miuix or Material3 implementation
 * based on the active UI mode. It manages its own expanded state and clears
 * the query when the bar collapses, keeping behavior identical across the
 * dual variants and all pages (SuperUser / APM / KPM).
 *
 * @param query the current search text.
 * @param onQueryChange callback invoked when the text changes.
 * @param onSearch callback invoked when the IME search action is triggered.
 * @param placeholder hint text shown while the bar is collapsed and empty.
 * @param modifier the [Modifier] to be applied to the search bar container.
 */
@Composable
fun AppSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {},
    placeholder: String = "",
    content: @Composable ColumnScope.() -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    val onExpandedChange: (Boolean) -> Unit = {
        expanded = it
        if (!it) onQueryChange("")
    }

    when (LocalUiMode.current) {
        UiMode.Miuix -> AppSearchBarMiuix(
            query = query,
            onQueryChange = onQueryChange,
            modifier = modifier,
            onSearch = onSearch,
            placeholder = placeholder,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            content = content,
        )
        UiMode.Material -> AppSearchBarMaterial(
            query = query,
            onQueryChange = onQueryChange,
            modifier = modifier,
            onSearch = onSearch,
            placeholder = placeholder,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            content = content,
        )
    }
}
