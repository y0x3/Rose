package com.tiempo.app.presentation.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiempo.app.R
import com.tiempo.app.domain.model.ProfileValidationError
import com.tiempo.app.domain.model.UserProfileDefaults
import com.tiempo.app.presentation.common.formatAsLongDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.roundToInt

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 3.sp
        )

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = stringResource(R.string.onboarding_title),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.onboarding_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(40.dp))

        BirthDateSelector(
            birthDate = uiState.birthDate,
            onClick = { showDatePicker = true }
        )

        Spacer(modifier = Modifier.height(28.dp))

        ReferenceAgeSelector(
            referenceAgeYears = uiState.referenceAgeYears,
            onReferenceAgeChanged = viewModel::onReferenceAgeChanged
        )

        Spacer(modifier = Modifier.height(20.dp))

        EstimateNotice()

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = viewModel::saveProfile,
            enabled = uiState.birthDate != null && !uiState.isSaving,
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = stringResource(
                    if (uiState.isSaving) R.string.onboarding_saving else R.string.onboarding_submit
                ),
                style = MaterialTheme.typography.labelLarge
            )
        }

        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(error.messageRes()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showDatePicker) {
        BirthDatePickerDialog(
            today = viewModel.today,
            initialDate = uiState.birthDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = viewModel::onBirthDateSelected
        )
    }
}

@Composable
private fun BirthDateSelector(
    birthDate: LocalDate?,
    onClick: () -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.onboarding_birth_date_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = birthDate?.formatAsLongDate()
                        ?: stringResource(R.string.onboarding_birth_date_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (birthDate == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    },
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = stringResource(R.string.cd_pick_birth_date),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ReferenceAgeSelector(
    referenceAgeYears: Int,
    onReferenceAgeChanged: (Int) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.onboarding_reference_age_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = referenceAgeYears.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.onboarding_reference_age_unit),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Slider(
                value = referenceAgeYears.toFloat(),
                onValueChange = { onReferenceAgeChanged(it.roundToInt()) },
                valueRange = UserProfileDefaults.REFERENCE_AGE_MIN_YEARS.toFloat()..
                    UserProfileDefaults.REFERENCE_AGE_MAX_YEARS.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            Text(
                text = stringResource(
                    R.string.onboarding_reference_age_range,
                    UserProfileDefaults.REFERENCE_AGE_MIN_YEARS,
                    UserProfileDefaults.REFERENCE_AGE_MAX_YEARS
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.onboarding_reference_age_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EstimateNotice() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.onboarding_estimate_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@StringRes
private fun OnboardingError.messageRes(): Int = when (this) {
    OnboardingError.Unknown -> R.string.error_unknown
    is OnboardingError.Validation -> error.messageRes()
}

@StringRes
private fun ProfileValidationError.messageRes(): Int = when (this) {
    ProfileValidationError.BIRTH_DATE_MISSING -> R.string.error_birth_date_missing
    ProfileValidationError.BIRTH_DATE_IN_FUTURE -> R.string.error_birth_date_in_future
    ProfileValidationError.BIRTH_DATE_TOO_OLD -> R.string.error_birth_date_too_old
    ProfileValidationError.REFERENCE_AGE_OUT_OF_RANGE -> R.string.error_reference_age_out_of_range
    ProfileValidationError.REFERENCE_AGE_NOT_ABOVE_CURRENT_AGE ->
        R.string.error_reference_age_not_above_current_age
    ProfileValidationError.SLEEP_WINDOW_INVALID -> R.string.error_sleep_window_invalid
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDatePickerDialog(
    today: LocalDate,
    initialDate: LocalDate?,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val todayMillis = today.toUtcMillis()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.toUtcMillis(),
        initialDisplayedMonthMillis = (initialDate ?: today.minusYears(30)).toUtcMillis(),
        yearRange = UserProfileDefaults.MIN_BIRTH_DATE.year..today.year,
        selectableDates = remember(todayMillis) {
            BirthDateSelectableDates(
                minMillis = UserProfileDefaults.MIN_BIRTH_DATE.toUtcMillis(),
                maxMillis = todayMillis,
                minYear = UserProfileDefaults.MIN_BIRTH_DATE.year,
                maxYear = today.year
            )
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(millis.toUtcLocalDate())
                    }
                    onDismiss()
                },
                enabled = datePickerState.selectedDateMillis != null
            ) {
                Text(text = stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = false,
            title = {
                Text(
                    text = stringResource(R.string.onboarding_date_picker_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                )
            },
            headline = {
                Text(
                    text = datePickerState.selectedDateMillis?.toUtcLocalDate()?.formatAsLongDate()
                        ?: stringResource(R.string.onboarding_birth_date_placeholder),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private class BirthDateSelectableDates(
    private val minMillis: Long,
    private val maxMillis: Long,
    private val minYear: Int,
    private val maxYear: Int
) : SelectableDates {

    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        utcTimeMillis in minMillis..maxMillis

    override fun isSelectableYear(year: Int): Boolean = year in minYear..maxYear
}

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
