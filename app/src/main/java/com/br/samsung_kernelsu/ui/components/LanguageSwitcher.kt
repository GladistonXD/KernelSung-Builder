package com.br.samsung_kernelsu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.br.samsung_kernelsu.i18n.AppLanguage

@Composable
fun LanguageSwitcher(
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier) {
        Row(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(currentLanguage.flag, fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                currentLanguage.code.uppercase().substringBefore("-"),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AppLanguage.values().forEach { language ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(language.flag, fontSize = 18.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                language.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (language == currentLanguage) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (language == currentLanguage)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            if (language == currentLanguage) {
                                Spacer(Modifier.width(8.dp))
                                Text("✓", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    onClick = {
                        onLanguageChange(language)
                        expanded = false
                    }
                )
            }
        }
    }
}