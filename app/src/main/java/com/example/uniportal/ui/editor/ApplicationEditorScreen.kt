package com.example.uniportal.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uniportal.R
import com.example.uniportal.domain.model.ApplicationStatus
import com.example.uniportal.domain.model.University
import com.example.uniportal.domain.rules.ApplicationError
import com.example.uniportal.domain.rules.ApplicationRules
import com.example.uniportal.ui.components.CountryBadge
import com.example.uniportal.ui.components.DeadlineChip
import com.example.uniportal.ui.components.ErrorState
import com.example.uniportal.ui.components.LoadingState
import com.example.uniportal.ui.components.PortalSnackbarHost
import com.example.uniportal.ui.components.SectionCard
import com.example.uniportal.ui.components.TestTags
import com.example.uniportal.ui.components.UniversityAvatar
import com.example.uniportal.ui.components.accentColor
import com.example.uniportal.ui.components.descriptionRes
import com.example.uniportal.ui.components.formatDay
import com.example.uniportal.ui.components.formatFullDay
import com.example.uniportal.ui.components.labelRes
import com.example.uniportal.ui.components.messageRes
import com.example.uniportal.ui.components.priorityLabelRes
import com.example.uniportal.ui.components.rememberCurrentTimeMillis
import com.example.uniportal.ui.components.relativeTimeText
import com.example.uniportal.ui.preview.PreviewData
import com.example.uniportal.ui.theme.UniPortalTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@Composable
fun ApplicationEditorRoute(
    onClose: () -> Unit,
    viewModel: ApplicationEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val completion = (state as? ApplicationEditorUiState.Editing)?.completion
    val currentOnClose by rememberUpdatedState(onClose)
    LaunchedEffect(completion) {
        if (completion != null) currentOnClose()
    }
    ApplicationEditorScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onClose = onClose,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationEditorScreen(
    state: ApplicationEditorUiState,
    onEvent: (ApplicationEditorEvent) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val editing = state as? ApplicationEditorUiState.Editing
    val snackbarHostState = remember { SnackbarHostState() }
    val failedText = stringResource(R.string.error_save_failed)
    val currentOnEvent by rememberUpdatedState(onEvent)
    val operationFailed = editing?.operationFailed == true
    LaunchedEffect(operationFailed) {
        if (!operationFailed) return@LaunchedEffect
        snackbarHostState.showSnackbar(message = failedText, duration = SnackbarDuration.Short)
        currentOnEvent(ApplicationEditorEvent.ErrorShown)
    }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    val needsDiscardConfirm = editing != null && editing.hasChanges && editing.completion == null && !editing.isBusy
    BackHandler(enabled = needsDiscardConfirm) { showDiscardDialog = true }
    if (showDatePicker && editing != null) {
        DeadlinePickerDialog(
            initialDate = editing.form.deadline,
            today = editing.today,
            onConfirm = { date ->
                showDatePicker = false
                onEvent(ApplicationEditorEvent.DeadlineSelected(date))
            },
            onDismiss = { showDatePicker = false },
        )
    }
    if (showDeleteDialog) {
        DeleteApplicationDialog(
            onConfirm = {
                showDeleteDialog = false
                onEvent(ApplicationEditorEvent.Delete)
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
    if (showDiscardDialog) {
        DiscardChangesDialog(
            onConfirm = {
                showDiscardDialog = false
                onClose()
            },
            onDismiss = { showDiscardDialog = false },
        )
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { PortalSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (editing != null) {
                        Text(
                            text = stringResource(
                                if (editing.isNew) R.string.editor_title_new else R.string.editor_title_edit,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (needsDiscardConfirm) showDiscardDialog = true else onClose() }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_close),
                        )
                    }
                },
                actions = {
                    if (editing != null && !editing.isNew) {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            enabled = !editing.isBusy,
                            modifier = Modifier.testTag(TestTags.EDITOR_DELETE_BUTTON),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.cd_delete_application),
                            )
                        }
                    }
                },

                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            if (editing != null) {
                SaveBar(
                    isNew = editing.isNew,
                    enabled = editing.canSave,
                    isSaving = editing.isBusy,
                    onSave = { onEvent(ApplicationEditorEvent.Save) },
                )
            }
        },
    ) { innerPadding ->
        when (state) {
            ApplicationEditorUiState.Loading -> LoadingState(
                modifier = Modifier.padding(innerPadding),
                showAfterMillis = LOADING_SPINNER_DELAY_MS,
            )
            is ApplicationEditorUiState.Error -> ErrorState(
                type = state.type,
                onRetry = { onEvent(ApplicationEditorEvent.Retry) },
                modifier = Modifier.padding(innerPadding),
            )
            is ApplicationEditorUiState.Editing -> EditorForm(
                state = state,
                contentPadding = innerPadding,
                onEvent = onEvent,
                onPickDeadline = { showDatePicker = true },
            )
        }
    }
}

@Composable
private fun EditorForm(
    state: ApplicationEditorUiState.Editing,
    contentPadding: PaddingValues,
    onEvent: (ApplicationEditorEvent) -> Unit,
    onPickDeadline: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()

            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        UniversityHeader(university = state.university)
        StatusSection(
            selected = state.form.status,
            onSelect = { onEvent(ApplicationEditorEvent.StatusSelected(it)) },
        )
        DeadlineSection(
            deadline = state.form.deadline,
            daysLeft = state.daysLeft,
            isOverdue = state.isOverdue,
            isActive = state.form.status.isActive,
            onPick = onPickDeadline,
            onClear = { onEvent(ApplicationEditorEvent.DeadlineSelected(null)) },
        )
        PrioritySection(
            priority = state.form.priority,
            onSelect = { onEvent(ApplicationEditorEvent.PriorityChanged(it)) },
        )
        NoteSection(
            note = state.form.note,
            noteLength = state.noteLength,
            errors = state.errors,
            onNoteChange = { onEvent(ApplicationEditorEvent.NoteChanged(it)) },
        )
        val createdAt = state.createdAt
        val updatedAt = state.updatedAt
        if (createdAt != null && updatedAt != null) {
            val now = rememberCurrentTimeMillis()
            val created = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
            Text(
                text = stringResource(
                    R.string.editor_meta,
                    created.formatDay(state.today),
                    relativeTimeText(updatedAt, now),
                ),
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UniversityHeader(university: University) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UniversityAvatar(id = university.id, name = university.name)
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = university.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (university.country.isNotBlank()) {
                    CountryBadge(country = university.country, countryCode = university.countryCode)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusSection(
    selected: ApplicationStatus,
    onSelect: (ApplicationStatus) -> Unit,
) {
    SectionCard(title = stringResource(R.string.editor_section_status), icon = Icons.Filled.Check) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ApplicationStatus.entries.forEach { status ->
                val isSelected = status == selected
                val accent = status.accentColor()
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(status) },
                    label = { Text(text = stringResource(status.labelRes())) },
                    modifier = Modifier.testTag(TestTags.editorStatus(status)),
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(STATUS_DOT_SIZE)
                                .background(accent, CircleShape),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = accent.copy(alpha = SELECTED_CHIP_ALPHA),
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = MaterialTheme.colorScheme.outline,
                        selectedBorderColor = accent,
                        selectedBorderWidth = 1.dp,
                    ),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(selected.descriptionRes()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DeadlineSection(
    deadline: LocalDate?,
    daysLeft: Int?,
    isOverdue: Boolean,
    isActive: Boolean,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    SectionCard(title = stringResource(R.string.editor_section_deadline), icon = Icons.Filled.DateRange) {
        if (deadline == null) {
            Text(
                text = stringResource(R.string.editor_no_deadline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = deadline.formatFullDay(),
                style = MaterialTheme.typography.titleMedium,
            )
            if (daysLeft != null) {
                Spacer(Modifier.height(8.dp))
                DeadlineChip(daysLeft = daysLeft, isOverdue = isOverdue, isActive = isActive)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onPick,
                modifier = Modifier.testTag(TestTags.EDITOR_DEADLINE_BUTTON),
            ) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(
                    text = stringResource(
                        if (deadline == null) R.string.editor_pick_deadline else R.string.editor_change_deadline,
                    ),
                )
            }
            if (deadline != null) {
                TextButton(
                    onClick = onClear,
                    modifier = Modifier.testTag(TestTags.EDITOR_CLEAR_DEADLINE_BUTTON),
                ) {
                    Text(text = stringResource(R.string.editor_clear_deadline))
                }
            }
        }
    }
}

@Composable
private fun PrioritySection(
    priority: Int,
    onSelect: (Int) -> Unit,
) {
    SectionCard(title = stringResource(R.string.editor_section_priority), icon = Icons.Filled.Star) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ApplicationRules.PRIORITIES.forEach { value ->
                val isSelected = value == priority
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(value) },
                    label = { Text(text = stringResource(priorityLabelRes(value))) },
                    modifier = Modifier.testTag(TestTags.editorPriority(value)),
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Done,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        }
                    } else {
                        null
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = MaterialTheme.colorScheme.outline,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        selectedBorderWidth = 1.dp,
                    ),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.editor_priority_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoteSection(
    note: String,
    noteLength: Int,
    errors: Set<ApplicationError>,
    onNoteChange: (String) -> Unit,
) {
    val tooLong = ApplicationError.NOTE_TOO_LONG in errors
    SectionCard(title = stringResource(R.string.editor_section_note), icon = Icons.Filled.Edit) {
        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.EDITOR_NOTE_FIELD),
            placeholder = { Text(text = stringResource(R.string.editor_note_placeholder)) },
            supportingText = {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (tooLong) stringResource(ApplicationError.NOTE_TOO_LONG.messageRes()) else "",
                        modifier = Modifier.weight(1f),
                    )
                    Text(text = stringResource(R.string.text_counter, noteLength, ApplicationRules.NOTE_MAX_LENGTH))
                }
            },
            isError = tooLong,
            minLines = NOTE_MIN_LINES,
            maxLines = NOTE_MAX_LINES,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
    }
}

@Composable
private fun SaveBar(
    isNew: Boolean,
    enabled: Boolean,
    isSaving: Boolean,
    onSave: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Box(
                modifier = Modifier

                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Button(
                    onClick = onSave,
                    enabled = enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.EDITOR_SAVE_BUTTON),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                            color = LocalContentColor.current,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                    }
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(if (isNew) R.string.editor_create else R.string.editor_save))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeadlinePickerDialog(
    initialDate: LocalDate?,
    today: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.toUtcMillis(),
        initialDisplayedMonthMillis = (initialDate ?: today).toUtcMillis(),
    )
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { pickerState.selectedDateMillis?.let { millis -> onConfirm(millis.toUtcLocalDate()) } },
                enabled = pickerState.selectedDateMillis != null,
            ) {
                Text(text = stringResource(R.string.action_choose))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
        colors = DatePickerDefaults.colors(containerColor = containerColor),
    ) {
        DatePicker(
            state = pickerState,
            colors = DatePickerDefaults.colors(containerColor = containerColor),
        )
    }
}

@Composable
private fun DeleteApplicationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Filled.Delete, contentDescription = null) },
        title = { Text(text = stringResource(R.string.editor_delete_title)) },
        text = { Text(text = stringResource(R.string.editor_delete_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(TestTags.CONFIRM_DELETE_BUTTON),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(text = stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        iconContentColor = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun DiscardChangesDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.editor_discard_title)) },
        text = { Text(text = stringResource(R.string.editor_discard_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(text = stringResource(R.string.editor_discard_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.editor_discard_stay))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private const val LOADING_SPINNER_DELAY_MS = 300L
private const val SELECTED_CHIP_ALPHA = 0.18f
private const val NOTE_MIN_LINES = 4
private const val NOTE_MAX_LINES = 10
private val STATUS_DOT_SIZE = 8.dp

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14, heightDp = 1200)
@Composable
private fun ApplicationEditorScreenPreview() {
    val today = LocalDate.now()
    UniPortalTheme {
        ApplicationEditorScreen(
            state = ApplicationEditorUiState.Editing(
                university = PreviewData.mtsu,
                form = ApplicationForm(
                    status = ApplicationStatus.PREPARING,
                    deadline = today.plusDays(5),
                    priority = 3,
                    note = PreviewData.application.note,
                ),
                isNew = false,
                errors = emptySet(),
                daysLeft = 5,
                isOverdue = false,
                today = today,
                hasChanges = true,
                isBusy = false,
                createdAt = PreviewData.application.createdAt,
                updatedAt = PreviewData.application.updatedAt,
            ),
            onEvent = {},
            onClose = {},
        )
    }
}
