package com.example.uniportal.ui.detail

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uniportal.R
import com.example.uniportal.domain.model.Shortlist
import com.example.uniportal.domain.rules.ShortlistError
import com.example.uniportal.domain.rules.ShortlistRules
import com.example.uniportal.ui.components.TestTags
import com.example.uniportal.ui.components.messageRes
import com.example.uniportal.ui.preview.PreviewData
import com.example.uniportal.ui.theme.UniPortalTheme

@Composable
fun ShortlistPickerDialog(
    shortlists: List<Shortlist>,
    state: ShortlistDialogState,
    onToggle: (Long) -> Unit,
    onNameChange: (String) -> Unit,
    onCreate: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val canCreate = state.newName.isNotBlank() && !state.isCreating
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(TestTags.SHORTLIST_DIALOG),
        icon = { Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = null) },
        title = { Text(text = stringResource(R.string.shortlist_dialog_title)) },
        text = {
            Column {
                if (shortlists.isEmpty()) {
                    Text(
                        text = stringResource(R.string.shortlist_dialog_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .heightIn(max = OPTIONS_MAX_HEIGHT)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        shortlists.forEach { shortlist ->
                            ShortlistOption(
                                shortlist = shortlist,
                                checked = shortlist.id in state.selectedIds,
                                onToggle = { onToggle(shortlist.id) },
                            )
                        }
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                OutlinedTextField(
                    value = state.newName,
                    onValueChange = onNameChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.SHORTLIST_DIALOG_NAME_FIELD),
                    label = { Text(text = stringResource(R.string.shortlist_dialog_new_label)) },
                    placeholder = { Text(text = stringResource(R.string.shortlist_dialog_new_placeholder)) },
                    trailingIcon = {
                        if (state.isCreating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(
                                onClick = onCreate,
                                enabled = canCreate,
                                modifier = Modifier.testTag(TestTags.SHORTLIST_DIALOG_CREATE_BUTTON),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = stringResource(R.string.cd_create_shortlist),
                                )
                            }
                        }
                    },
                    supportingText = {
                        val error = state.nameError
                        Text(
                            text = if (error != null) {
                                stringResource(error.messageRes())
                            } else {
                                stringResource(
                                    R.string.text_counter,
                                    state.newName.trim().length,
                                    ShortlistRules.NAME_MAX_LENGTH,
                                )
                            },
                        )
                    },
                    isError = state.nameError != null,
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { if (canCreate) onCreate() }),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !state.isCreating,
                modifier = Modifier.testTag(TestTags.SHORTLIST_DIALOG_CONFIRM_BUTTON),
            ) {
                val label = if (state.newName.isBlank()) {
                    R.string.action_done
                } else {
                    R.string.shortlist_dialog_create_and_save
                }
                Text(text = stringResource(label))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        iconContentColor = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun ShortlistOption(
    shortlist: Shortlist,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .testTag(TestTags.shortlistOption(shortlist.id))
            .heightIn(min = OPTION_MIN_HEIGHT)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Spacer(Modifier.width(12.dp))
        Text(
            text = shortlist.emoji,
            modifier = Modifier.clearAndSetSemantics {},
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = shortlist.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pluralStringResource(R.plurals.universities_count, shortlist.itemCount, shortlist.itemCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val OPTIONS_MAX_HEIGHT = 280.dp

private val OPTION_MIN_HEIGHT = 48.dp

@Preview
@Composable
private fun ShortlistPickerDialogPreview() {
    UniPortalTheme {
        ShortlistPickerDialog(
            shortlists = PreviewData.shortlists,
            state = ShortlistDialogState(
                selectedIds = setOf(1L),
                newName = PreviewData.shortlists.first().name,
                nameError = ShortlistError.NAME_DUPLICATE,
            ),
            onToggle = {},
            onNameChange = {},
            onCreate = {},
            onConfirm = {},
            onDismiss = {},
        )
    }
}
