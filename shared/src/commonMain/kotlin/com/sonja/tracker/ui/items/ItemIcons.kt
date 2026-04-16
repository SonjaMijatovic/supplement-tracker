package com.sonja.tracker.ui.items

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.EmojiFoodBeverage
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.sonja.tracker.generated.resources.Res
import com.sonja.tracker.generated.resources.ic_capsule
import com.sonja.tracker.generated.resources.ic_supplement_bottle
import com.sonja.tracker.generated.resources.ic_herb
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

object ItemIcons {
    // Material Icons Extended — ordered by supplement relevance
    val materialIcons: Map<String, ImageVector> = mapOf(
        "medication"          to Icons.Outlined.Medication,
        "local_pharmacy"      to Icons.Outlined.LocalPharmacy,
        "medical_services"    to Icons.Outlined.MedicalServices,
        "healing"             to Icons.Outlined.Healing,
        "monitor_heart"       to Icons.Outlined.MonitorHeart,
        "favorite"            to Icons.Outlined.Favorite,
        "spa"                 to Icons.Outlined.Spa,
        "eco"                 to Icons.Outlined.Eco,
        "water_drop"          to Icons.Outlined.WaterDrop,
        "opacity"             to Icons.Outlined.Opacity,
        "grain"               to Icons.Outlined.Grain,
        "fitness_center"      to Icons.Outlined.FitnessCenter,
        "self_improvement"    to Icons.Outlined.SelfImprovement,
        "bolt"                to Icons.Outlined.Bolt,
        "restaurant"          to Icons.Outlined.Restaurant,
        "emoji_food_beverage" to Icons.Outlined.EmojiFoodBeverage,
        "science"             to Icons.Outlined.Science,
        "wb_sunny"            to Icons.Outlined.WbSunny,
        "nights_stay"         to Icons.Outlined.NightsStay,
        "star"                to Icons.Outlined.Star,
    )

    // Custom Phosphor/Lucide icons stored as Compose Resources drawables
    val customIcons: Map<String, DrawableResource> = mapOf(
        "ic_capsule"           to Res.drawable.ic_capsule,
        "ic_supplement_bottle" to Res.drawable.ic_supplement_bottle,
        "ic_herb"              to Res.drawable.ic_herb,
    )

    // Ordered list for display in the grid — material icons first, then custom
    val allIconIds: List<String> = materialIcons.keys.toList() + customIcons.keys.toList()
}

/**
 * Renders the icon for a given iconId using Material ImageVector (preferred) or
 * Compose Resource painterResource (for custom SVG icons).
 * Renders nothing for unknown iconIds — callers should guard against this.
 */
@Composable
fun ItemIconContent(iconId: String, modifier: Modifier = Modifier) {
    val vec = ItemIcons.materialIcons[iconId]
    val res = ItemIcons.customIcons[iconId]
    when {
        vec != null -> Icon(
            imageVector = vec,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = modifier
        )
        res != null -> Icon(
            painter = painterResource(res),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = modifier
        )
        // Unknown iconId — render nothing (should not occur with valid data)
    }
}
