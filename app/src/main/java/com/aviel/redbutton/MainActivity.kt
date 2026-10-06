package com.aviel.redbutton

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

private const val PREFS = "red_button_prefs"
private const val KEY_NUMBER = "phone_number"
const val DEFAULT_NUMBER = "0500000000"

private val ButtonRed = Color(0xFFE53935)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RedButtonScreen()
                }
            }
        }
    }
}

private fun loadNumber(context: Context): String =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(KEY_NUMBER, DEFAULT_NUMBER) ?: DEFAULT_NUMBER

private fun saveNumber(context: Context, number: String) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit().putString(KEY_NUMBER, number).apply()
}

/** Keeps digits and a leading '+', so pasted numbers like "050-000 0000" still dial. */
fun cleanNumber(raw: String): String {
    val trimmed = raw.trim()
    val digits = trimmed.filter { it.isDigit() }
    return if (trimmed.startsWith("+")) "+$digits" else digits
}

private fun placeCall(context: Context, number: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No phone app found on this device", Toast.LENGTH_LONG).show()
    } catch (e: SecurityException) {
        Toast.makeText(context, "Call permission was not granted", Toast.LENGTH_LONG).show()
    }
}

private fun openDialer(context: Context, number: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No phone app found on this device", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun RedButtonScreen() {
    val context = LocalContext.current
    var number by remember { mutableStateOf(loadNumber(context)) }
    var editing by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            placeCall(context, number)
        } else {
            // Without permission Android won't let us place the call directly,
            // so open the dialer with the number filled in instead.
            Toast.makeText(context, "No call permission, opening the dialer", Toast.LENGTH_SHORT).show()
            openDialer(context, number)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top part: the logo. Tap it to change the number.
        Spacer(Modifier.height(32.dp))
        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = "Logo, tap to change the number",
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .clickable { editing = true },
        )
        Spacer(Modifier.height(12.dp))
        Text(text = number, style = MaterialTheme.typography.titleMedium)

        // Middle part: the big red button.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Button(
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CALL_PHONE
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) placeCall(context, number)
                    else permissionLauncher.launch(Manifest.permission.CALL_PHONE)
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonRed,
                    contentColor = Color.White,
                ),
                modifier = Modifier.size(220.dp),
            ) {
                Text("CALL", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (editing) {
        NumberDialog(
            initial = number,
            onClose = { newValue ->
                val cleaned = cleanNumber(newValue)
                // Closing the modal saves what was typed (an empty field keeps the old number).
                if (cleaned.isNotEmpty() && cleaned != "+") {
                    number = cleaned
                    saveNumber(context, cleaned)
                }
                editing = false
            },
        )
    }
}

@Composable
private fun NumberDialog(initial: String, onClose: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        // Tapping outside or pressing back also saves.
        onDismissRequest = { onClose(text) },
        title = { Text("Phone number") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onClose(text) }) { Text("Done") }
        },
    )
}
