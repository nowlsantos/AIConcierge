package com.nowlsantos.aiconcierge.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nowlsantos.aiconcierge.R
import com.nowlsantos.aiconcierge.presentation.ConciergeUiEvent
import com.nowlsantos.aiconcierge.presentation.ConciergeViewModel
import com.nowlsantos.aiconcierge.ui.component.*
import com.nowlsantos.aiconcierge.ui.component.selection.SelectionView
import com.nowlsantos.aiconcierge.ui.component.media.MediaHubView
import com.nowlsantos.aiconcierge.ui.component.room.RoomControlDashboardView
import com.nowlsantos.aiconcierge.ui.component.widget.ArisVoiceIndicator
import com.nowlsantos.aiconcierge.ui.component.widget.BackButton
import com.nowlsantos.aiconcierge.ui.component.widget.HomeButton
import com.nowlsantos.common.ActionIntent
import com.nowlsantos.common.ActionType
import com.nowlsantos.aiconcierge.ui.component.modal.TopBarModalDialog
import com.nowlsantos.aiconcierge.ui.component.modal.TopBarDialogType
import com.nowlsantos.aiconcierge.ui.component.modal.ZoneItem
import com.nowlsantos.aiconcierge.ui.theme.GoldPen
import com.nowlsantos.aiconcierge.ui.theme.Transparent
import com.nowlsantos.aiconcierge.ui.theme.sfTypography
import com.nowlsantos.aiconcierge.utils.LoadImage
import com.nowlsantos.common.UiTemplate

@Composable
fun ConciergeScreen(
    viewModel: ConciergeViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentNode = uiState.currentNode
    val isMainHub = uiState.navStack.size <= 1 || currentNode?.uiTemplate == UiTemplate.RIBBON_HUB

    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(uiState.error) {
        uiState.error?.let { errorMsg ->
            android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_LONG).show()
            viewModel.onEvent(ConciergeUiEvent.OnErrorDismissed)
        }
    }

    Scaffold(
        //containerColor = PeninsulaCharcoal,
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(insets = WindowInsets.safeDrawing),
        topBar = {
            if (!uiState.isLoading) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    color = Transparent
                ) {
                    // Peninsula London Logo
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(R.drawable.peninsula_london_logo),
                            contentDescription = "Peninsula London",
                            modifier = Modifier.fillMaxHeight(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // GLOBAL BACK BUTTON (Top Left - Displayed on ALL screens EXCEPT Main Screen / RibbonHub)
                    if (!isMainHub) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BackButton(
                                onClick = { viewModel.onEvent(ConciergeUiEvent.OnBackClicked) },
                            )
                        }
                    }

                    Row(modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 90.dp),
                        //.background(WhitePen.copy(alpha = 0.1f)),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            // Aris Microphone
                            ArisVoiceIndicator(
                                isListening = uiState.isListening,
                                isSpeaking = uiState.isSpeaking,
                                isStreaming = uiState.isStreaming,
                                voiceVolume = uiState.voiceVolume,
                                onClick = { viewModel.onEvent(ConciergeUiEvent.OnMicClicked) },
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(0.8f),
                                //.padding(start = 50.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ZoneItem("top_language_icon", "Language", onClick = {
                                viewModel.onEvent(ConciergeUiEvent.OnTopBarDialogChanged(TopBarDialogType.LANGUAGE)) })
                            ZoneItem("top_myorders_icon", "My Orders", onClick = {
                                viewModel.onEvent(ConciergeUiEvent.OnTopBarDialogChanged(TopBarDialogType.MY_ORDERS)) })
                            ZoneItem("top_penchat_icon", "Penchat", onClick = {
                                viewModel.onEvent(ConciergeUiEvent.OnTopBarDialogChanged(TopBarDialogType.MESSAGES)) })
                        }
                    }

                }
            }
        },
        floatingActionButton = {
            if (!isMainHub && !uiState.isLoading) {
                HomeButton(
                    onClick = { viewModel.onEvent(ConciergeUiEvent.OnHomeClicked) },
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                //.background(PeninsulaCharcoal)
        ) {
            // 1. Root Stationary Background Image
            LoadImage(
                imageID = R.drawable.main_background,
                description = "Peninsula Main Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // 2. Main Screen / Navigation Content
            if (uiState.isLoading || currentNode == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = GoldPen,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Connecting to Residence Services...",
                        style = sfTypography.titleMedium,
                        color = GoldPen.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.5.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                ) {
                    // Main Content Animated Navigation Transition
                    AnimatedContent(
                        targetState = currentNode,
                        modifier = Modifier.weight(1f),
                        transitionSpec = {
                            val targetIndex = uiState.navStack.indexOf(targetState)
                            val initialIndex = uiState.navStack.indexOf(initialState)

                            val isHomeNav = targetState.uiTemplate == UiTemplate.RIBBON_HUB && initialState.uiTemplate != UiTemplate.RIBBON_HUB
                            val isBack = (initialIndex == -1 && targetIndex != -1) || (targetIndex != -1 && targetIndex < initialIndex)

                            if (isHomeNav || isBack) {
                                (slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }) + fadeIn()).togetherWith(
                                    slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }) + fadeOut()
                                )
                            } else {
                                (slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) + fadeIn()).togetherWith(
                                    slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }) + fadeOut()
                                )
                            }
                        },
                        label = "nav"
                    ) { node ->
                        when (node.uiTemplate) {
                            UiTemplate.ROOM_CONTROLS -> RoomControlDashboardView(
                                node = node,
                                roomStates = uiState.roomStates,
                                onRoomControlChange = { category, target, value ->
                                    viewModel.onEvent(ConciergeUiEvent.OnRoomControlChanged(category, target, value))
                                },
                                onActiveZoneChange = { zone ->
                                    viewModel.onEvent(ConciergeUiEvent.OnActiveRoomZoneChanged(zone))
                                }
                            )

                            UiTemplate.MEDIA_HUB -> MediaHubView(
                                node = node,
                                mediaState = uiState.mediaState,
                                scrollEvent = uiState.scrollEvent,
                                onScrollStatusChanged = { canDown, canUp ->
                                    viewModel.onScrollStatusChanged(node, canDown = canDown, canUp = canUp)
                                },
                                onMediaControlClick = { category, action, item ->
                                    viewModel.processActionIntent(
                                        ActionIntent(
                                            actionType = ActionType.MEDIA_CONTROL,
                                            category = category,
                                            value = action,
                                            targetSection = item
                                        )
                                    )
                                }
                            )

                            UiTemplate.RIBBON_HUB -> RibbonHub(
                                nodes = node.children,
                                roomStates = uiState.roomStates,
                                actionParams = uiState.actionParams,
                                onNodeClick = { viewModel.onEvent(ConciergeUiEvent.OnNodeSelected(it)) }
                            )

                            UiTemplate.CATEGORY_GRID -> CategoryGrid(
                                title = node.name,
                                nodes = node.children,
                                onNodeClick = { viewModel.onEvent(ConciergeUiEvent.OnNodeSelected(it)) }
                            )

                            UiTemplate.CHOICE_LIST -> ChoiceList(
                                title = node.name,
                                nodes = node.children,
                                scrollEvent = uiState.scrollEvent,
                                onScrollStatusChanged = { canDown, canUp ->
                                    viewModel.onScrollStatusChanged(node, canDown = canDown, canUp = canUp)
                                },
                                onNodeClick = { viewModel.onEvent(ConciergeUiEvent.OnNodeSelected(it)) }
                            )

                            UiTemplate.SELECTION_VIEW -> SelectionView(
                                node = node,
                                currentSelections = uiState.selections,
                                onSelectionChange = { g, o -> viewModel.onEvent(ConciergeUiEvent.OnSelectionChanged(g, o)) },
                                onOrderClick = { viewModel.onEvent(ConciergeUiEvent.OnSubmitOrder) },
                                department = uiState.navStack.getOrNull(1)?.name ?: "Service",
                                scrollEvent = uiState.scrollEvent,
                                onScrollStatusChanged = { canDown, canUp ->
                                    viewModel.onScrollStatusChanged(node, canDown = canDown, canUp = canUp)
                                }
                            )

                            UiTemplate.DETAIL_VIEW -> DetailView(
                                node = node,
                                actionParams = uiState.actionParams,
                                roomStates = uiState.roomStates,
                                mediaState = uiState.mediaState,
                                navStack = uiState.navStack,
                                scrollEvent = uiState.scrollEvent,
                                onScrollStatusChanged = { canDown, canUp ->
                                    viewModel.onScrollStatusChanged(node, canDown = canDown, canUp = canUp)
                                }
                            )

                            UiTemplate.ORDER_SUMMARY -> OrderSummaryView(
                                node = node,
                                selections = uiState.selections,
                                isConfirmed = uiState.isOrderConfirmed,
                                onConfirm = { viewModel.onEvent(ConciergeUiEvent.OnConfirmOrder) },
                                onHomeClick = { viewModel.onEvent(ConciergeUiEvent.OnHomeClicked) }
                            )

                            else -> RibbonHub(
                                nodes = node.children,
                                roomStates = uiState.roomStates,
                                actionParams = uiState.actionParams,
                                onNodeClick = { viewModel.onEvent(ConciergeUiEvent.OnNodeSelected(it)) }
                            )
                        }
                    }
                }
            }
            // 3. TopBar Modal Dialog (Languages, My Orders, Messages / Penchat)
            TopBarModalDialog(
                dialogType = uiState.activeTopBarDialog,
                selections = uiState.selections,
                pastOrders = uiState.pastOrders,
                onLanguageSelected = { viewModel.onEvent(ConciergeUiEvent.OnLanguageSelected(it)) },
                onCancelOrder = { viewModel.onEvent(ConciergeUiEvent.OnCancelOrder(it)) },
                onDismiss = { viewModel.onEvent(ConciergeUiEvent.OnTopBarDialogChanged(TopBarDialogType.NONE)) }
            )
        } // end of Main box
    }
}
