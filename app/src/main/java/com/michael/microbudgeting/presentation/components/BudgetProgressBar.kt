package com.michael.microbudgeting.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.michael.microbudgeting.domain.model.BudgetSummary
import java.util.Locale

@Composable
fun BudgetProgressBar(
    summary: BudgetSummary,
    modifier: Modifier = Modifier
) {
    val budgetSet = summary.budget != null
    val limit = summary.budget?.limitAmount ?: 0.0
    val progress = if (limit > 0.0) (summary.spent / limit).coerceIn(0.0, 1.0).toFloat() else 0f
    
    // Aesthetic pairings for thresholds
    val color = when {
        progress >= 1f -> MaterialTheme.colorScheme.error
        progress >= 0.8f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(
                    iconName = summary.category.iconName,
                    hexColor = summary.category.colorHex,
                    size = 32
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = summary.category.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!budgetSet) {
                        Text(
                            text = "Unbudgeted category",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (budgetSet) {
                        String.format(Locale.getDefault(), "%,.0f / %.,0f EGP", summary.spent, limit)
                    } else {
                        String.format(Locale.getDefault(), "%,.0f EGP", summary.spent)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (budgetSet) {
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (summary.remaining < 0) {
                Text(
                    text = String.format(Locale.getDefault(), "Over budget by %.,0f EGP!", -summary.remaining),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else if (summary.remaining > 0) {
                Text(
                    text = String.format(Locale.getDefault(), "%.,0f EGP remaining available", summary.remaining),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
