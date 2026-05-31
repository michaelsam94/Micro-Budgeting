package com.michael.microbudgeting.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.michael.microbudgeting.domain.model.BudgetSummary
import java.util.Locale

@Composable
fun SpendingPieChart(
    summaries: List<BudgetSummary>,
    modifier: Modifier = Modifier
) {
    val nonZeroSummaries = summaries.filter { it.spent > 0.0 }
    val totalSpent = nonZeroSummaries.sumOf { it.spent }

    if (totalSpent == 0.0) {
        // Empty State visual representation
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "No recorded expenses yet for this month.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Add transactions manually or paste bank alerts to generate spending graphs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    var animateTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = totalSpent) {
        animateTrigger = true
    }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animateTrigger) 1f else 0f,
        animationSpec = tween(durationMillis = 800)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            val strokeColor = MaterialTheme.colorScheme.surface
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                nonZeroSummaries.forEach { summary ->
                    val sweepAngle = ((summary.spent / totalSpent) * 360f).toFloat() * animatedProgress
                    val color = try {
                        Color(android.graphics.Color.parseColor(summary.category.colorHex))
                    } catch (e: Exception) {
                        Color.Gray
                    }

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round),
                        size = Size(size.width - 24.dp.toPx(), size.height - 24.dp.toPx()),
                        topLeft = Offset(12.dp.toPx(), 12.dp.toPx())
                    )
                    startAngle += sweepAngle
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Spent",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format(Locale.getDefault(), "%,.1f EGP", totalSpent),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Grid-based legend
        Column(modifier = Modifier.fillMaxWidth()) {
            nonZeroSummaries.chunked(2).forEach { rowSummaries ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    rowSummaries.forEach { summary ->
                        val percent = (summary.spent / totalSpent * 100f).toFloat()
                        val color = try {
                            Color(android.graphics.Color.parseColor(summary.category.colorHex))
                        } catch (e: Exception) {
                            Color.Gray
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(color, shape = CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = summary.category.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "%,.1f EGP (%.1f%%)", summary.spent, percent),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (rowSummaries.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
