package me.bmax.apatch.ui.page.material.patch

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.SwitchItem
import me.bmax.apatch.ui.navigation.LocalNavigator
import me.bmax.apatch.ui.navigation.Navigator
import me.bmax.apatch.ui.page.kpm.KPModel
import me.bmax.apatch.ui.page.patch.PatchMode
import me.bmax.apatch.ui.page.patch.PatchesViewModel
import me.bmax.apatch.ui.page.patch.PatchUiState
import me.bmax.apatch.ui.page.patch.utils.checkSuperKeyValidation
import me.bmax.apatch.ui.theme.LocalEnableBlur
import me.bmax.apatch.ui.theme.getMaterial3AppBarColor
import me.bmax.apatch.ui.theme.material3BlurEffect
import me.bmax.apatch.ui.theme.rememberMaterial3BlurBackdrop
import me.bmax.apatch.ui.theme.withBackdrop
import me.bmax.apatch.util.Version
import me.bmax.apatch.util.reboot

private const val TAG = "KernelPatchModule"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PatchesScreenMaterial(
    mode: PatchMode,
    bootImageUri: Uri? = null,
    viewModel: PatchesViewModel = viewModel()
) {
    val navigator = LocalNavigator.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val scrollState = rememberScrollState()
    val backdrop = rememberMaterial3BlurBackdrop(LocalEnableBlur.current)

    var needKey by rememberSaveable { mutableStateOf(false) }

    val selectFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.copyAndParseBootimg(uri)
            }
        }
    }

    LaunchedEffect(mode) {
        viewModel.prepare(mode)
        if (mode == PatchMode.PATCH_ONLY && bootImageUri != null) {
            viewModel.copyAndParseBootimg(bootImageUri)
        }
    }

    LaunchedEffect(Unit) {
        val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(
                context, it
            ) != PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                context as Activity,
                toRequest.toTypedArray(),
                1001
            )
        }
    }

    LaunchedEffect(uiState.patchLog) {
        if (uiState.isPatching) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            Column(modifier = Modifier.material3BlurEffect(backdrop)) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.patch_config_title),
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = dropUnlessResumed { navigator.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backdrop.getMaterial3AppBarColor(),
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        scrolledContainerColor = backdrop.getMaterial3AppBarColor()
                    )
                )
            }
        },
        bottomBar = {
            if (!uiState.isRunning) {
                BottomButtonsMaterial(
                    viewModel = viewModel,
                    uiState = uiState,
                    mode = mode,
                    needKey = needKey,
                    navigator = navigator,
                    selectFileLauncher = selectFileLauncher
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .withBackdrop(backdrop)
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(mode.sId),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            if (uiState.error.isNotEmpty()) {
                ErrorViewMaterial(uiState.error)
            }

            if (uiState.kpimgInfo.version.isNotEmpty()) {
                KernelPatchImageViewMaterial(uiState.kpimgInfo)
            }

            if (uiState.bootSlot.isNotEmpty() || uiState.bootDev.isNotEmpty()) {
                BootimgViewMaterial(
                    slot = uiState.bootSlot,
                    boot = uiState.bootDev
                )
            }

            if (uiState.kimgInfo.banner.isNotEmpty()) {
                KernelImageViewMaterial(uiState.kimgInfo)
            }

            if (mode != PatchMode.UNPATCH && uiState.kimgInfo.banner.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceBright
                ) {
                    Column {
                        SwitchItem(
                            icon = Icons.Default.Key,
                            title = stringResource(R.string.patch_custom_superkey),
                            summary = stringResource(R.string.patch_custom_superkey_summary),
                            checked = needKey,
                            onCheckedChange = { checked ->
                                needKey = checked
                            }
                        )
                        AnimatedVisibility(
                            visible = needKey,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            SetSuperKeyViewMaterial(viewModel, uiState)
                        }
                    }
                }
            }

            if (mode != PatchMode.UNPATCH) {
                uiState.existedExtras.toList().forEach { extra ->
                    ExtraItemMaterial(
                        extra = extra,
                        existed = true,
                        onDelete = {
                            viewModel.removeExistedExtra(extra)
                        }
                    )
                }

                uiState.newExtras.toList().forEach { extra ->
                    ExtraItemMaterial(
                        extra = extra,
                        existed = false,
                        onDelete = {
                            val idx = uiState.newExtras.indexOf(extra)
                            viewModel.removeNewExtra(idx)
                        }
                    )
                }

                if (!uiState.isPatching && !uiState.isPatchDone) {
                    AddKpmItemMaterial { uri ->
                        viewModel.embedKPM(uri)
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.isPatching || uiState.isPatchDone
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceBright
                ) {
                    SelectionContainer {
                        Text(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            text = uiState.patchLog,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
    if (uiState.isRunning) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun BottomButtonsMaterial(
    viewModel: PatchesViewModel,
    uiState: PatchUiState,
    mode: PatchMode,
    needKey: Boolean,
    navigator: Navigator,
    selectFileLauncher: ActivityResultLauncher<Intent>,
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        when {
            uiState.isPatching -> {
                Button(
                    enabled = false,
                    onClick = {},
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.patch_patching))
                }
            }

            uiState.needReboot -> {
                Button(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) { reboot() }
                        }
                    }
                ) {
                    Text(stringResource(R.string.reboot))
                }
            }

            uiState.isPatchDone -> {
                Button(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    onClick = { navigator.popBackStack() }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            }

            mode == PatchMode.PATCH_ONLY && uiState.kimgInfo.banner.isEmpty() -> {
                Button(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    onClick = {
                        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }
                        selectFileLauncher.launch(intent)
                    }
                ) {
                    Text(stringResource(R.string.patch_select_bootimg_btn))
                }
            }

            else -> {
                val isUnpatch = mode == PatchMode.UNPATCH
                val isSecurityReady = !needKey || uiState.superkey.isNotEmpty()

                val shouldShow = if (isUnpatch) uiState.kimgInfo.banner.isNotEmpty() else true
                val isEnabled = !isUnpatch && isSecurityReady || isUnpatch

                val btnText =
                    stringResource(if (isUnpatch) R.string.patch_start_unpatch_btn else R.string.patch_start_patch_btn)

                if (shouldShow) {
                    Button(
                        shape = RoundedCornerShape(16.dp),
                        enabled = isEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        onClick = {
                            if (isUnpatch) viewModel.doUnpatch() else viewModel.doPatch(
                                mode,
                                needKey
                            )
                        }
                    ) {
                        Text(btnText)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtraConfigDialogMaterial(
    kpmInfo: KPModel.KPMInfo,
    show: MutableState<Boolean>
) {
    var event by remember { mutableStateOf(kpmInfo.event) }
    var args by remember { mutableStateOf(kpmInfo.args) }

    if (show.value) {
        BasicAlertDialog(
            onDismissRequest = { show.value = false }
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = AlertDialogDefaults.shape,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.kpm_control_dialog_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = event,
                        label = { Text(stringResource(R.string.patch_item_extra_event)) },
                        onValueChange = { event = it },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = args,
                        label = { Text(stringResource(id = R.string.patch_item_extra_args)) },
                        onValueChange = { args = it },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { show.value = false }) {
                            Text(stringResource(android.R.string.cancel))
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                kpmInfo.event = event
                                kpmInfo.args = args
                                show.value = false
                            }
                        ) {
                            Text(stringResource(android.R.string.ok))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtraItemMaterial(
    extra: KPModel.IExtraInfo,
    existed: Boolean,
    onDelete: () -> Unit
) {
    val showConfigDialog = remember { mutableStateOf(false) }

    if (extra is KPModel.KPMInfo && showConfigDialog.value) {
        ExtraConfigDialogMaterial(kpmInfo = extra, show = showConfigDialog)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceBright
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (extra is KPModel.KPMInfo) extra.name else extra.type.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(if (existed) R.string.patch_item_existed_extra_kpm else R.string.patch_item_new_extra_kpm),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (extra.type == KPModel.ExtraType.KPM) {
                    IconButton(onClick = { showConfigDialog.value = true }) {
                        Icon(Icons.Default.Settings, null, modifier = Modifier.size(20.dp))
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp))
                }
            }

            if (extra is KPModel.KPMInfo) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    DetailTextMaterial(stringResource(R.string.patch_item_extra_version), extra.version)
                    DetailTextMaterial(stringResource(R.string.patch_item_extra_kpm_license), extra.license)
                    DetailTextMaterial(stringResource(R.string.patch_item_extra_author), extra.author)
                    DetailTextMaterial(
                        stringResource(R.string.patch_item_extra_kpm_desciption),
                        extra.description
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailTextMaterial(label: String, value: String) {
    Text(
        text = "$label $value",
        fontSize = 12.sp,
        fontWeight = FontWeight(550),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AddKpmItemMaterial(onSelected: (Uri) -> Unit) {
    val selectFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                Log.d(TAG, "select kpm, uri=$uri")
                onSelected(uri)
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }
                selectFileLauncher.launch(intent)
            },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceBright
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer(rotationZ = -90f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.patch_embed_kpm_btn),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SetSuperKeyViewMaterial(viewModel: PatchesViewModel, uiState: PatchUiState) {
    var skey by remember { mutableStateOf(uiState.superkey) }
    var keyVisible by remember { mutableStateOf(false) }
    val showWarn by remember(skey) {
        derivedStateOf { !checkSuperKeyValidation(skey) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (showWarn) {
            Text(
                color = MaterialTheme.colorScheme.error,
                text = stringResource(id = R.string.patch_item_set_skey_label),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(6.dp))
        }

        Box {
            OutlinedTextField(
                value = skey,
                label = { Text(stringResource(id = R.string.patch_set_superkey)) },
                visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                onValueChange = {
                    skey = it
                    viewModel.setSuperKey(it)
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            IconButton(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 5.dp),
                onClick = { keyVisible = !keyVisible }
            ) {
                Icon(
                    imageVector = if (keyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun KernelPatchImageViewMaterial(kpImgInfo: KPModel.KPImgInfo) {
    if (kpImgInfo.version.isEmpty()) return
    InfoCardMaterial {
        InfoTitleMaterial(stringResource(id = R.string.patch_item_kpimg))
        Spacer(Modifier.height(8.dp))
        DetailTextMaterial(
            stringResource(id = R.string.patch_item_kpimg_version),
            Version.uInt2String(kpImgInfo.version.substring(2).toUInt(16))
        )
        DetailTextMaterial(
            stringResource(id = R.string.patch_item_kpimg_comile_time),
            kpImgInfo.compileTime
        )
        DetailTextMaterial(
            stringResource(id = R.string.patch_item_kpimg_config),
            kpImgInfo.config
        )
    }
}

@Composable
private fun BootimgViewMaterial(slot: String, boot: String) {
    InfoCardMaterial {
        InfoTitleMaterial(stringResource(id = R.string.patch_item_bootimg))
        Spacer(Modifier.height(8.dp))
        if (slot.isNotEmpty()) {
            DetailTextMaterial(
                stringResource(id = R.string.patch_item_bootimg_slot),
                slot
            )
        }
        DetailTextMaterial(
            stringResource(id = R.string.patch_item_bootimg_dev),
            boot
        )
    }
}

@Composable
private fun KernelImageViewMaterial(kImgInfo: KPModel.KImgInfo) {
    InfoCardMaterial {
        InfoTitleMaterial(stringResource(id = R.string.patch_item_kernel))
        Spacer(Modifier.height(8.dp))
        Text(text = kImgInfo.banner, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ErrorViewMaterial(error: String) {
    if (error.isEmpty()) return
    InfoCardMaterial {
        Text(
            text = stringResource(id = R.string.patch_item_error),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun InfoCardMaterial(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceBright
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            content = content
        )
    }
}

@Composable
private fun InfoTitleMaterial(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
