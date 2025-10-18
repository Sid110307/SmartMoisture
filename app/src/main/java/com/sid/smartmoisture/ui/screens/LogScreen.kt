package com.sid.smartmoisture.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sid.smartmoisture.R
import com.sid.smartmoisture.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(vm: MainViewModel, onBack: () -> Unit) {
    val log by vm.log.collectAsState()
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Bluetooth Log") }, navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(id = R.drawable.nav_arrow),
                        contentDescription = "Back",
                        modifier = Modifier.height(24.dp)
                    )
                }
            })
        }) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(12.dp), state = listState
        ) {
            itemsIndexed(
                items = log, key = { _, item -> item.id }) { idx, item ->
                val formattedTime = remember(item.time) {
                    DateFormat.format("dd-MM-yyyy HH:mm:ss.SSS", item.time).toString()
                }

                ListItem(headlineContent = {
                    Text(item.text)
                }, supportingContent = {
                    Text(formattedTime)
                }, trailingContent = {
                    Text("#${idx + 1}")
                })
                HorizontalDivider(Modifier.padding(vertical = 2.dp))
            }
        }
        if (log.isEmpty()) Text(
            "No logs available",
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
fun LogScreenPreview() {
    LogScreen(vm = MainViewModel.preview(), onBack = {})
}
