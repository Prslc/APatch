package me.bmax.apatch.ui.component.searchbar

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.bmax.apatch.ui.component.material.ExpressiveInputField
import me.bmax.apatch.ui.component.material.ExpressiveSearchBar

@Composable
internal fun AppSearchBarMaterial(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier,
    onSearch: () -> Unit,
    placeholder: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ExpressiveSearchBar(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        inputField = {
            ExpressiveInputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { onSearch() },
                expanded = expanded,
                placeholder = placeholder,
                onExpandedChange = onExpandedChange,
            )
        },
        content = content,
    )
}
