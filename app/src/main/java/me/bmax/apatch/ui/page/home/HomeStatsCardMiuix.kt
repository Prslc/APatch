package me.bmax.apatch.ui.page.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.dialog.rememberJailbreakSoftRebootDialog
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.page.patch.PatchMode
import me.bmax.apatch.ui.theme.isInDarkTheme
import me.bmax.apatch.util.Version
import me.bmax.apatch.util.Version.getManagerVersion
import me.bmax.apatch.util.reboot
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor
import top.yukonga.miuix.kmp.utils.PressFeedbackType

private val managerVersion = getManagerVersion()

@Composable
fun KStatusCard(
    kpState: APApplication.State,
    apState: APApplication.State,
    apmCount: Int,
    kpmCount: Int,
    isPermissive: Boolean,
    onApmClick: () -> Unit,
    onKpmClick: () -> Unit,
    isJailbreak: Boolean,
    onJailbreakClick: () -> Unit,
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

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.defaultColors(
                    color = when {
                        isJailbreak -> colorScheme.tertiaryContainer
                        cardState.buttonAction == KPatchAction.UPDATE -> colorScheme.errorContainer
                        cardState.buttonAction == KPatchAction.UNKNOWN_STATE -> colorScheme.surfaceVariant
                        isDynamicColor -> colorScheme.secondaryContainer
                        isInDarkTheme(0) -> Color(0xFF1A3825)
                        else -> Color(0xFFDFFAE4)
                    }
                ),
                onClick = {
                    if (isJailbreak) {
                        softRebootDialog.show()
                    } else {
                        onMainCardClick()
                    }
                },
                pressFeedbackType = PressFeedbackType.Tilt
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(38.dp, 45.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Icon(
                            modifier = Modifier.size(170.dp),
                            imageVector = when {
                                isJailbreak -> Icons.Rounded.LockOpen
                                cardState.buttonAction == KPatchAction.UPDATE -> Icons.Rounded.ErrorOutline
                                cardState.buttonAction == KPatchAction.UNKNOWN_STATE -> Icons.AutoMirrored.Outlined.HelpOutline
                                else -> Icons.Rounded.CheckCircleOutline
                            },
                            tint = when {
                                isJailbreak -> colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
                                cardState.buttonAction == KPatchAction.UPDATE -> colorScheme.error.copy(alpha = 0.6f)
                                cardState.buttonAction == KPatchAction.UNKNOWN_STATE -> colorScheme.onSurfaceVariantSummary.copy(alpha = 0.4f)
                                isDynamicColor -> colorScheme.primary.copy(alpha = 0.8f)
                                else -> Color(0xFF36D167)
                            },
                            contentDescription = null
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(all = 16.dp)
                    ) {

                        Text(
                            text = if (isJailbreak) stringResource(R.string.settings_jailbreak_mode)
                            else stringResource(cardState.title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )

                        Spacer(Modifier.height(2.dp))
                        if (isJailbreak) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(R.string.settings_jailbreak_mode_summary),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            cardState.versionInfo?.let {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = cardState.badge?.let { badge -> "$it - $badge" } ?: it,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            cardState.subtitle?.let {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = it,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    insideMargin = PaddingValues(16.dp),
                    onClick = onApmClick,
                    showIndication = true,
                    pressFeedbackType = PressFeedbackType.Tilt
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.apm),
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                        )
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = apmCount.toString(),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    insideMargin = PaddingValues(16.dp),
                    onClick = onKpmClick,
                    showIndication = true,
                    pressFeedbackType = PressFeedbackType.Tilt
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.kpm),
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                        )
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = kpmCount.toString(),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        if (kpState == APApplication.State.UNKNOWN_STATE && isPermissive && !isJailbreak) {
            Spacer(Modifier.height(8.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onJailbreakClick
            ) {
                Text(stringResource(R.string.jailbreak))
            }
        }
    }
}

@Composable
fun AStatusCard(
    apState: APApplication.State
) {
    val cardState = remember(apState) {
        apState.toAPatchCardState(managerVersion)
    }

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                Text(
                    text = stringResource(R.string.android_patch),
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(cardState.icon, contentDescription = cardState.iconDesc)
                Column(
                    Modifier
                        .weight(2f)
                        .padding(start = 16.dp)
                ) {
                    Text(
                        text = stringResource(cardState.title),
                        style = MiuixTheme.textStyles.body2
                    )
                    cardState.subtitle?.let {
                        Text(text = it, style = MiuixTheme.textStyles.body2)
                    }
                }

                if (cardState.showButton) {
                    Column(
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Button(
                            enabled = cardState.isButtonEnabled,
                            colors = ButtonDefaults.buttonColors(),
                            onClick = {
                                when (cardState.buttonAction) {
                                    APatchAction.INSTALL, APatchAction.UPDATE -> {
                                        APApplication.installApatch()
                                    }

                                    APatchAction.NONE -> {}
                                }
                            },
                            content = {
                                val bIcon = cardState.buttonIcon
                                val bText = cardState.buttonText
                                if (bIcon != null) {
                                    Icon(bIcon, contentDescription = null)
                                } else if (bText != null) {
                                    Text(text = stringResource(id = bText))
                                }
                            })
                    }
                }
            }
        }
    }
}