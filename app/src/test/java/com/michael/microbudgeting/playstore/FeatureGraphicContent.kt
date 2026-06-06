package com.michael.microbudgeting.playstore

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.michael.microbudgeting.ui.theme.BrandGold
import com.michael.microbudgeting.ui.theme.BrandGoldDark
import com.michael.microbudgeting.ui.theme.BrandInk
import com.michael.microbudgeting.ui.theme.BrandSlate
import com.michael.microbudgeting.ui.theme.BrandTeal
import com.michael.microbudgeting.ui.theme.BrandTealDark
import com.michael.microbudgeting.ui.theme.BrandTealLight
import com.michael.microbudgeting.ui.theme.MyApplicationTheme

@Composable
fun FeatureGraphicContent() {
    MyApplicationTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(BrandTealLight, BrandTealDark),
                        start = Offset.Zero,
                        end = Offset(1024f, 500f)
                    )
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val diagonal = Path().apply {
                    moveTo(0f, size.height * 0.08f)
                    lineTo(size.width, size.height * 0.54f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(diagonal, color = BrandTealDark.copy(alpha = 0.88f))
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 64.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Micro Budgeting",
                        color = Color.White,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Offline expense tracking",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Budgets, pasted bank alerts, charts, and encrypted local backups.",
                        color = Color.White.copy(alpha = 0.86f),
                        fontSize = 22.sp,
                        lineHeight = 30.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FeatureChip("No account")
                        FeatureChip("No internet permission")
                        FeatureChip("Encrypted backups")
                    }
                }

                Surface(
                    modifier = Modifier.size(236.dp),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(48.dp),
                    shadowElevation = 14.dp
                ) {
                    PlayStoreBrandMark(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun FeatureChip(text: String) {
    Surface(
        color = Color(0xFFEFFAF8),
        contentColor = BrandInk,
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(8.dp)) {
                drawCircle(color = BrandGold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PlayStoreBrandMark(
    modifier: Modifier = Modifier,
    cornerSize: Dp = 8.dp
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val radius = cornerSize.toPx()
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(BrandTealLight, BrandTealDark),
                start = Offset.Zero,
                end = Offset(w, h)
            ),
            cornerRadius = CornerRadius(radius, radius)
        )
        val diagonal = Path().apply {
            moveTo(0f, h * 0.12f)
            lineTo(w, h * 0.55f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(diagonal, color = BrandTealDark.copy(alpha = 0.82f))

        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.24f, h * 0.18f),
            size = Size(w * 0.42f, h * 0.58f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
        )
        drawRoundRect(
            color = BrandTealLight,
            topLeft = Offset(w * 0.32f, h * 0.28f),
            size = Size(w * 0.23f, h * 0.08f),
            cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
        )
        listOf(0.46f, 0.57f, 0.68f).forEachIndexed { index, rowY ->
            val rowColor = if (index == 0) BrandTeal else BrandSlate
            drawCircle(
                color = rowColor,
                radius = w * 0.028f,
                center = Offset(w * 0.31f, h * rowY)
            )
            drawRoundRect(
                color = rowColor,
                topLeft = Offset(w * 0.37f, h * rowY - h * 0.018f),
                size = Size(w * if (index == 1) 0.21f else 0.25f, h * 0.036f),
                cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
            )
        }
        drawCircle(
            color = BrandGoldDark,
            radius = w * 0.15f,
            center = Offset(w * 0.66f, h * 0.72f)
        )
        drawCircle(
            color = BrandGold,
            radius = w * 0.15f,
            center = Offset(w * 0.61f, h * 0.67f)
        )
        drawLine(
            color = BrandInk,
            start = Offset(w * 0.54f, h * 0.67f),
            end = Offset(w * 0.60f, h * 0.75f),
            strokeWidth = w * 0.05f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = BrandInk,
            start = Offset(w * 0.60f, h * 0.75f),
            end = Offset(w * 0.72f, h * 0.58f),
            strokeWidth = w * 0.05f,
            cap = StrokeCap.Round
        )
    }
}
