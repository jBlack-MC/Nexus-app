package com.example.nexus.ui.tasks

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.nexus.api.ChecklistItem
import com.example.nexus.api.Task
import com.example.nexus.api.TaskPriority
import com.example.nexus.api.TaskStatus
import com.example.nexus.ui.components.NexusPrimaryButton
import com.example.nexus.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Material's date picker works in UTC milliseconds, so dates are formatted in UTC to preserve the
 * exact day the user tapped. "Today", however, must be the device's local date so the overdue
 * comparison agrees with DashboardScreen.
 */
private val utcIsoFormatter: SimpleDateFormat =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
private val localIsoFormatter: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

internal fun currentIsoDate(): String = localIsoFormatter.format(Date())

internal fun millisToIsoDate(millis: Long): String = utcIsoFormatter.format(Date(millis))

internal fun isoDateToMillis(date: String?): Long? =
    date?.takeIf { it.isNotBlank() }?.let { runCatching { utcIsoFormatter.parse(it)?.time }.getOrNull() }

/** Overdue means still open and due before the device's local today. */
internal fun isTaskOverdue(
    dueDate: String?,
    isCompleted: Boolean,
): Boolean = !isCompleted && dueDate != null && dueDate < currentIsoDate()

internal fun TaskPriority.displayName(): String =
    when (this) {
        TaskPriority.NONE -> "None"
        TaskPriority.LOW -> "Low"
        TaskPriority.MEDIUM -> "Medium"
        TaskPriority.HIGH -> "High"
    }

internal fun TaskStatus.displayName(): String =
    when (this) {
        TaskStatus.TODO -> "To do"
        TaskStatus.IN_PROGRESS -> "In progress"
        TaskStatus.DONE -> "Done"
    }

/** A horizontally scrollable row of single-choice chips. Every option carries a text label. */
@Composable
private fun <T> ChipSelector(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.smCompact)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(label(option)) },
                )
            }
        }
    }
}

/** Shows the chosen due date, or a hint, with an X to clear it. */
@Composable
private fun DueDateField(
    dueDate: String?,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.smCompact)) {
        Text("Due date", style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            OutlinedButton(onClick = onPick, modifier = Modifier.weight(1f)) {
                Text(dueDate ?: "Pick a date")
            }
            if (dueDate != null) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, contentDescription = "Clear due date")
                }
            }
        }
    }
}

/**
 * Creates a new task when [task] is null, or edits an existing one. Saving hands the caller a
 * [TaskDraft]; the caller decides whether that becomes a create or an update.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorDialog(
    task: Task? = null,
    onDismiss: () -> Unit,
    onSave: (TaskDraft) -> Unit,
) {
    val key = task?.id
    var title by remember(key) { mutableStateOf(task?.title.orEmpty()) }
    var description by remember(key) { mutableStateOf(task?.description.orEmpty()) }
    var dueDate by remember(key) { mutableStateOf(task?.dueDate) }
    var priority by remember(key) { mutableStateOf(task?.priority ?: TaskPriority.NONE) }
    var status by remember(key) { mutableStateOf(task?.status ?: TaskStatus.TODO) }
    var labels by remember(key) { mutableStateOf(task?.labels.orEmpty()) }
    var checklist by remember(key) { mutableStateOf(task?.checklist.orEmpty()) }

    var labelInput by remember(key) { mutableStateOf("") }
    var stepInput by remember(key) { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier =
                    Modifier
                        .heightIn(max = 560.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(Spacing.xlCompact),
                verticalArrangement = Arrangement.spacedBy(Spacing.smd),
            ) {
                Text(
                    text = if (task == null) "New task" else "Edit task",
                    style = MaterialTheme.typography.titleLarge,
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )

                DueDateField(
                    dueDate = dueDate,
                    onPick = { showDatePicker = true },
                    onClear = { dueDate = null },
                )

                ChipSelector(
                    title = "Priority",
                    options = TaskPriority.entries,
                    selected = priority,
                    label = { it.displayName() },
                    onSelect = { priority = it },
                )

                ChipSelector(
                    title = "Status",
                    options = TaskStatus.entries,
                    selected = status,
                    label = { it.displayName() },
                    onSelect = { status = it },
                )

                LabelsEditor(
                    labels = labels,
                    input = labelInput,
                    onInputChange = { labelInput = it },
                    onAdd = {
                        val value = labelInput.trim()
                        if (value.isNotEmpty() && labels.none { it.equals(value, ignoreCase = true) }) {
                            labels = labels + value
                        }
                        labelInput = ""
                    },
                    onRemove = { removed -> labels = labels.filterNot { it == removed } },
                )

                ChecklistEditor(
                    items = checklist,
                    input = stepInput,
                    onInputChange = { stepInput = it },
                    onAdd = {
                        val value = stepInput.trim()
                        if (value.isNotEmpty()) {
                            checklist = checklist +
                                ChecklistItem(
                                    id = UUID.randomUUID().toString(),
                                    title = value,
                                )
                        }
                        stepInput = ""
                    },
                    onToggle = { toggled ->
                        checklist =
                            checklist.map {
                                if (it.id == toggled.id) it.copy(isCompleted = !it.isCompleted) else it
                            }
                    },
                    onRemove = { removed -> checklist = checklist.filterNot { it.id == removed.id } },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    NexusPrimaryButton(
                        text = if (task == null) "Create" else "Save",
                        onClick = {
                            onSave(
                                TaskDraft(
                                    title = title.trim(),
                                    description = description.trim().ifBlank { null },
                                    dueDate = dueDate,
                                    priority = priority,
                                    status = status,
                                    labels = labels,
                                    checklist = checklist,
                                ),
                            )
                        },
                        enabled = title.isNotBlank(),
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = isoDateToMillis(dueDate))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dueDate = millisToIsoDate(it) }
                        showDatePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/** Free-form labels: type a name, add it, and tap a chip to remove it. */
@Composable
private fun LabelsEditor(
    labels: List<String>,
    input: String,
    onInputChange: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.smCompact)) {
        Text("Labels", style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                label = { Text("Add label") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            NexusPrimaryButton(text = "Add", onClick = onAdd, enabled = input.isNotBlank())
        }
        if (labels.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.smCompact),
            ) {
                labels.forEach { label ->
                    InputChip(
                        selected = false,
                        onClick = { onRemove(label) },
                        label = { Text(label) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove label $label",
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                }
            }
        }
    }
}

/** Checklist steps: add a step, tick it off, or remove it. */
@Composable
private fun ChecklistEditor(
    items: List<ChecklistItem>,
    input: String,
    onInputChange: (String) -> Unit,
    onAdd: () -> Unit,
    onToggle: (ChecklistItem) -> Unit,
    onRemove: (ChecklistItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.smCompact)) {
        Text("Checklist", style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                label = { Text("Add step") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            NexusPrimaryButton(text = "Add", onClick = onAdd, enabled = input.isNotBlank())
        }
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Checkbox(checked = item.isCompleted, onCheckedChange = { onToggle(item) })
                Text(
                    text = item.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                )
                IconButton(onClick = { onRemove(item) }) {
                    Icon(Icons.Default.Close, contentDescription = "Remove step ${item.title}")
                }
            }
        }
    }
}
