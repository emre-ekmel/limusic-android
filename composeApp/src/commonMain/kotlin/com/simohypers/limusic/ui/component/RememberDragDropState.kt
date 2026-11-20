package com.simohypers.limusic.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.simohypers.limusic.logger.Logger
import com.simohypers.limusic.ui.icon.DragHandle
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.theme.rounded
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.move_down
import limusic.composeapp.generated.resources.move_up
import limusic.composeapp.generated.resources.queue_reorder
import org.jetbrains.compose.resources.stringResource

/**
 * [onSwap] gets the dragged row's lazy index before the drag and where it was dropped; [canMoveTo]
 * says which lazy indexes a row may be dragged over and dropped on (headers are not). Both are
 * the latest the caller passed, not the first: they read the caller's current list.
 */
@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    canMoveTo: (Int) -> Boolean = { true },
    onSwap: (Int, Int) -> Unit,
): DragDropState {
    val scope = rememberCoroutineScope()
    val state = remember(lazyListState) { DragDropState(state = lazyListState, scope = scope) }
    state.onSwap = onSwap
    state.canMoveTo = canMoveTo
    return state
}

fun LazyListState.getVisibleItemInfoFor(absoluteIndex: Int): LazyListItemInfo? =
    this
        .layoutInfo
        .visibleItemsInfo
        .getOrNull(
            absoluteIndex -
                this.layoutInfo.visibleItemsInfo
                    .first()
                    .index,
        )

val LazyListItemInfo.offsetEnd: Int
    get() = this.offset + this.size

/**
 * A reorderable row. The one picked up lifts (a tonal card, a shadow, a little larger, above the
 * others) and follows the finger exactly; the others slide out of its way. Dropped, it springs into
 * its slot and settles back down.
 *
 * [key] is the row's lazy key: the row is recognised by it, so it stays the dragged one while its
 * index changes under it.
 */
@OptIn(ExperimentalFoundationApi::class)
@ExperimentalFoundationApi
@Composable
fun LazyItemScope.DraggableItem(
    dragDropState: DragDropState,
    index: Int,
    key: Any,
    modifier: Modifier,
    /** What the lifted row is drawn on: [tonalLift] on a solid background, [translucentLift] over the cover. */
    liftColor: Color = tonalLift(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface),
    content: @Composable ColumnScope.(isDragging: Boolean) -> Unit,
) {
    val dragging = key == dragDropState.draggingKey
    val settling = key == dragDropState.settlingKey
    val liftState = animateFloatAsState(if (dragging) 1f else 0f, spring(stiffness = Spring.StiffnessMediumLow), label = "rowLift")
    val raised by remember { derivedStateOf { liftState.value > 0f } }
    val shape = rounded(8.dp)
    // A see-through lift shows the shadow under it too: a fainter one keeps the cover's colours.
    val shadowColor = if (liftColor.alpha < 1f) SOFT_SHADOW else DefaultShadowColor
    Column(
        modifier =
            Modifier
                // The row being moved places itself (through the translation below): a placement
                // animation on it as well would pull it away from the finger on every swap.
                .animateItem(
                    fadeInSpec = null,
                    fadeOutSpec = null,
                    placementSpec =
                        if (dragging || settling) {
                            null
                        } else {
                            spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold)
                        },
                ).zIndex(if (dragging || settling || raised) 1f else 0f)
                .then(modifier)
                // Read at draw time, so following the finger doesn't recompose the row.
                .graphicsLayer {
                    val lift = liftState.value
                    translationY = dragDropState.translationOf(key)
                    scaleX = 1f + LIFT_SCALE * lift
                    scaleY = 1f + LIFT_SCALE * lift
                    shadowElevation = LIFT_ELEVATION.toPx() * lift
                    ambientShadowColor = shadowColor
                    spotShadowColor = shadowColor
                    this.shape = shape
                    clip = false
                }.drawBehind {
                    val lift = liftState.value
                    if (lift > 0f) {
                        val radius = shape.topStart.toPx(size, this)
                        drawRoundRect(liftColor, cornerRadius = CornerRadius(radius), alpha = lift)
                    }
                },
    ) {
        content(dragging)
    }
}

private const val LIFT_SCALE = 0.03f
private val LIFT_ELEVATION = 8.dp
private val SOFT_SHADOW = Color.Black.copy(alpha = 0.35f)

/**
 * The lift for rows drawn over the cover or a blur: a veil in the rows' own [content] colour, so the
 * cover still shows through and the row only reads as picked up.
 */
fun translucentLift(content: Color): Color = content.copy(alpha = 0.12f)

/**
 * The lift for rows on a solid [background]: that background a step towards the rows' [content]
 * colour, as Material's tonal elevation does, so the hue stays the list's own.
 */
fun tonalLift(
    background: Color,
    content: Color,
): Color = content.copy(alpha = 0.08f).compositeOver(background)

/**
 * The handle at the end of a reorderable row. It starts the move as soon as it is touched, no long
 * press, and keeps the pointer to itself until it lifts, so the row's swipe, the list's scroll and
 * the sheet underneath never see the drag. [onMoveUp] and [onMoveDown] are the same move offered to
 * accessibility services, so reordering never depends on dragging; null where there is nowhere to go.
 */
@Composable
fun ReorderHandle(
    dragDropState: DragDropState,
    /** The row's current lazy index. */
    index: Int,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val currentIndex by rememberUpdatedState(index)
    val coordinates = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val label = stringResource(Res.string.queue_reorder)
    val upLabel = stringResource(Res.string.move_up)
    val downLabel = stringResource(Res.string.move_down)
    Box(
        modifier
            .size(48.dp)
            .onGloballyPositioned { coordinates[0] = it }
            .pointerInput(dragDropState) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val owner = Any()
                    if (!dragDropState.onDragStartAt(currentIndex, owner)) return@awaitEachGesture
                    // Taken here so the row's tap doesn't play the song when the handle is let go.
                    down.consume()
                    // The handle rides on the row it moves: measured in its own coordinates the
                    // finger would barely move, so distances are taken in the root's.
                    var lastY = coordinates[0]?.takeIf { it.isAttached }?.localToRoot(down.position)?.y ?: down.position.y
                    var dropped = false
                    try {
                        dropped =
                            drag(down.id) { change ->
                                val y = coordinates[0]?.takeIf { it.isAttached }?.localToRoot(change.position)?.y ?: lastY
                                dragDropState.onDragAndScroll(Offset(0f, y - lastY), owner)
                                lastY = y
                                change.consume()
                            }
                        currentEvent.changes.forEach { it.consume() }
                    } finally {
                        dragDropState.onDragInterrupted(end = dropped, owner = owner)
                    }
                }
            }.semantics {
                contentDescription = label
                customActions =
                    listOfNotNull(
                        onMoveUp?.let { up ->
                            CustomAccessibilityAction(upLabel) {
                                up()
                                true
                            }
                        },
                        onMoveDown?.let { down ->
                            CustomAccessibilityAction(downLabel) {
                                down()
                                true
                            }
                        },
                    )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(LimusicIcons.DragHandle, contentDescription = null)
    }
}

/**
 * Hosts a list whose long-press reorder sits in [gesture], with a quicker long press than the
 * system's: the reorder is the only long press there, and half a second felt like nothing happens.
 * A delay the user lengthened in the accessibility settings is kept. Only [gesture] sees the change:
 * [content] gets the system's back, so nothing in the rows presses for less.
 */
@Composable
fun QuickLongPressBox(
    gesture: Modifier,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val system = LocalViewConfiguration.current
    val quick =
        remember(system) {
            if (system.longPressTimeoutMillis > DEFAULT_LONG_PRESS_MS) system else QuickLongPressConfiguration(system)
        }
    CompositionLocalProvider(LocalViewConfiguration provides quick) {
        Box(modifier.then(gesture)) {
            CompositionLocalProvider(LocalViewConfiguration provides system) { content() }
        }
    }
}

private const val QUICK_LONG_PRESS_MS = 300L
private const val DEFAULT_LONG_PRESS_MS = 500L

@OptIn(ExperimentalComposeUiApi::class)
private class QuickLongPressConfiguration(
    system: ViewConfiguration,
) : ViewConfiguration by system {
    override val longPressTimeoutMillis: Long = minOf(QUICK_LONG_PRESS_MS, system.longPressTimeoutMillis)
}

/**
 * A drag reorders the rows on screen as it goes ([arrange]) and tells the caller only once, on the
 * drop ([onSwap]): one move for the player, not one per row passed. The new order stays on screen
 * until the caller's list comes back with it, so the rows don't jump back and forth meanwhile.
 *
 * Every drag has an owner (the list's long press, or a row's handle): only the owner moves or ends
 * it, so the list's long press firing under a held handle can't take the drag over or cancel it.
 */
class DragDropState internal constructor(
    val state: LazyListState,
    private val scope: CoroutineScope,
) {
    internal var onSwap: (Int, Int) -> Unit = { _, _ -> }
    internal var canMoveTo: (Int) -> Boolean = { true }

    private var owner: Any? = null
    private var draggedDistance by mutableFloatStateOf(0f)
    private var draggingItemInitialOffset by mutableIntStateOf(0)

    // used to obtain initial offsets on drag start
    private var initiallyDraggedElement by mutableStateOf<LazyListItemInfo?>(null)

    var currentIndexOfDraggedItem by mutableStateOf<Int?>(null)
        private set
    internal var draggingKey by mutableStateOf<Any?>(null)
        private set

    // The move on screen: the row's index before the drag, and where it is now.
    private var move by mutableStateOf<Pair<Int, Int>?>(null)

    // The caller's list as [arrange] last saw it, and as it was at the drop.
    private var lastSource: List<Any?>? = null
    private var movedSource: List<Any?>? = null

    // The dropped row on its way into its slot: where it was let go, and how much of that is left.
    internal var settlingKey by mutableStateOf<Any?>(null)
        private set
    private var settleFrom = 0f
    private val settleProgress = Animatable(0f)
    private var generation = 0
    private var overscrollJob: Job? = null

    /** The caller's list in the order on screen: [source] with the row being dragged moved. [base] is the first row's lazy index. */
    fun <T> arrange(
        source: List<T>,
        base: Int = 0,
    ): List<T> {
        val (from, to) = move ?: return source
        val dragging = currentIndexOfDraggedItem != null
        if (dragging && lastSource != null && lastSource != source) {
            // The queue changed under the drag (a song ended, the host moved it): the indexes no
            // longer name the rows they did, so drop it rather than move the wrong song.
            scope.launch { cancel() }
            return source
        }
        lastSource = source
        // Dropped, and the caller's list has come back changed: that is the new order.
        if (!dragging && source != movedSource) return source
        val f = from - base
        val t = to - base
        if (f == t || f !in source.indices || t !in source.indices) return source
        return source.toMutableList().apply { add(t, removeAt(f)) }
    }

    /** The lazy item under [offset], a position in the list's own coordinates. */
    fun itemAt(offset: Offset): LazyListItemInfo? {
        // Item offsets start after the top content padding; the pointer's start at the list's edge.
        val y = offset.y.toInt() - state.layoutInfo.beforeContentPadding
        return state.layoutInfo.visibleItemsInfo.firstOrNull { item -> y in item.offset..item.offsetEnd }
    }

    fun onDragStart(
        offset: Offset,
        owner: Any = LIST,
    ): Boolean = itemAt(offset)?.let { start(it, owner) } ?: false

    fun onDragStartAt(
        index: Int,
        owner: Any,
    ): Boolean = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }?.let { start(it, owner) } ?: false

    private fun start(
        item: LazyListItemInfo,
        owner: Any,
    ): Boolean {
        if (this.owner != null || !canMoveTo(item.index)) return false
        // The last drop not yet back from the caller: these indexes are still the old list's.
        if (move != null && lastSource == movedSource) return false
        this.owner = owner
        generation++
        settlingKey = null
        currentIndexOfDraggedItem = item.index
        draggingKey = item.key
        initiallyDraggedElement = item
        draggingItemInitialOffset = item.offset
        draggedDistance = 0f
        lastSource = null
        move = item.index to item.index
        return true
    }

    fun onDragInterrupted(
        end: Boolean = false,
        owner: Any = LIST,
    ) {
        if (owner !== this.owner) return
        this.owner = null
        overscrollJob?.cancel()
        val (from, to) = move ?: (0 to 0)
        val committed = end && from != to && from >= 0 && to >= 0
        settleFrom = draggingItemInitialOffset + draggedDistance
        settlingKey = draggingKey
        if (committed) {
            Logger.d("QueueBottomSheet", "onDragInterrupted: $from, $to")
            movedSource = lastSource
            onSwap(from, to)
        } else {
            move = null
        }
        draggingItemInitialOffset = 0
        draggedDistance = 0f
        currentIndexOfDraggedItem = null
        draggingKey = null
        initiallyDraggedElement = null
        val drop = ++generation
        scope.launch {
            settleProgress.snapTo(1f)
            settleProgress.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
            if (generation == drop) settlingKey = null
        }
        if (committed) {
            scope.launch {
                // Should the caller's list never come back changed (a player that ignored the
                // move), show its order again rather than one that isn't real.
                delay(PENDING_TIMEOUT_MS)
                if (generation == drop) move = null
            }
        }
    }

    private fun cancel() {
        owner?.let { onDragInterrupted(end = false, owner = it) }
    }

    fun onDrag(
        offset: Offset,
        owner: Any = LIST,
    ) {
        if (owner !== this.owner) return
        draggedDistance += offset.y
        moveIfOverAnother()
    }

    /** [onDrag], then scrolls the list on while the row is held against its top or bottom edge. */
    fun onDragAndScroll(
        offset: Offset,
        owner: Any = LIST,
    ) {
        if (owner !== this.owner) return
        onDrag(offset, owner)
        if (overscrollJob?.isActive == true) return
        val over = checkForOverScroll()
        if (over != 0f) {
            overscrollJob =
                scope.launch {
                    state.animateScrollBy(over * 1.3f, tween(easing = FastOutLinearInEasing))
                    // The rows scrolled under a finger that stayed put.
                    moveIfOverAnother()
                }
        }
    }

    // Once the middle of the row in hand is over another row, it takes that row's place.
    private fun moveIfOverAnother() {
        val current = currentIndexOfDraggedItem ?: return
        val visible = state.layoutInfo.visibleItemsInfo
        val slot = visible.firstOrNull { it.index == current } ?: return
        val middle = (draggingItemInitialOffset + draggedDistance + slot.size / 2f).toInt()
        val target =
            visible.firstOrNull { it.index != current && middle in it.offset..it.offsetEnd && canMoveTo(it.index) }
                ?: return
        // The list keeps its first row's key in place: moving that row would scroll the list with it.
        if (current == state.firstVisibleItemIndex || target.index == state.firstVisibleItemIndex) {
            state.requestScrollToItem(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
        }
        move = move?.let { it.first to target.index }
        currentIndexOfDraggedItem = target.index
    }

    /** How far the row with [key] is drawn from its slot: under the finger while dragged, on its way home once dropped. */
    internal fun translationOf(key: Any): Float {
        val dragging = key == draggingKey
        if (!dragging && key != settlingKey) return 0f
        val slot = state.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return 0f
        return if (dragging) {
            draggingItemInitialOffset + draggedDistance - slot.offset
        } else {
            (settleFrom - slot.offset) * settleProgress.value
        }
    }

    fun checkForOverScroll(): Float {
        if (owner == null) return 0f
        return initiallyDraggedElement?.let {
            val startOffset = it.offset + draggedDistance
            val endOffset = it.offsetEnd + draggedDistance
            return@let when {
                draggedDistance > 0 -> (endOffset - state.layoutInfo.viewportEndOffset + 50f).takeIf { diff -> diff > 0 }
                draggedDistance < 0 -> (startOffset - state.layoutInfo.viewportStartOffset - 50f).takeIf { diff -> diff < 0 }
                else -> null
            }
        } ?: 0f
    }

    private companion object {
        // The owner of a drag started by the list's own long press.
        val LIST = Any()
        const val PENDING_TIMEOUT_MS = 2_000L
    }
}
