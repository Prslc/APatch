package me.bmax.apatch.ui.page.online

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.bmax.apatch.R
import me.bmax.apatch.ui.LocalUiMode
import me.bmax.apatch.ui.UiMode
import me.bmax.apatch.util.ModuleRepo
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator as MiuixCircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun OnlineModuleDetailDialog(
    module: OnlineModule,
    kpatchReady: Boolean,
    cachedDoc: ModuleRepo.RepoModuleDoc?,
    loadDoc: suspend () -> ModuleRepo.ModuleDocResult,
    onDismiss: () -> Unit,
    onInstall: (ModuleRepo.RepoModuleDoc) -> Unit
) {
    var doc by remember(module) { mutableStateOf(cachedDoc) }
    var loading by remember(module) { mutableStateOf(cachedDoc == null) }
    var failed by remember(module) { mutableStateOf(false) }
    var unavailable by remember(module) { mutableStateOf(false) }
    var unavailableMessage by remember(module) { mutableStateOf<String?>(null) }
    var attempt by remember(module) { mutableIntStateOf(0) }

    LaunchedEffect(module, attempt) {
        failed = false
        unavailable = false
        unavailableMessage = null
        if (doc == null) loading = true
        val result = loadDoc()
        loading = false
        when {
            result.doc != null -> doc = result.doc

            doc == null && result.unavailable -> {
                unavailable = true
                unavailableMessage = result.error
            }

            doc == null -> failed = true
        }
    }

    when (LocalUiMode.current) {
        UiMode.Material -> OnlineDetailDialogMaterial(
            module = module,
            doc = doc,
            loading = loading,
            failed = failed,
            unavailable = unavailable,
            unavailableMessage = unavailableMessage,
            kpatchReady = kpatchReady,
            onDismiss = onDismiss,
            onRetry = { attempt++ },
            onInstall = onInstall
        )

        UiMode.Miuix -> OnlineDetailDialogMiuix(
            module = module,
            doc = doc,
            loading = loading,
            failed = failed,
            unavailable = unavailable,
            unavailableMessage = unavailableMessage,
            kpatchReady = kpatchReady,
            onDismiss = onDismiss,
            onRetry = { attempt++ },
            onInstall = onInstall
        )
    }
}

private fun updateAvailable(module: OnlineModule, doc: ModuleRepo.RepoModuleDoc): Boolean {
    val installed = module.installedModule ?: return false
    return ModuleRepo.isNewerVersion(doc, installed.version, installed.versionCode)
}

@Composable
private fun statusText(module: OnlineModule, doc: ModuleRepo.RepoModuleDoc): String {
    return when {
        updateAvailable(module, doc) -> stringResource(
            R.string.online_update_available,
            doc.version
        )

        module.installed -> if (module.installedVersion.isBlank()) {
            stringResource(R.string.online_installed)
        } else {
            stringResource(R.string.online_installed_version, module.installedVersion)
        }

        else -> ""
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnlineDetailDialogMaterial(
    module: OnlineModule,
    doc: ModuleRepo.RepoModuleDoc?,
    loading: Boolean,
    failed: Boolean,
    unavailable: Boolean,
    unavailableMessage: String?,
    kpatchReady: Boolean,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onInstall: (ModuleRepo.RepoModuleDoc) -> Unit
) {
    val title = doc?.name?.ifBlank { module.id } ?: module.id
    val canUpdate = doc != null && updateAvailable(module, doc)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column {
                when {
                    loading && doc == null -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }

                    unavailable && doc == null -> Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(stringResource(R.string.online_not_available))
                        if (!unavailableMessage.isNullOrBlank()) {
                            Text(
                                text = unavailableMessage,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    failed && doc == null -> Text(
                        text = stringResource(R.string.online_detail_failed)
                    )

                    doc != null -> {
                        val status = statusText(module, doc)
                        if (status.isNotBlank()) {
                            Text(
                                text = status,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceBright
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                InfoItemMaterial(
                                    label = stringResource(R.string.module_info_version),
                                    value = doc.version
                                )
                                InfoItemMaterial(
                                    label = stringResource(R.string.module_info_author),
                                    value = doc.authors.joinToString(", ")
                                )
                                if (doc.license.isNotBlank()) {
                                    InfoItemMaterial(
                                        label = stringResource(R.string.module_info_license),
                                        value = doc.license
                                    )
                                }
                                val description =
                                    doc.descriptionText().ifBlank { doc.summaryText() }
                                if (description.isNotBlank()) {
                                    HorizontalDivider()
                                    Text(
                                        text = description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 10,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .heightIn(max = 240.dp)
                                            .verticalScroll(rememberScrollState())
                                    )
                                }
                            }
                        }
                        if (module.kpm && !kpatchReady) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.kpm_kp_not_installed),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            when {
                doc != null && doc.downloadUrl.isNotBlank() &&
                    (!module.installed || canUpdate) -> Button(
                    onClick = { onInstall(doc) },
                    enabled = !module.kpm || kpatchReady
                ) {
                    Text(
                        stringResource(
                            if (canUpdate) R.string.online_update else R.string.online_install
                        )
                    )
                }

                failed && doc == null -> TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.online_retry))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun OnlineDetailDialogMiuix(
    module: OnlineModule,
    doc: ModuleRepo.RepoModuleDoc?,
    loading: Boolean,
    failed: Boolean,
    unavailable: Boolean,
    unavailableMessage: String?,
    kpatchReady: Boolean,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onInstall: (ModuleRepo.RepoModuleDoc) -> Unit
) {
    val title = doc?.name?.ifBlank { module.id } ?: module.id
    val canUpdate = doc != null && updateAvailable(module, doc)

    WindowDialog(
        show = true,
        title = title,
        onDismissRequest = onDismiss
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            when {
                loading && doc == null -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MiuixCircularProgressIndicator()
                }

                unavailable && doc == null -> Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MiuixText(
                        text = stringResource(R.string.online_not_available),
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariantSummary
                    )
                    if (!unavailableMessage.isNullOrBlank()) {
                        MiuixText(
                            text = unavailableMessage,
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariantSummary
                        )
                    }
                }

                failed && doc == null -> MiuixText(
                    text = stringResource(R.string.online_detail_failed),
                    fontSize = 14.sp,
                    color = colorScheme.onSurfaceVariantSummary
                )

                doc != null -> {
                    val status = statusText(module, doc)
                    if (status.isNotBlank()) {
                        MiuixText(
                            text = status,
                            fontSize = 12.sp,
                            color = colorScheme.primary
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InfoItemMiuix(
                            label = stringResource(R.string.module_info_version),
                            value = doc.version
                        )
                        InfoItemMiuix(
                            label = stringResource(R.string.module_info_author),
                            value = doc.authors.joinToString(", ")
                        )
                        if (doc.license.isNotBlank()) {
                            InfoItemMiuix(
                                label = stringResource(R.string.module_info_license),
                                value = doc.license
                            )
                        }
                    }
                    val description = doc.descriptionText().ifBlank { doc.summaryText() }
                    if (description.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        MiuixText(
                            text = description,
                            fontSize = 14.sp,
                            color = colorScheme.onSurface,
                            maxLines = 10,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (module.kpm && !kpatchReady) {
                        Spacer(Modifier.height(8.dp))
                        MiuixText(
                            text = stringResource(R.string.kpm_kp_not_installed),
                            fontSize = 12.sp,
                            color = colorScheme.error
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                MiuixTextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                when {
                    failed && doc == null -> MiuixTextButton(
                        text = stringResource(R.string.online_retry),
                        onClick = onRetry,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )

                    doc != null && doc.downloadUrl.isNotBlank() &&
                        (!module.installed || canUpdate) -> MiuixTextButton(
                        text = stringResource(
                            if (canUpdate) R.string.online_update else R.string.online_install
                        ),
                        onClick = { onInstall(doc) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        enabled = !module.kpm || kpatchReady
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoItemMaterial(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun InfoItemMiuix(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiuixText(
            text = label,
            fontSize = 12.sp,
            color = colorScheme.onSurfaceVariantSummary
        )
        MiuixText(
            text = value,
            fontSize = 14.sp,
            color = colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}
