@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalTextApi::class,
    ExperimentalFoundationApi::class
)

package com.nukirk.medrx.elements.MainActivity

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.nukirk.medrx.ExpressiveTextButton
import com.nukirk.medrx.R
import com.nukirk.medrx.TimeSelectorItem
import com.nukirk.medrx.elements.TimePicker
import com.nukirk.medrx.services.MedData
import com.nukirk.medrx.services.SymptomSeverity
import com.nukirk.medrx.ui.theme.GoogleSansFlex
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Create or edit a symptom entry: title, editable date AND time (symptoms can
 * be logged after the fact), severity, and an optional note.
 */
@Composable
fun SymptomBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?, LocalDate, LocalTime, SymptomSeverity?, String?) -> Unit,
    initialItem: MedData? = null,
    initialText: String = ""
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var text by remember { mutableStateOf(initialItem?.title ?: initialText) }
    var notes by remember { mutableStateOf(initialItem?.notes ?: "") }
    var nameError by remember { mutableStateOf(false) }
    var selectedSeverity by remember {
        mutableStateOf(initialItem?.symptomSeverity ?: SymptomSeverity.MILD)
    }

    var selectedDate by remember { mutableStateOf(initialItem?.creationDate ?: LocalDate.now()) }
    var selectedTime by remember {
        mutableStateOf(initialItem?.creationTime ?: LocalTime.now())
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState =
            rememberDatePickerState(initialSelectedDateMillis = selectedDate.toEpochDay() * 24 * 60 * 60 * 1000)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                ExpressiveTextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                    }; showDatePicker = false
                }, text = stringResource(R.string.ok_action))
            },
            dismissButton = {
                ExpressiveTextButton(
                    onClick = { showDatePicker = false },
                    text = stringResource(R.string.cancel_action)
                )
            },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = RoundedCornerShape(32.dp),
            tonalElevation = 6.dp
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        TimePicker(
            onDismiss = { showTimePicker = false },
            onConfirm = { newTime ->
                selectedTime = newTime
                showTimePicker = false
            },
            initialTime = selectedTime
        )
    }

    val cancelInteractionSource = remember { MutableInteractionSource() }
    val saveInteractionSource = remember { MutableInteractionSource() }
    val isCancelPressed by cancelInteractionSource.collectIsPressedAsState()
    val isSavePressed by saveInteractionSource.collectIsPressedAsState()

    val cancelCorner by animateIntAsState(
        targetValue = if (isCancelPressed) 15 else 50,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cancelCorner"
    )
    val saveCorner by animateIntAsState(
        targetValue = if (isSavePressed) 15 else 50,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "saveCorner"
    )

    val listState = rememberLazyListState()
    var wasAtTopWhenGestureStarted by remember { mutableStateOf(true) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                return if (!wasAtTopWhenGestureStarted) available else Velocity.Zero
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.statusBarsPadding(),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitFirstDown(requireUnconsumed = false)
                                wasAtTopWhenGestureStarted = !listState.canScrollBackward
                            }
                        }
                    }
                    .nestedScroll(nestedScrollConnection),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                item {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Psychology,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                item {
                    OutlinedTextField(
                        value = text,
                        onValueChange = {
                            text = it
                            nameError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                stringResource(R.string.symptom_title_hint),
                                fontFamily = GoogleSansFlex
                            )
                        },
                        singleLine = true,
                        isError = nameError
                    )
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                stringResource(R.string.symptom_notes_hint),
                                fontFamily = GoogleSansFlex
                            )
                        },
                        minLines = 2,
                        maxLines = 4
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
                    // Severity as a three-way segmented control: Mild / Moderate / Severe.
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val severities = listOf(
                            SymptomSeverity.MILD to R.string.severity_mild,
                            SymptomSeverity.MODERATE to R.string.severity_moderate,
                            SymptomSeverity.SEVERE to R.string.severity_severe
                        )
                        severities.forEachIndexed { index, (severity, labelRes) ->
                            SegmentedButton(
                                selected = selectedSeverity == severity,
                                onClick = { selectedSeverity = severity },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = severities.size
                                )
                            ) {
                                Text(
                                    stringResource(labelRes),
                                    fontFamily = GoogleSansFlex,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selectedSeverity == severity) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
                    DateSelectorItem(
                        label = stringResource(R.string.symptom_date_label),
                        date = selectedDate
                    ) { showDatePicker = true }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }

                item {
                    TimeSelectorItem(
                        label = stringResource(R.string.symptom_time_label),
                        time = selectedTime
                    ) { showTimePicker = true }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }

                item {
                    Surface(
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                        if (!sheetState.isVisible) {
                                            onDismiss()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(cancelCorner),
                                interactionSource = cancelInteractionSource
                            ) {
                                Text(
                                    stringResource(R.string.cancel_action),
                                    fontFamily = GoogleSansFlex,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1
                                )
                            }

                            Button(
                                onClick = {
                                    if (text.isNotBlank()) {
                                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                                            if (!sheetState.isVisible) {
                                                onConfirm(
                                                    text,
                                                    "Psychology",
                                                    "dynamic",
                                                    selectedDate,
                                                    selectedTime,
                                                    selectedSeverity,
                                                    notes.takeIf { it.isNotBlank() }
                                                )
                                            }
                                        }
                                    } else {
                                        nameError = true
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(saveCorner),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                interactionSource = saveInteractionSource
                            ) {
                                Text(
                                    stringResource(if (initialItem == null) R.string.log_symptom_action else R.string.save_action),
                                    fontFamily = GoogleSansFlex,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}
