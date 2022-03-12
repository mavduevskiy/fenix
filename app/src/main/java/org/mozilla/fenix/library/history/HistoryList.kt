package org.mozilla.fenix.library.history

import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import mozilla.components.lib.state.ext.observeAsComposableState
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.PrimaryText
import org.mozilla.fenix.compose.SecondaryText
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.loadIntoView
import org.mozilla.fenix.theme.FirefoxTheme

data class CollapsedRange(var start: Int, var finish: Int? = null)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryList(
    history: Flow<PagingData<History>>,
    historyStore: HistoryFragmentStore,
    interactor: HistoryInteractor
) {
    val historyItems: LazyPagingItems<History> = history.collectAsLazyPagingItems()
    val pendingDeletionIds = historyStore.observeAsComposableState { state ->
        Log.d("kolobok", "observeAsComposableState = ${state.pendingDeletionIds}")
        state.pendingDeletionIds
    }.value

    val itemsWithHeaders: MutableMap<HistoryItemTimeGroup, Int> = mutableMapOf()
    val collapsedHeaders = remember {
        mutableStateMapOf(
            HistoryItemTimeGroup.Today to false,
            HistoryItemTimeGroup.Yesterday to false,
            HistoryItemTimeGroup.ThisWeek to false,
            HistoryItemTimeGroup.ThisMonth to false,
            HistoryItemTimeGroup.Older to false
        )
    }
//    val expandedState = remember(ArrayList<Boolean>()) { syncedTabs.map { EXPANDED_BY_DEFAULT }.toMutableStateList() }
//    history.collectAsState(initial = emptyArray<History>())
    val context = LocalContext.current
//    val collapsedRange = remember { mutableStateListOf<CollapsedRange>() }

    LazyColumn {

        val itemCount = historyItems.itemCount
        Log.d("kolobok", "itemCount = $itemCount")
//        val test = headersPositions.filter { it.second == true }
        for (index in 0 until itemCount) {
            val historyItem = historyItems.peek(index)
            if (historyItem != null) {
                var timeGroup: HistoryItemTimeGroup? = null
                val isPendingDeletion = false
                if (itemsWithHeaders.containsKey(historyItem.historyTimeGroup)) {
                    if (isPendingDeletion && itemsWithHeaders[historyItem.historyTimeGroup] == index) {
                        itemsWithHeaders.remove(historyItem.historyTimeGroup)
                    } else if (isPendingDeletion && itemsWithHeaders[historyItem.historyTimeGroup] != index) {
                        // do nothing
                    } else {
                        if (index <= itemsWithHeaders[historyItem.historyTimeGroup] as Int) {
                            itemsWithHeaders[historyItem.historyTimeGroup] = index
                            timeGroup = historyItem.historyTimeGroup
                        }
                    }
                } else if (!isPendingDeletion) {
                    itemsWithHeaders[historyItem.historyTimeGroup] = index
                    timeGroup = historyItem.historyTimeGroup
                }
                timeGroup?.humanReadable(context)?.let { text ->
//                    Log.d("kolobok", "humanReadable = $text")
//                    if (headersPositions.find { it.first == index } == null) {
//                        headersPositions.add(Pair(index, false))
//                    }
                    this@LazyColumn.stickyHeader(key = index) {
                        HistorySectionHeader(text, expanded = collapsedHeaders[historyItem.historyTimeGroup]) {
                            val currentValue = collapsedHeaders[historyItem.historyTimeGroup]!!
                            collapsedHeaders[historyItem.historyTimeGroup] = !currentValue
//                            val item = headersPositions.find { it.first == index }
//                            item?.let {
//                                headersPositions.add(Pair(it.first, true))
//                                headersPositions.remove(it)
//                            }
                        }
                    }
                }

//                val shouldHide = collapsedHeaders[historyItem.historyTimeGroup] //headersPositions.find { it.second && it.first < index } != null
                    item {
                        // Gets item, triggering page loads if needed
                        val historyItem = historyItems[index]!!

                        val collapsedHeader = collapsedHeaders[historyItem.historyTimeGroup] == true
                        val pendingDeletion = pendingDeletionIds?.contains(historyItem.visitedAt) == true
                        Log.d("kolobok", "collapsedHeader = $collapsedHeader, pendingDeletion = $pendingDeletion")
                        val shouldHide = collapsedHeader || pendingDeletion
                        Log.d("kolobok", "shouldHide = $shouldHide")
                        if (!shouldHide) {
                            val bodyText = when (historyItem) {
                                is History.Regular -> historyItem.url
                                is History.Metadata -> historyItem.url
                                is History.Group -> {
                                    val numChildren = historyItem.items.size
                                    val stringId = if (numChildren == 1) {
                                        R.string.history_search_group_site
                                    } else {
                                        R.string.history_search_group_sites
                                    }
                                    String.format(LocalContext.current.getString(stringId), numChildren)
                                }
                            }

                            val url = when (historyItem) {
                                is History.Regular -> historyItem.url
                                is History.Metadata -> historyItem.url
                                is History.Group -> null
                            }

                            HistoryItem(historyItem.title, bodyText, url, {}, {
                                interactor.onDeleteSome(setOf(historyItem))
                            })
                        }
                    }
            }
        }

//        itemsIndexed(historyItems) { index, historyItem ->
//            historyItem?.let {
//
//                var timeGroup: HistoryItemTimeGroup? = null
//                val isPendingDeletion = false
//                if (itemsWithHeaders.containsKey(it.historyTimeGroup)) {
//                    if (isPendingDeletion && itemsWithHeaders[it.historyTimeGroup] == index) {
//                        itemsWithHeaders.remove(it.historyTimeGroup)
//                    } else if (isPendingDeletion && itemsWithHeaders[it.historyTimeGroup] != index) {
//                        // do nothing
//                    } else {
//                        if (index <= itemsWithHeaders[it.historyTimeGroup] as Int) {
//                            itemsWithHeaders[it.historyTimeGroup] = index
//                            timeGroup = it.historyTimeGroup
//                        }
//                    }
//                } else if (!isPendingDeletion) {
//                    itemsWithHeaders[it.historyTimeGroup] = index
//                    timeGroup = it.historyTimeGroup
//                }
//
//                timeGroup?.humanReadable(LocalContext.current)?.let { text ->
////                    Log.d("kolobok", "humanReadable = $text")
//                    this@LazyColumn.stickyHeader(key = index) {
//                        Text(
//                            text = text,
//                            color = FirefoxTheme.colors.textPrimary,
//                            fontSize = 14.sp,
//                            overflow = TextOverflow.Ellipsis,
//                            maxLines = 1
//                        )
//                    }
//                }
//
//                val bodyText = when (it) {
//                    is History.Regular -> it.url
//                    is History.Metadata -> it.url
//                    is History.Group -> {
//                        val numChildren = it.items.size
//                        val stringId = if (numChildren == 1) {
//                            R.string.history_search_group_site
//                        } else {
//                            R.string.history_search_group_sites
//                        }
//                        String.format(LocalContext.current.getString(stringId), numChildren)
//                    }
//                }
//
//                val url = when (it) {
//                    is History.Regular -> it.url
//                    is History.Metadata -> it.url
//                    is History.Group -> null
//                }
//
//                HistoryItem(it.title, bodyText, url, {})
//            }
//        }
    }

}

@Composable
fun HistorySectionHeader(
    text: String,
    expanded: Boolean? = null,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FirefoxTheme.colors.layer1)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PrimaryText(
                text = text,
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(R.font.metropolis_semibold)),
                maxLines = 1,
            )

            expanded?.let {
                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    painter = painterResource(
                        if (expanded) R.drawable.ic_chevron_down else R.drawable.ic_chevron_up
                    ),
                    contentDescription = stringResource(
                        if (expanded) R.string.synced_tabs_collapse_group else R.string.synced_tabs_expand_group,
                    ),
                    tint = FirefoxTheme.colors.textPrimary,
                )
            }
        }

        Divider(color = FirefoxTheme.colors.borderPrimary)
    }
}

@Composable
fun HistoryItem(
    titleText: String,
    bodyText: String,
    url: String?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row {
//        Log.d("Test", "url = $url")
        if (url != null) {
            AndroidView(
                modifier = Modifier.size(36.dp, 36.dp),
                // The viewBlock provides us with the Context so we do not have to pass this down into the @Composable
                // ourself
                factory = { context ->
                    // Inside the viewBlock we create a good ol' fashion TextView to match the width and height of its
                    // parent
                    ImageView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            50.dp.value.toInt(),
                            50.dp.value.toInt()
                        )
                        context.components.core.icons.loadIntoView(this, url)
                    }
                })
        } else {
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.ic_multiple_tabs),
                contentDescription = null,
                modifier = Modifier.size(36.dp, 36.dp),
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.Center
            )
        }
//
//        Image(
//            url = previewImageUrl,
//            modifier = modifier,
//            targetSize = 108.dp,
//            contentScale = ContentScale.Crop
//        )

        Column(
            Modifier.weight(1f)
        ){
            PrimaryText(
                text = titleText,
//                modifier = Modifier.fillMaxWidth(),
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            SecondaryText(
                text = bodyText,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(top = 2.dp),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = stringResource(R.string.history_delete_item),
            tint = FirefoxTheme.colors.textPrimary,
            modifier = Modifier
                .size(36.dp, 36.dp)
                .background(FirefoxTheme.colors.layer1)
                .clickable(onClick = {
                    onDelete.invoke()
                })
        )
    }
}