package com.zackjp.devicedx.shared.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zackjp.devicedx.ui.theme.IrisLight


@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
) {
    ColorButton(
        modifier = modifier,
        onClick = onClick,
        borderColor = IrisLight,
        containerColor = IrisLight.copy(alpha = 0.05f),
        contentColor = IrisLight,
        text = text,
    )
}

@Composable
fun ColorButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color?,
    containerColor: Color,
    contentColor: Color,
    text: String,
) {
    Button(
        border = borderColor?.let { BorderStroke(1.dp, borderColor) },
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        contentPadding = PaddingValues(16.dp),
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(
            modifier = Modifier,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            text = text,
            textAlign = TextAlign.Center,
        )
    }
}
