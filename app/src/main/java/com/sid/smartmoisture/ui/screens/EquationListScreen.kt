package com.sid.smartmoisture.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sid.smartmoisture.R
import com.sid.smartmoisture.core.Equation
import com.sid.smartmoisture.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquationListScreen(
    vm: MainViewModel, onAdd: () -> Unit, onEdit: (Equation) -> Unit, onBack: () -> Unit
) {
    val items by vm.equations.collectAsState()
    val selected by vm.selected.collectAsState()

    Scaffold(topBar = {
        TopAppBar(title = { Text("Equations") }, navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(id = R.drawable.nav_arrow),
                    contentDescription = "Back",
                    modifier = Modifier.height(24.dp)
                )
            }
        })
    }, floatingActionButton = {
        FloatingActionButton(onClick = onAdd) {
            Icon(
                painter = painterResource(id = R.drawable.plus),
                contentDescription = "Delete",
                modifier = Modifier.height(24.dp)
            )
        }
    }) { pad ->
        LazyColumn(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items, key = { it.id }) { e ->
                EquationRow(
                    e = e,
                    selected = selected?.id == e.id,
                    onSelect = { vm.setSelected(e) },
                    onEdit = { onEdit(e) },
                    onDelete = { vm.removeEquation(e) })
            }
        }
        if (items.isEmpty()) Text(
            "No equations added yet. Click the + button to add one!",
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EquationRow(
    e: Equation, selected: Boolean, onSelect: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) AlertDialog(
        onDismissRequest = { showDialog = false },
        title = { Text("Delete Equation") },
        text = { Text("Are you sure you want to delete \"${e.name}\"?") },
        confirmButton = {
            FilledTonalButton(onClick = {
                showDialog = false
                onDelete()
            }) { Text("Delete") }
        },
        dismissButton = {
            OutlinedButton(onClick = { showDialog = false }) { Text("Cancel") }
        })

    ElevatedCard {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(e.name, style = MaterialTheme.typography.titleMedium)
                Text(e.formula, style = MaterialTheme.typography.bodyMedium)
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.End
            ) {
                if (!selected) OutlinedButton(onClick = onSelect) { Text("Use") }
                if (selected) FilledTonalButton(onClick = {}, enabled = false) { Text("Selected") }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            painter = painterResource(id = R.drawable.edit_pencil),
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.height(24.dp)
                        )
                    }
                    IconButton(onClick = { showDialog = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.trash),
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun EquationListScreenPreview() {
    EquationListScreen(vm = MainViewModel.preview(), onAdd = {}, onEdit = {}, onBack = {})
}
