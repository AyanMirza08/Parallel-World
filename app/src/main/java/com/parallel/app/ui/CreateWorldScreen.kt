package com.parallel.app.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parallel.app.domain.LocalWorldGenerator
import com.parallel.app.domain.ParallelWorld
import com.parallel.app.ui.theme.ParallelPalette
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CreateWorldScreen(onBack: () -> Unit, onGenerate: suspend (ParallelWorld) -> Unit) {
    var location by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var premise by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }
    var generationError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val locationError = if (submitted && location.isBlank()) "Enter a location or city." else null
    val countryError = if (submitted && country.isBlank()) "Enter a country." else null
    val yearNumber = year.toIntOrNull()
    val yearError = if (year.isNotBlank() && (yearNumber == null || yearNumber !in 1..9999)) {
        "Enter a year between 1 and 9999."
    } else null
    val formIsValid = location.isNotBlank() && country.isNotBlank() && yearError == null

    AdaptivePage {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, enabled = !isGenerating) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color(0xFFE4E6ED))
            }
            Text("CREATE A WORLD", color = ParallelPalette.TextSecondary, style = MaterialTheme.typography.labelMedium)
        }

        Spacer(Modifier.height(23.dp))
        Text(
            "Start with a place.",
            color = ParallelPalette.TextPrimary,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(9.dp))
        Text(
            "Choose where your alternate story begins, then give history a different turn.",
            color = ParallelPalette.TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(28.dp))

        WorldInputField(
            label = "LOCATION / CITY",
            value = location,
            onValueChange = { location = it },
            placeholder = "e.g. Tokyo",
            error = locationError,
            enabled = !isGenerating
        )
        Spacer(Modifier.height(18.dp))
        WorldInputField(
            label = "COUNTRY",
            value = country,
            onValueChange = { country = it },
            placeholder = "e.g. Japan",
            error = countryError,
            enabled = !isGenerating
        )
        Spacer(Modifier.height(18.dp))
        WorldInputField(
            label = "YEAR · OPTIONAL",
            value = year,
            onValueChange = { year = it.filter(Char::isDigit).take(4) },
            placeholder = "e.g. 2026",
            error = yearError,
            enabled = !isGenerating,
            keyboardType = KeyboardType.Number
        )
        Spacer(Modifier.height(18.dp))
        WorldInputField(
            label = "WHAT IF? · OPTIONAL",
            value = premise,
            onValueChange = { premise = it },
            placeholder = "Tokyo never became one of the world's major railway hubs.",
            enabled = !isGenerating,
            minLines = 3
        )

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = {
                submitted = true
                if (formIsValid) {
                    isGenerating = true
                    generationError = null
                    scope.launch {
                        try {
                            delay(320)
                            val world = withContext(Dispatchers.Default) {
                                LocalWorldGenerator.generate(location, country, year.takeIf { it.isNotBlank() }, premise)
                            }
                            onGenerate(world)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            generationError = "Parallel couldn't save this world. Check available storage and try again."
                            isGenerating = false
                        }
                    }
                }
            },
            enabled = !isGenerating,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ParallelPalette.TextPrimary,
                contentColor = Color(0xFF11131A),
                disabledContainerColor = Color(0xFF414550),
                disabledContentColor = Color(0xFF999EAB)
            )
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF242731), strokeWidth = 2.dp)
                Spacer(Modifier.size(10.dp))
                Text("BUILDING YOUR WORLD", style = MaterialTheme.typography.labelLarge)
            } else {
                Text("GENERATE WORLD", style = MaterialTheme.typography.labelLarge)
            }
        }
        generationError?.let { error ->
            Spacer(Modifier.height(12.dp))
            Text(
                error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                color = ParallelPalette.Error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "OFFLINE WORLD BUILDING  ·  SAVED ON THIS DEVICE",
            modifier = Modifier.fillMaxWidth(),
            color = ParallelPalette.TextMuted,
            style = MaterialTheme.typography.labelSmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun WorldInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: String? = null,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = minLines == 1,
        minLines = minLines,
        isError = error != null,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        placeholder = { Text(placeholder, color = Color(0xFF777E8D), style = MaterialTheme.typography.bodySmall) },
        keyboardOptions = KeyboardOptions(
            capitalization = if (keyboardType == KeyboardType.Text) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
            keyboardType = keyboardType,
            imeAction = if (minLines > 1) ImeAction.Default else ImeAction.Next
        ),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
        shape = RoundedCornerShape(10.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ParallelPalette.Accent,
            unfocusedBorderColor = ParallelPalette.Outline,
            focusedTextColor = ParallelPalette.TextPrimary,
            unfocusedTextColor = ParallelPalette.TextPrimary,
            cursorColor = ParallelPalette.Accent,
            focusedContainerColor = ParallelPalette.Surface,
            unfocusedContainerColor = ParallelPalette.Surface,
            errorBorderColor = ParallelPalette.Error,
            errorSupportingTextColor = ParallelPalette.Error
        ),
        supportingText = if (error != null) {{ Text(error, style = MaterialTheme.typography.bodySmall) }} else null
    )
}
