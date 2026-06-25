package com.smartorders.driverhelper.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.smartorders.driverhelper.AppState
import com.smartorders.driverhelper.data.AppPreferences
import com.smartorders.driverhelper.ui.*

@Composable
fun RulesScreen() {
    val context = LocalContext.current
    var minPrice by remember { mutableStateOf(AppPreferences.getMinPrice(context).toString()) }
    var maxPrice by remember { mutableStateOf(AppPreferences.getMaxPrice(context).toString()) }
    var minPickupMinutes by remember { mutableStateOf(AppPreferences.getMinPickupMinutes(context).toString()) }
    var maxPickupMinutes by remember { mutableStateOf(AppPreferences.getMaxPickupMinutes(context).toString()) }
    var maxPickupDistance by remember { mutableStateOf(AppPreferences.getMaxPickupDistance(context).toString()) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Auto-Accept Rules",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Trips are accepted only when ALL conditions match",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )

        // Target App Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A3A)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Purple60)
                Column {
                    Text("Target App", color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Text("Jeeny Driver", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        "com.jeeny.driver / com.jeeny.drivers",
                        color = OnSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        HorizontalDivider(color = SurfaceVariant)

        // Price Rules
        Text(
            text = "Price Rules (﷼ SAR)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Purple60
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RuleTextField(
                modifier = Modifier.weight(1f),
                label = "Min Price ﷼",
                value = minPrice,
                onValueChange = { minPrice = it; saved = false },
                icon = Icons.Default.ArrowUpward
            )
            RuleTextField(
                modifier = Modifier.weight(1f),
                label = "Max Price ﷼",
                value = maxPrice,
                onValueChange = { maxPrice = it; saved = false },
                icon = Icons.Default.ArrowDownward
            )
        }

        // Pickup Minutes Rules
        Text(
            text = "Pickup Time Rules (minutes)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Purple60
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RuleTextField(
                modifier = Modifier.weight(1f),
                label = "Min Minutes",
                value = minPickupMinutes,
                onValueChange = { minPickupMinutes = it; saved = false },
                icon = Icons.Default.Schedule
            )
            RuleTextField(
                modifier = Modifier.weight(1f),
                label = "Max Minutes",
                value = maxPickupMinutes,
                onValueChange = { maxPickupMinutes = it; saved = false },
                icon = Icons.Default.Schedule
            )
        }

        // Pickup Distance Rule
        Text(
            text = "Pickup Distance (km)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Purple60
        )
        RuleTextField(
            modifier = Modifier.fillMaxWidth(),
            label = "Max Pickup Distance (km)",
            value = maxPickupDistance,
            onValueChange = { maxPickupDistance = it; saved = false },
            icon = Icons.Default.NearMe
        )

        // Save Button
        Button(
            onClick = {
                AppPreferences.setMinPrice(context, minPrice.toFloatOrNull() ?: 0f)
                AppPreferences.setMaxPrice(context, maxPrice.toFloatOrNull() ?: 9999f)
                AppPreferences.setMinPickupMinutes(context, minPickupMinutes.toFloatOrNull() ?: 0f)
                AppPreferences.setMaxPickupMinutes(context, maxPickupMinutes.toFloatOrNull() ?: 30f)
                AppPreferences.setMaxPickupDistance(context, maxPickupDistance.toFloatOrNull() ?: 10f)
                saved = true
                AppState.appendLog("💾 Rules saved: price=${minPrice}–${maxPrice}﷼, minutes=${minPickupMinutes}–${maxPickupMinutes}, dist≤${maxPickupDistance}km")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Purple60)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Save Rules", fontWeight = FontWeight.Bold)
        }

        if (saved) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A3A1A)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenAccent)
                    Text("Rules saved successfully!", color = GreenAccent)
                }
            }
        }

        // Current Rules Summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Active Rules Summary", fontWeight = FontWeight.Bold, color = Color.White)
                HorizontalDivider(color = SurfaceVariant)
                SummaryRow("Price Range", "${AppPreferences.getMinPrice(context)} – ${AppPreferences.getMaxPrice(context)} ﷼")
                SummaryRow("Pickup Minutes", "${AppPreferences.getMinPickupMinutes(context)} – ${AppPreferences.getMaxPickupMinutes(context)} min")
                SummaryRow("Max Distance", "${AppPreferences.getMaxPickupDistance(context)} km")
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text(value, color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun RuleTextField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Purple60,
            focusedLabelColor = Purple60,
            focusedLeadingIconColor = Purple60,
            unfocusedBorderColor = SurfaceVariant,
            unfocusedLabelColor = OnSurfaceVariant,
            cursorColor = Purple60,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )
}
