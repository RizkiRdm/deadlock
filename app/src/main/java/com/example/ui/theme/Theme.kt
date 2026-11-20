package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ObsidianColorScheme = darkColorScheme(
    primary = SignalAmber,
    onPrimary = AmberInk,
    primaryContainer = AmberContainer,
    onPrimaryContainer = SignalAmber,
    secondary = BoneInk,
    onSecondary = ObsidianBg,
    secondaryContainer = ElevatedPlate,
    onSecondaryContainer = BoneInk,
    tertiary = VerifiedEmerald,
    onTertiary = ObsidianBg,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = VerifiedEmerald,
    background = ObsidianBg,
    onBackground = BoneInk,
    surface = AnodizedSurface,
    onSurface = BoneInk,
    surfaceVariant = ElevatedPlate,
    onSurfaceVariant = SteelMuted,
    outline = HairlineBorder,
    outlineVariant = HairlineBorder,
    error = AlertCrimson,
    onError = ObsidianBg,
    errorContainer = CrimsonContainer,
    onErrorContainer = AlertCrimson
)

private val PrecisionShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun DeadlockTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ObsidianColorScheme,
        typography = Typography,
        shapes = PrecisionShapes,
        content = content
    )
}
