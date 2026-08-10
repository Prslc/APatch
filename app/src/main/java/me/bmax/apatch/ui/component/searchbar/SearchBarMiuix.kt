package me.bmax.apatch.ui.component.searchbar

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.SearchBar

@Composable
internal fun AppSearchBarMiuix(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier,
    onSearch: () -> Unit,
    placeholder: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    SearchBar(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        inputField = {
            InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { onSearch() },
                expanded = expanded,
                label = placeholder,
                onExpandedChange = onExpandedChange,
            )
        },
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        content = content,
    )
}
