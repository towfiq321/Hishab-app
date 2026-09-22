@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.hishab.finance.ui.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hishab.finance.data.local.entity.PayFrequency
import com.hishab.finance.ui.components.SectionHeader
import com.hishab.finance.ui.rememberViewModel

/** Add or edit an income source, with the contact block the brief asks for. */
@Composable
fun SourceEditScreen(sourceId: Long, onDone: () -> Unit) {
    val vm: SourceEditViewModel = rememberViewModel(key = "source-edit-$sourceId") {
        SourceEditViewModel(it, sourceId)
    }
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Column(Modifier.fillMaxSize().imePadding()) {
        TopAppBar(
            title = { Text(if (state.isEditing) "Edit source" else "New income source") },
            navigationIcon = {
                IconButton(onClick = onDone) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = vm::setName,
                label = { Text("Source name") },
                placeholder = { Text("Solar Projects") },
                isError = state.error != null,
                supportingText = state.error?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(18.dp))
            SectionHeader("Source type")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SourceEditViewModel.SOURCE_TYPES.forEach { type ->
                    FilterChip(
                        selected = state.sourceType == type,
                        onClick = { vm.setType(type) },
                        label = { Text(type) }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionHeader("Expected payment frequency")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PayFrequency.entries.forEach { frequency ->
                    FilterChip(
                        selected = state.payFrequency == frequency,
                        onClick = { vm.setFrequency(frequency) },
                        label = { Text(frequency.label) }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = state.typicalAmountText,
                onValueChange = vm::setTypicalAmount,
                label = { Text("Typical income amount") },
                prefix = { Text(com.hishab.finance.core.TAKA) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(18.dp))
            SectionHeader("Icon and colour")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SourceEditViewModel.ICONS.forEach { icon ->
                    FilterChip(
                        selected = state.icon == icon,
                        onClick = { vm.setIcon(icon) },
                        label = { Text(icon) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SourceEditViewModel.COLORS.forEach { argb ->
                    val selected = state.colorArgb == argb
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .border(
                                width = if (selected) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape
                            )
                            .clickable { vm.setColor(argb) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader("Contact", subtitle = "Optional, but handy when chasing a payment")
            OutlinedTextField(
                value = state.contactName,
                onValueChange = vm::setContactName,
                label = { Text("Person or company") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.contactPhone,
                onValueChange = vm::setPhone,
                label = { Text("Phone") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.email,
                onValueChange = vm::setEmail,
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.address,
                onValueChange = vm::setAddress,
                label = { Text("Address") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = state.description,
                onValueChange = vm::setDescription,
                label = { Text("Description") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.notes,
                onValueChange = vm::setNotes,
                label = { Text("Notes") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
        }

        Box(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = vm::save,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (state.isEditing) "Save changes" else "Add source",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
