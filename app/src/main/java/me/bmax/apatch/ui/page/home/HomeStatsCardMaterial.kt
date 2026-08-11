package me.bmax.apatch.ui.page.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.dialog.rememberJailbreakSoftRebootDialog
import me.bmax.apatch.ui.component.labeltext.LabelText
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.page.patch.PatchMode
import me.bmax.apatch.util.Version
import me.bmax.apatch.util.Version.getManagerVersion
import me.bmax.apatch.util.reboot

private val managerVersion = getManagerVersion()

@Composable
internal fun KStatusCardMaterial(
    kpState: APApplication.State,
    apState: APApplication.State,
    isPermissive: Boolean = false,
    isJailbreak: Boolean = false,
    onJailbreakClick: () -> Unit = {},
) {
    val navigator = LocalNavigator.current
    val cardState = remember(kpState, apState) {
        kpState.toKPatchCardState(apState, managerVersion)
    }

    val softRebootDialog = rememberJailbreakSoftRebootDialog()

    val onMainCardClick = {
        when (cardState.buttonAction) {
            KPatchAction.UNKNOWN_STATE -> navigator.navigateToModeSelect()
            KPatchAction.UPDATE -> {
                if (Version.installedKPVUInt() < 0x900u) {
                    navigator.navigateToPatches(PatchMode.PATCH_ONLY)
                } else {
                    navigator.navigateToModeSelect()
                }
            }

            KPatchAction.REBOOT -> reboot()

            else -> {
                if (kpState != APApplication.State.KERNELPATCH_INSTALLED) {
                    navigator.navigateToModeSelect()
                }
            }
        }
    }
    
    val containerColor = when {
        cardState.buttonAction == KPatchAction.UPDATE -> MaterialTheme.colorScheme.errorContainer
        cardState.buttonAction == KPatchAction.UNKNOWN_STATE -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.primary
    }
    val contentColor = when {
        cardState.buttonAction == KPatchAction.UPDATE -> MaterialTheme.colorScheme.onErrorContainer
        cardState.buttonAction == KPatchAction.UNKNOWN_STATE -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onPrimary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                if (isJailbreak) {
                    softRebootDialog.show()
                } else {
                    onMainCardClick()
                }
            }),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isJailbreak) Icons.Rounded.LockOpen else cardState.icon,
                contentDescription = cardState.iconDesc,
                modifier = Modifier.size(24.dp),
                tint = contentColor
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isJailbreak) stringResource(R.string.settings_jailbreak_mode)
                        else stringResource(cardState.title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!isJailbreak) {
                        cardState.badge?.let {
                            LabelText(
                                label = it,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                if (isJailbreak) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_jailbreak_mode_summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                } else {
                    cardState.versionInfo?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    cardState.subtitle?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            if (!isJailbreak && cardState.buttonAction != KPatchAction.NONE) {
                Spacer(Modifier.width(12.dp))
                FilledTonalButton(
                    enabled = cardState.isButtonEnabled,
                    onClick = onMainCardClick
                ) {
                    Text(stringResource(cardState.buttonText))
                }
            }
        }
    }

    if (kpState == APApplication.State.UNKNOWN_STATE && isPermissive && !isJailbreak) {
        Spacer(Modifier.height(8.dp))
        FilledTonalButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onJailbreakClick
        ) {
            Text(stringResource(R.string.jailbreak))
        }
    }
}


@Composable
internal fun AStatusCardMaterial(apState: APApplication.State) {
    val cardState = remember(apState) {
        apState.toAPatchCardState(managerVersion)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceBright
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = cardState.icon,
                contentDescription = cardState.iconDesc,
                modifier = Modifier.size(24.dp),
                tint = when (cardState.buttonAction) {
                    APatchAction.INSTALL, APatchAction.UPDATE ->
                        MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.android_patch),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(cardState.title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                cardState.subtitle?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (cardState.showButton) {
                Spacer(Modifier.width(12.dp))
                FilledTonalButton(
                    enabled = cardState.isButtonEnabled,
                    onClick = {
                        when (cardState.buttonAction) {
                            APatchAction.INSTALL, APatchAction.UPDATE -> {
                                APApplication.installApatch()
                            }

                            APatchAction.NONE -> {}
                        }
                    }
                ) {
                    val bIcon = cardState.buttonIcon
                    val bText = cardState.buttonText
                    if (bIcon != null) {
                        Icon(bIcon, contentDescription = null)
                    } else if (bText != null) {
                        Text(text = stringResource(id = bText))
                    }
                }
            }
        }
    }
}


