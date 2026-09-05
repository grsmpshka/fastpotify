package rocks.fastpotify.android.ui.profile

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit

enum class UiProfile(val wireValue: String, val label: String) {
    Automatic("automatic", "Авто"),
    VoyahFree("voyah_free", "Voyah Free"),
    Phone("phone", "Телефон"),
}

enum class ResolvedProfile { VoyahFree, Phone }

fun resolveProfile(
    preference: UiProfile,
    width: Dp,
    height: Dp,
): ResolvedProfile = when (preference) {
    UiProfile.VoyahFree -> ResolvedProfile.VoyahFree
    UiProfile.Phone -> ResolvedProfile.Phone
    UiProfile.Automatic -> {
        val aspectRatio = width.value / height.value.coerceAtLeast(1f)
        if (width >= 900.dp && height >= 360.dp && width > height && aspectRatio >= 1.8f) {
            ResolvedProfile.VoyahFree
        } else {
            ResolvedProfile.Phone
        }
    }
}

@Immutable
data class UiMetrics(
    val spacing: Dp,
    val smallSpacing: Dp,
    val panelPadding: Dp,
    val cardRadius: Dp,
    val touchTarget: Dp,
    val navigationHeight: Dp,
    val playerPanelWidth: Dp,
    val artworkSize: Dp,
    val cardWidth: Dp,
    val trackRowHeight: Dp,
    val navigationIconSize: Dp,
    val navigationLabelSize: TextUnit,
)

val CarMetrics = UiMetrics(
    spacing = 16.dp,
    smallSpacing = 8.dp,
    panelPadding = 24.dp,
    cardRadius = 8.dp,
    touchTarget = 64.dp,
    navigationHeight = 92.dp,
    playerPanelWidth = 400.dp,
    artworkSize = 230.dp,
    cardWidth = 190.dp,
    trackRowHeight = 76.dp,
    navigationIconSize = 32.dp,
    navigationLabelSize = 14.sp,
)

val PhoneMetrics = UiMetrics(
    spacing = 16.dp,
    smallSpacing = 8.dp,
    panelPadding = 16.dp,
    cardRadius = 8.dp,
    touchTarget = 48.dp,
    navigationHeight = 72.dp,
    playerPanelWidth = 0.dp,
    artworkSize = 320.dp,
    cardWidth = 158.dp,
    trackRowHeight = 64.dp,
    navigationIconSize = 26.dp,
    navigationLabelSize = 12.sp,
)
