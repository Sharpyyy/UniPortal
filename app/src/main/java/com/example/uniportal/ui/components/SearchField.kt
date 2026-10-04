package com.example.uniportal.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uniportal.R
import com.example.uniportal.ui.theme.UniPortalTheme

@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.search_placeholder),
    clearContentDescription: String = stringResource(R.string.cd_clear_search),
    latinHint: String = stringResource(R.string.search_latin_hint),
    tag: String = TestTags.SEARCH_FIELD,
) {
    val focusManager = LocalFocusManager.current
    val colors = MaterialTheme.colorScheme

    val hasCyrillic = query.any { it in CYRILLIC }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .testTag(tag),
        placeholder = {
            Text(
                text = placeholder,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = clearContentDescription,
                    )
                }
            }
        } else {
            null
        },
        supportingText = if (hasCyrillic) {
            { Text(text = latinHint) }
        } else {
            null
        },
        isError = hasCyrillic,
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,

        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceContainer,
            unfocusedContainerColor = colors.surfaceContainer,
            errorContainerColor = colors.surfaceContainer,
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.outline,
            focusedLeadingIconColor = colors.primary,
            unfocusedLeadingIconColor = colors.onSurfaceVariant,
            cursorColor = colors.primary,
        ),
    )
}

private val CYRILLIC = '\u0400'..'\u04FF'

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun SearchFieldPreview() {
    UniPortalTheme {
        SearchField(query = "Moscow", onQueryChange = {}, modifier = Modifier.padding(16.dp))
    }
}
