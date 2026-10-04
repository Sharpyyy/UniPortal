package com.example.uniportal.ui.shortlist

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.uniportal.R
import com.example.uniportal.domain.rules.ShortlistError
import com.example.uniportal.domain.rules.ShortlistNameValidation
import com.example.uniportal.domain.rules.ShortlistRules
import com.example.uniportal.ui.components.EmojiBadge
import com.example.uniportal.ui.components.TestTags
import com.example.uniportal.ui.components.messageRes
import com.example.uniportal.ui.theme.UniPortalTheme

data class ShortlistFormInput(
    val editingId: Long? = null,
    val name: String = "",
    val emoji: String = ShortlistRules.DEFAULT_EMOJI,
    val description: String = "",

    val nameTouched: Boolean = false,

    val submitError: ShortlistError? = null,
    val isSaving: Boolean = false,
)

data class ShortlistFormState(
    val isEdit: Boolean,
    val name: String,
    val emoji: String,
    val description: String,
    val nameError: ShortlistError?,
    val descriptionError: ShortlistError?,
    val isSaving: Boolean,
    val canSave: Boolean,
) {
    val nameLength: Int
        get() = name.trim().length

    val descriptionLength: Int
        get() = description.trim().length
}

sealed interface ShortlistFormEvent {
    data class NameChanged(val name: String) : ShortlistFormEvent
    data class EmojiSelected(val emoji: String) : ShortlistFormEvent
    data class DescriptionChanged(val description: String) : ShortlistFormEvent
    data object Submit : ShortlistFormEvent
    data object Dismiss : ShortlistFormEvent
}

fun ShortlistFormInput.edit(event: ShortlistFormEvent): ShortlistFormInput = when (event) {
    is ShortlistFormEvent.NameChanged -> copy(name = event.name, nameTouched = true, submitError = null)
    is ShortlistFormEvent.EmojiSelected -> copy(emoji = event.emoji)
    is ShortlistFormEvent.DescriptionChanged -> copy(description = event.description, submitError = null)
    ShortlistFormEvent.Submit, ShortlistFormEvent.Dismiss -> this
}

fun ShortlistFormInput.toFormState(otherNames: Collection<String>): ShortlistFormState {
    val nameValidation = ShortlistRules.validateName(name, otherNames)
    val liveNameError = (nameValidation as? ShortlistNameValidation.Invalid)?.error
        ?.takeUnless { it == ShortlistError.NAME_EMPTY && !nameTouched }
    val descriptionTooLong = description.trim().length > ShortlistRules.DESCRIPTION_MAX_LENGTH
    val rejected = submitError
    return ShortlistFormState(
        isEdit = editingId != null,
        name = name,
        emoji = emoji,
        description = description,
        nameError = liveNameError ?: rejected?.takeUnless { it == ShortlistError.DESCRIPTION_TOO_LONG },
        descriptionError = if (descriptionTooLong) {
            ShortlistError.DESCRIPTION_TOO_LONG
        } else {
            rejected?.takeIf { it == ShortlistError.DESCRIPTION_TOO_LONG }
        },
        isSaving = isSaving,
        canSave = nameValidation is ShortlistNameValidation.Valid && !descriptionTooLong && !isSaving,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShortlistFormDialog(
    state: ShortlistFormState,
    onEvent: (ShortlistFormEvent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onEvent(ShortlistFormEvent.Dismiss) },
        modifier = Modifier.testTag(TestTags.SHORTLIST_FORM),
        icon = { EmojiBadge(emoji = state.emoji, size = 56.dp) },
        title = {
            Text(
                text = stringResource(
                    if (state.isEdit) R.string.shortlist_form_edit_title else R.string.shortlist_form_create_title,
                ),
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { onEvent(ShortlistFormEvent.NameChanged(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.SHORTLIST_FORM_NAME_FIELD),
                    label = { Text(text = stringResource(R.string.shortlist_form_name_label)) },
                    placeholder = { Text(text = stringResource(R.string.shortlist_dialog_new_placeholder)) },
                    supportingText = {
                        FieldSupport(
                            error = state.nameError,
                            length = state.nameLength,
                            maxLength = ShortlistRules.NAME_MAX_LENGTH,
                        )
                    },
                    isError = state.nameError != null,
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.shortlist_form_emoji_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ShortlistRules.EMOJI_OPTIONS.forEachIndexed { index, emoji ->
                        EmojiOption(
                            emoji = emoji,
                            selected = emoji == state.emoji,
                            onClick = { onEvent(ShortlistFormEvent.EmojiSelected(emoji)) },
                            modifier = Modifier.testTag(TestTags.shortlistEmoji(index)),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = state.description,
                    onValueChange = { onEvent(ShortlistFormEvent.DescriptionChanged(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.SHORTLIST_FORM_DESCRIPTION_FIELD),
                    label = { Text(text = stringResource(R.string.shortlist_form_description_label)) },
                    placeholder = { Text(text = stringResource(R.string.shortlist_form_description_placeholder)) },
                    supportingText = {
                        FieldSupport(
                            error = state.descriptionError,
                            length = state.descriptionLength,
                            maxLength = ShortlistRules.DESCRIPTION_MAX_LENGTH,
                        )
                    },
                    isError = state.descriptionError != null,
                    minLines = DESCRIPTION_MIN_LINES,
                    maxLines = DESCRIPTION_MAX_LINES,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (state.canSave) onEvent(ShortlistFormEvent.Submit) },
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(ShortlistFormEvent.Submit) },
                enabled = state.canSave,
                modifier = Modifier.testTag(TestTags.SHORTLIST_FORM_SAVE_BUTTON),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                }
                Text(text = stringResource(if (state.isEdit) R.string.action_save else R.string.action_create))
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(ShortlistFormEvent.Dismiss) }) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}

@Composable
private fun FieldSupport(
    error: ShortlistError?,
    length: Int,
    maxLength: Int,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (error != null) stringResource(error.messageRes()) else "",
            modifier = Modifier.weight(1f),
        )
        Text(text = stringResource(R.string.text_counter, length, maxLength))
    }
}

@Composable
private fun EmojiOption(
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val description = stringResource(R.string.cd_shortlist_emoji, emoji)

    val fontSize = with(LocalDensity.current) { EMOJI_GLYPH_SIZE.toSp() }
    Box(
        modifier = modifier
            .size(EMOJI_OPTION_SIZE)
            .clip(CircleShape)
            .background(if (selected) colors.primaryContainer else colors.surfaceContainer)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.primary else colors.outline,
                shape = CircleShape,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, fontSize = fontSize, maxLines = 1)
    }
}

private const val DESCRIPTION_MIN_LINES = 2
private const val DESCRIPTION_MAX_LINES = 4

private val EMOJI_OPTION_SIZE = 48.dp
private val EMOJI_GLYPH_SIZE = 22.dp

@Preview
@Composable
private fun ShortlistFormDialogPreview() {
    UniPortalTheme {
        ShortlistFormDialog(
            state = ShortlistFormInput(name = "Dream", emoji = "🎯", nameTouched = true)
                .toFormState(otherNames = listOf("dream")),
            onEvent = {},
        )
    }
}
