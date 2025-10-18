package com.sid.smartmoisture.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sid.smartmoisture.R
import com.sid.smartmoisture.core.Equation
import com.sid.smartmoisture.ui.components.CollapsibleCard
import com.sid.smartmoisture.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquationEditScreen(vm: MainViewModel, onClose: () -> Unit, existing: Equation? = null) {
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(TextFieldValue("")) }
    var formula by remember { mutableStateOf(TextFieldValue("x")) }
    var formulaError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(existing?.id) {
        if (existing != null) {
            name = TextFieldValue(existing.name)
            formula = TextFieldValue(existing.formula)
        }
    }
    LaunchedEffect(formula.text) {
        formulaError = vm.validateFormula(formula.text.trim(), 1.5, 1.0, 0.5).second
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing != null) "Edit Equation" else "Add Equation") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            painter = painterResource(id = R.drawable.nav_arrow),
                            contentDescription = "Back",
                            modifier = Modifier.height(24.dp)
                        )
                    }
                })
        }) { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                isError = name.text.isBlank(),
                supportingText = {
                    if (name.text.isBlank()) Text(
                        "Name cannot be empty.", color = MaterialTheme.colorScheme.error
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = formula,
                onValueChange = { formula = it },
                label = { Text("Formula") },
                isError = formulaError != null,
                supportingText = {
                    if (formula.text.isBlank()) Text(
                        "Formula cannot be empty.", color = MaterialTheme.colorScheme.error
                    ) else if (formulaError != null) Text(
                        formulaError!!, color = MaterialTheme.colorScheme.error
                    ) else Text("Uses variables: x (current), xp (previous), dx (x - xp)")
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    enabled = formulaError == null && name.text.isNotBlank() && formula.text.isNotBlank(),
                    onClick = {
                        scope.launch {
                            formulaError =
                                vm.validateFormula(formula.text.trim(), 1.5, 1.0, 0.5).second
                            if (formulaError == null) {
                                if (existing != null) vm.updateEquation(
                                    existing.id,
                                    name.text.trim(),
                                    formula.text.trim(),
                                    select = true
                                ) else vm.addEquation(
                                    name.text.trim(), formula.text.trim(), select = true
                                )
                                onClose()
                            }
                        }
                    }) { Text(if (existing != null) "Update" else "Save") }
                OutlinedButton(onClick = onClose) { Text("Cancel") }
            }
            CollapsibleCard(
                modifier = Modifier.fillMaxWidth(),
                title = { Text("Tips", style = MaterialTheme.typography.titleMedium) }) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Equation examples:", style = MaterialTheme.typography.bodySmall)
                    listOf(
                        "x * 0.01",
                        "(x - 128) / 256 * 100",
                        "0.8 * xp + 0.2 * x",
                        "min(max(x/340, 0), 100)"
                    ).forEach { example ->
                        Text(
                            text = example,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            CollapsibleCard(
                modifier = Modifier.fillMaxWidth(),
                title = { Text("Syntax", style = MaterialTheme.typography.titleMedium) },
                initiallyExpanded = true
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Operators: + - * / ^ ( )", style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Functions: sin, cos, tan, log, ln, sqrt, abs, min, max, etc.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text("Variables:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "  x = current raw sensor value", style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "  xp = previous raw sensor value",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text("  dx = difference (x - xp)", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Preview
@Composable
private fun EquationEditScreenPreview() {
    EquationEditScreen(vm = MainViewModel.preview(), onClose = {})
}
