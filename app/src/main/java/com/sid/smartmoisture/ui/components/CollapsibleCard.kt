package com.sid.smartmoisture.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sid.smartmoisture.R

@Composable
fun CollapsibleCard(
    modifier: Modifier = Modifier, title: @Composable () -> Unit = {
        Text(
            "Details", style = MaterialTheme.typography.titleMedium
        )
    }, initiallyExpanded: Boolean = false, content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }

    ElevatedCard(modifier = modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) { title() }
                Icon(
                    painter = if (expanded) painterResource(id = R.drawable.expand_less)
                    else painterResource(id = R.drawable.expand_more),
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.height(24.dp)
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = content
                )
            }
        }
    }
}

@Preview
@Composable
private fun CollapsibleCardPreview() {
    CollapsibleCard(
        title = { Text("Collapsible Card", style = MaterialTheme.typography.titleMedium) },
        initiallyExpanded = true
    ) {
        Text(
            "This is some sample content inside the collapsible card. Click the header to expand or collapse.",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "You can put anything you want here.", style = MaterialTheme.typography.bodyMedium
        )
    }
}
