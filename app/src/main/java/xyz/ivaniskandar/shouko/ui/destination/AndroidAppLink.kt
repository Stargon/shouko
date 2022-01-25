package xyz.ivaniskandar.shouko.ui.destination

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.ListItem
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.activity.MainActivityViewModel
import xyz.ivaniskandar.shouko.item.LinkHandlerAppItem
import xyz.ivaniskandar.shouko.ui.ComposeLifecycleCallback
import xyz.ivaniskandar.shouko.ui.Screen
import xyz.ivaniskandar.shouko.ui.component.Preference
import xyz.ivaniskandar.shouko.ui.theme.ShoukoTheme
import xyz.ivaniskandar.shouko.util.checkDefaultBrowser
import xyz.ivaniskandar.shouko.util.getPackageLabel

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun AndroidAppLinkSettings(
    context: Context = LocalContext.current,
    navController: NavController,
    onOpenSettings: () -> Unit
) {
    var isDefaultBrowser by remember { mutableStateOf(checkDefaultBrowser(context)) }
    var showEnableInfoDialog by remember { mutableStateOf(false) }

    Column {
        CustomChooserToggle(
            checked = isDefaultBrowser,
            onClick = {
                if (!isDefaultBrowser) {
                    showEnableInfoDialog = true
                } else {
                    onOpenSettings()
                }
            }
        )

        Preference(
            title = stringResource(R.string.approved_link_target_title),
            subtitle = stringResource(R.string.approved_link_target_subtitle),
            onPreferenceClick = { navController.navigate(Screen.ApprovedLinkTargetList.route) }
        )
        Preference(
            title = stringResource(R.string.unapproved_link_target_title),
            subtitle = stringResource(R.string.unapproved_link_target_subtitle),
            onPreferenceClick = { navController.navigate(Screen.UnapprovedLinkTargetList.route) }
        )

        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp)
            )
            Text(
                text = stringResource(id = R.string.link_chooser_info),
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.body2
            )
        }
    }

    if (showEnableInfoDialog) {
        AlertDialog(
            onDismissRequest = { showEnableInfoDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEnableInfoDialog = false
                        onOpenSettings()
                    }
                ) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
            title = { Text(text = stringResource(R.string.link_chooser_toggle_label)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.link_chooser_enable_dialog,
                        context.getPackageLabel(context.packageName)
                    )
                )
            }
        )
    }

    // Refresh default browser status on resume
    ComposeLifecycleCallback(onResume = { isDefaultBrowser = checkDefaultBrowser(context) })
}

@Composable
fun CustomChooserToggle(checked: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
                Text(
                    text = stringResource(R.string.link_chooser_toggle_label),
                    modifier = Modifier.weight(1F),
                    style = MaterialTheme.typography.h6
                )
            }
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Preview
@Composable
fun CustomChooserTogglePreview() {
    ShoukoTheme {
        CustomChooserToggle(checked = true) {}
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun LinkTargetList(
    approved: Boolean, // if true, show approved else unapproved
    mainViewModel: MainActivityViewModel = viewModel(),
    navController: NavController
) {
    val items by mainViewModel.linkHandlerList.observeAsState()
    val isRefreshing by mainViewModel.isRefreshingLinkHandlerList.collectAsState()
    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing),
        onRefresh = { mainViewModel.refreshLinkHandlerList() },
        modifier = Modifier.fillMaxSize()
    ) {
        val filteredItems = items?.filter { if (approved) it.linkHandlingAllowed && it.isApproved else it.isUnapproved }
        val disabledItems = if (approved) items?.filter { !it.linkHandlingAllowed && it.isApproved } else null

        LazyColumn(contentPadding = rememberInsetsPaddingValues(LocalWindowInsets.current.navigationBars)) {
            if (filteredItems != null) {
                items(items = filteredItems, key = { it.packageName }) { item ->
                    LinkTargetListItem(item = item) {
                        navController.navigate(Screen.LinkTargetInfoSheet.createRoute(item.packageName))
                    }
                }
            }

            if (!disabledItems.isNullOrEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.disabled),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colors.primary,
                        style = MaterialTheme.typography.subtitle2
                    )
                }

                items(items = disabledItems, key = { it.packageName }) { item ->
                    LinkTargetListItem(item = item) {
                        navController.navigate(Screen.LinkTargetInfoSheet.createRoute(item.packageName))
                    }
                }
            }
        }
    }

    ComposeLifecycleCallback(onResume = { mainViewModel.refreshLinkHandlerList() })
}

@Composable
fun LinkTargetListItem(item: LinkHandlerAppItem, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        icon = {
            Image(
                bitmap = item.icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
        },
        text = {
            Text(text = item.label)
        }
    )
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun LinkTargetInfoSheet(
    packageName: String,
    mainViewModel: MainActivityViewModel = viewModel(),
    onOpenSettings: (String) -> Unit
) {
    val list by mainViewModel.linkHandlerList.observeAsState()
    val item = list!!.find { it.packageName == packageName }!!
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, top = 24.dp, end = 16.dp)
    ) {
        Image(
            bitmap = item.icon,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .padding(bottom = 4.dp)
                .align(Alignment.CenterHorizontally)
        )
        Text(
            text = item.label,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.h6
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (item.linkHandlingAllowed && (item.verifiedDomains.isNotEmpty() || item.userSelectedDomains.isNotEmpty())) {
            val domains = (item.verifiedDomains + item.userSelectedDomains).toList()
            val domainsCount = domains.count()
            Text(
                text = LocalContext.current.resources.getQuantityString(
                    R.plurals.approved_link_list_title,
                    domainsCount,
                    domainsCount
                ),
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colors.primary,
                style = MaterialTheme.typography.subtitle2
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1F, fill = false)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(domains) { domain ->
                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                        ProvideTextStyle(MaterialTheme.typography.subtitle1) {
                            Text(text = domain, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }
        } else {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                Text(
                    text = stringResource(R.string.approved_link_disabled_text),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body2
                )
            }
        }

        Button(
            onClick = { onOpenSettings(packageName) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Icon(
                Icons.Default.Launch,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = stringResource(R.string.open_settings))
        }
    }
}