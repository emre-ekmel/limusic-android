package com.simohypers.limusic.ui.component

import com.simohypers.limusic.ui.theme.rounded
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.simohypers.limusic.domain.data.entities.AlbumEntity
import com.simohypers.limusic.domain.data.entities.ArtistEntity
import com.simohypers.limusic.domain.data.entities.DownloadState
import com.simohypers.limusic.domain.data.entities.LocalPlaylistEntity
import com.simohypers.limusic.domain.data.entities.PlaylistEntity
import com.simohypers.limusic.domain.data.entities.PodcastsEntity
import com.simohypers.limusic.domain.data.entities.SongEntity
import com.simohypers.limusic.domain.data.model.browse.album.Track
import com.simohypers.limusic.domain.data.model.searchResult.albums.AlbumsResult
import com.simohypers.limusic.domain.data.model.searchResult.artists.ArtistsResult
import com.simohypers.limusic.domain.data.model.searchResult.playlists.PlaylistsResult
import com.simohypers.limusic.domain.data.type.ArtistType
import com.simohypers.limusic.domain.data.type.PlaylistType
import com.simohypers.limusic.domain.repository.SongRepository
import com.simohypers.limusic.domain.utils.connectArtists
import com.simohypers.limusic.domain.utils.toListName
import com.simohypers.limusic.ui.icon.Add
import com.simohypers.limusic.ui.icon.Check
import com.simohypers.limusic.ui.icon.DownloadForOffline
import com.simohypers.limusic.ui.icon.DragHandle
import com.simohypers.limusic.ui.icon.MoreVert
import com.simohypers.limusic.ui.icon.PlayCircle
import com.simohypers.limusic.ui.icon.PushPin
import com.simohypers.limusic.ui.icon.QueueMusic
import com.simohypers.limusic.ui.icon.Suggested
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.theme.LocalForceDarkText
import com.simohypers.limusic.ui.theme.typo
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.add_to_queue
import limusic.composeapp.generated.resources.album
import limusic.composeapp.generated.resources.artists
import limusic.composeapp.generated.resources.drag_to_reorder
import limusic.composeapp.generated.resources.play_next
import limusic.composeapp.generated.resources.playlist
import limusic.composeapp.generated.resources.podcasts
import limusic.composeapp.generated.resources.radio
import limusic.composeapp.generated.resources.suggested_by_dynamic_shuffle
import limusic.composeapp.generated.resources.you
import kotlin.math.roundToInt

/**
 * This is the song item in the playlist or other places.
 *
 * Multi-selection is opt-in per screen: pass [onLongClick] to let a long press start it, then
 * drive [selectionMode] and [isSelected] from the screen's
 * [com.simohypers.limusic.ui.component.selection.TrackSelection].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongFullWidthItems(
    track: Track? = null,
    index: Int? = null,
    songEntity: SongEntity? = null,
    isPlaying: Boolean,
    shouldShowDragHandle: Boolean = false,
    // Gestures for the drag handle, so a screen can start a reorder from it.
    dragHandleModifier: Modifier = Modifier,
    onMoreClickListener: ((videoId: String) -> Unit)? = null,
    onClickListener: ((videoId: String) -> Unit)? = null,
    onAddToQueue: ((videoId: String) -> Unit)? = null,
    // The swipe towards the start side; null keeps the row from moving that way.
    onPlayNext: ((videoId: String) -> Unit)? = null,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onLongClick: ((videoId: String) -> Unit)? = null,
    onSelectToggle: ((videoId: String) -> Unit)? = null,
    modifier: Modifier,
    rightView: @Composable (() -> Unit)? = null,
    forceDark: Boolean = LocalForceDarkText.current,
    padding: PaddingValues = PaddingValues(start = 23.dp, end = 15.dp, top = 6.dp, bottom = 6.dp),
    // Before the ⋯, in the subtitle colour: a queue row's drag handle.
    trailingView: @Composable (() -> Unit)? = null,
    // Before the cover, inside the row's tap and tint: a chart's rank.
    leadingView: @Composable (() -> Unit)? = null,
    // A queue row dynamic shuffle lined up: a sparkle in front of the artist. Queue rows only.
    suggested: Boolean = false,
) {
    val contentColor = if (forceDark) Color.White else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (forceDark) Color(0xC4FFFFFF) else MaterialTheme.colorScheme.onSurfaceVariant
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val songRepository: SongRepository = koinInject<SongRepository>()
    val downloadState by songRepository
        .getSongAsFlow(songEntity?.videoId ?: track?.videoId ?: "")
        .mapNotNull { it?.downloadState }
        .collectAsState(initial = DownloadState.STATE_NOT_DOWNLOADED)
    val itemVideoId = track?.videoId ?: songEntity?.videoId ?: ""
    val accent = MaterialTheme.colorScheme.primary
    // Picking fades the row in and pops the tick, rather than switching in one frame.
    val rowTint by animateColorAsState(if (isSelected) accent.copy(alpha = 0.18f) else Color.Transparent, label = "rowTint")
    val tickFill by animateColorAsState(if (isSelected) accent else Color.Transparent, label = "tickFill")
    val tickRing by animateColorAsState(if (isSelected) accent else contentColor.copy(alpha = 0.6f), label = "tickRing")

    SwipeToQueueBox(
        videoId = itemVideoId,
        // The playing row and a row being picked stay put.
        enabled = !isPlaying && !selectionMode,
        onAddToQueue = onAddToQueue,
        onPlayNext = onPlayNext,
        iconTint = contentColor,
        modifier = modifier,
        rowModifier =
            Modifier
                .background(
                    // The theme's accent: the artwork's while "Adapt colors to artwork" is on.
                    rowTint,
                ).combinedClickable(
                    onClick = {
                        if (selectionMode) {
                            onSelectToggle?.invoke(itemVideoId)
                        } else {
                            onClickListener?.invoke(itemVideoId)
                        }
                    },
                    onLongClick =
                        if (onLongClick != null) {
                            { onLongClick(itemVideoId) }
                        } else {
                            null
                        },
                ).animateContentSize(),
    ) {
        Row(
            Modifier
                .padding(padding)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = selectionMode,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(tickFill)
                                .border(
                                    width = 1.5.dp,
                                    color = tickRing,
                                    shape = CircleShape,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isSelected,
                            enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)) + fadeIn(),
                            exit = scaleOut() + fadeOut(),
                        ) {
                            Icon(
                                imageVector = LimusicIcons.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }
            leadingView?.invoke()
            // A track number needs less room than a cover.
            Box(
                modifier = Modifier.size(if (index == null) 48.dp else 32.dp, 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (index == null) {
                    // The cover stays on the playing row; the bars play over it.
                    val thumb = track?.thumbnails?.lastOrNull()?.url ?: songEntity?.thumbnails
                    AsyncImage(
                        model =
                            ImageRequest
                                .Builder(LocalPlatformContext.current)
                                .data(thumb)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .diskCacheKey(thumb)
                                .crossfade(true)
                                .build(),
                        placeholder = rememberHolderPainter(),
                        error = rememberHolderPainter(),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .clip(rounded(4.dp)),
                    )
                }
                Crossfade(isPlaying) {
                    if (it && index == null) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clip(rounded(4.dp))
                                .background(Color.Black.copy(alpha = 0.45f)),
                        ) {
                            AudioPlayingIndicator(
                                modifier = Modifier.fillMaxSize().padding(10.dp),
                                color = Color.White,
                            )
                        }
                    } else if (it) {
                        AudioPlayingIndicator(
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else if (index == null) {
                        Spacer(Modifier.fillMaxSize())
                    } else {
                        Text(
                            text = (index + 1).toString(),
                            color = contentColor,
                            style = typo().titleMedium,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 10.dp)
                    .align(Alignment.CenterVertically),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                Text(
                    text = track?.title ?: songEntity?.title ?: "",
                    style = typo().titleSmall,
                    maxLines = 1,
                    color = contentColor,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(
                        visible =
                            if (songEntity != null || track != null) {
                                downloadState == DownloadState.STATE_DOWNLOADED
                            } else {
                                false
                            },
                    ) {
                        Row {
                            Icon(
                                imageVector = LimusicIcons.DownloadForOffline,
                                tint = contentColor,
                                contentDescription = "",
                                modifier = Modifier.size(16.dp).padding(2.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                    AnimatedVisibility(
                        visible =
                            songEntity?.isExplicit
                                ?: (track?.isExplicit ?: false),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ExplicitBadge(
                                modifier = Modifier.size(20.dp).padding(1.dp),
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                    }
                    // Where the explicit mark goes: it says where the track came from, and the
                    // title keeps its whole width. Read out with the row.
                    if (suggested) {
                        Icon(
                            imageVector = LimusicIcons.Suggested,
                            contentDescription = stringResource(Res.string.suggested_by_dynamic_shuffle),
                            tint = if (forceDark) contentColor else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 4.dp).size(12.dp),
                        )
                    }
                    Text(
                        text =
                            (
                                track?.artists?.toListName()?.connectArtists()
                                    ?: songEntity?.artistName?.connectArtists()
                            ) ?: "",
                        style = typo().bodySmall,
                        maxLines = 1,
                        color = subtitleColor,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = MarqueeAnimationMode.Immediately,
                                ).focusable(),
                    )
                }
            }
            if (rightView != null) {
                rightView()
            }
            // The drag handle sits left of the ⋯, so the ⋯ stays where it is on every row.
            AnimatedVisibility(shouldShowDragHandle, enter = handleEnter(isRtl), exit = handleExit(isRtl)) {
                // 48dp tall so the handle is easy to grab with a thumb.
                Box(
                    modifier = Modifier.size(width = DragHandleWidth, height = 48.dp).then(dragHandleModifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        LimusicIcons.DragHandle,
                        contentDescription = stringResource(Res.string.drag_to_reorder),
                        tint = contentColor,
                    )
                }
            }
            if (trailingView != null) {
                CompositionLocalProvider(LocalContentColor provides subtitleColor) { trailingView() }
            }
            // Hidden while selecting: the per-item menu moves up to the selection app bar,
            // so one tap cannot mean both "act on this song" and "pick this song".
            if (onMoreClickListener != null && !selectionMode) {
                RippleIconButton(imageVector = LimusicIcons.MoreVert, fillMaxSize = false, tint = contentColor) {
                    if (itemVideoId.isNotBlank()) onMoreClickListener.invoke(itemVideoId)
                }
            }
        }
    }
}

/**
 * The song rows' swipe: towards the end side adds [videoId] to the queue, towards the start side
 * plays it next. Each side moves only when it has an action, and screen readers get both as
 * actions, since the swipe is the row's only way to them.
 *
 * [rowModifier] is the moving layer's own look and tap. It sits before the swipe detector, so a
 * drag that passes the touch slop sideways is consumed and cancels the tap; a vertical drag is
 * never claimed, so the list around the row still scrolls.
 */
@Composable
internal fun SwipeToQueueBox(
    videoId: String,
    enabled: Boolean,
    onAddToQueue: ((videoId: String) -> Unit)?,
    onPlayNext: ((videoId: String) -> Unit)?,
    iconTint: Color,
    modifier: Modifier = Modifier,
    rowModifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    // In dp so the swipe needs the same finger travel on every screen density.
    val maxOffset = with(density) { 130.dp.toPx() }
    // The detector outlives recompositions, so it reads the latest callbacks and id through these.
    val currentOnAddToQueue by rememberUpdatedState(onAddToQueue)
    val currentOnPlayNext by rememberUpdatedState(onPlayNext)
    val currentVideoId by rememberUpdatedState(videoId)
    // offsetX counts towards the end side (negative: towards the start), which offset() already mirrors;
    // drag amounts do not mirror.
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val offsetX = remember { Animatable(initialValue = 0f) }
    var heightDp by remember { mutableStateOf(0.dp) }

    Box(
        modifier =
        modifier,
    ) {
        Crossfade(
            offsetX.value >= maxOffset / 2,
        ) { shouldShowAddToQueue ->
            if (shouldShowAddToQueue) {
                Box(
                    modifier =
                        Modifier
                            .height(heightDp)
                            .aspectRatio(1f)
                            .padding(start = 15.dp)
                            .align(Alignment.CenterStart),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        tint = iconTint,
                        imageVector = LimusicIcons.QueueMusic,
                        contentDescription = stringResource(Res.string.add_to_queue),
                    )
                }
            }
        }
        // On the end side: the side the row moves away from when swiped towards the start.
        Crossfade(
            offsetX.value <= -maxOffset / 2,
            modifier = Modifier.align(Alignment.CenterEnd),
        ) { shouldShowPlayNext ->
            if (shouldShowPlayNext) {
                Box(
                    modifier =
                        Modifier
                            .height(heightDp)
                            .aspectRatio(1f)
                            .padding(end = 15.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        tint = iconTint,
                        imageVector = LimusicIcons.PlayCircle,
                        contentDescription = stringResource(Res.string.play_next),
                    )
                }
            }
        }
        val addToQueueLabel = stringResource(Res.string.add_to_queue)
        val playNextLabel = stringResource(Res.string.play_next)
        val swipeActions =
            if (!enabled) {
                emptyList()
            } else {
                listOfNotNull(
                    onAddToQueue?.let { action ->
                        CustomAccessibilityAction(addToQueueLabel) {
                            action(videoId)
                            true
                        }
                    },
                    onPlayNext?.let { action ->
                        CustomAccessibilityAction(playNextLabel) {
                            action(videoId)
                            true
                        }
                    },
                )
            }
        Box(
            // The moving layer starts from the same modifier as the row, as it always has.
            modifier =
                modifier
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .then(rowModifier)
                    .semantics { if (swipeActions.isNotEmpty()) customActions = swipeActions }
                    // Keyed on every flag the body reads, so the swipe detector is torn down when
                    // the row stops swiping (selection starts, it starts playing) and comes back after.
                    .pointerInput(enabled, onAddToQueue != null, onPlayNext != null, isRtl) {
                        val canAddToQueue = onAddToQueue != null
                        val canPlayNext = onPlayNext != null
                        if (enabled && (canAddToQueue || canPlayNext)) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, dragAmount ->
                                    // Towards the end side: rightward in LTR, leftward in RTL.
                                    val towardsEnd = if (isRtl) -dragAmount else dragAmount
                                    val next = offsetX.value + towardsEnd
                                    // Each side moves only when it has an action to fire.
                                    if ((next > 0 && canAddToQueue) || (next < 0 && canPlayNext)) {
                                        change.consume()
                                        coroutineScope.launch {
                                            offsetX.snapTo(
                                                (offsetX.value + towardsEnd).coerceIn(-maxOffset, maxOffset),
                                            )
                                        }
                                    }
                                },
                                onDragEnd = {
                                    // At or past either end, not only exactly on it: the last snapTo may still be in flight.
                                    if (offsetX.value >= maxOffset) {
                                        currentOnAddToQueue?.invoke(currentVideoId)
                                    } else if (offsetX.value <= -maxOffset) {
                                        currentOnPlayNext?.invoke(currentVideoId)
                                    }
                                    coroutineScope.launch {
                                        offsetX.animateTo(0f)
                                    }
                                },
                                // A cancelled swipe would otherwise leave the row shifted.
                                onDragCancel = {
                                    coroutineScope.launch {
                                        offsetX.animateTo(0f)
                                    }
                                },
                            )
                        }
                    }.onGloballyPositioned { coordinates ->
                        with(density) {
                            heightDp = coordinates.size.height.toDp()
                        }
                    },
            content = content,
        )
    }
}

@Composable
fun SuggestItems(
    track: Track,
    isPlaying: Boolean,
    onClickListener: (() -> Unit)? = null,
    onAddClickListener: (() -> Unit)? = null,
    forceDark: Boolean = LocalForceDarkText.current,
) {
    val contentColor = if (forceDark) Color.White else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (forceDark) Color(0xC4FFFFFF) else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier =
            Modifier
                .clickable {
                    if (onClickListener != null) {
                        onClickListener()
                    }
                }.animateContentSize(),
    ) {
        Row(
            Modifier
                .padding(vertical = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(40.dp)) {
                Crossfade(isPlaying) {
                    if (it) {
                        AudioPlayingIndicator(
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        val thumb = track.thumbnails?.lastOrNull()?.url
                        AsyncImage(
                            model =
                                ImageRequest
                                    .Builder(LocalPlatformContext.current)
                                    .data(thumb)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .diskCacheKey(thumb)
                                    .crossfade(true)
                                    .build(),
                            placeholder = rememberHolderPainter(),
                            error = rememberHolderPainter(),
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier =
                                Modifier
                                    .wrapContentHeight()
                                    .fillMaxWidth()
                                    .clip(rounded(4.dp)),
                        )
                    }
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 10.dp)
                    .align(Alignment.CenterVertically),
            ) {
                Text(
                    text = track.title,
                    style = typo().titleSmall,
                    maxLines = 1,
                    color = contentColor,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable(),
                )
                Text(
                    text =
                        (
                            track.artists?.toListName()?.connectArtists()
                        ) ?: "",
                    style = typo().bodySmall,
                    maxLines = 1,
                    color = subtitleColor,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable(),
                )
            }
            RippleIconButton(
                imageVector = LimusicIcons.Add,
                fillMaxSize = false,
                onClick =
                    onAddClickListener ?: {
                    },
            )
        }
    }
}

@Composable
fun PlaylistFullWidthItems(
    data: PlaylistType,
    onClickListener: (() -> Unit)? = null,
    rightView: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    forceDark: Boolean = LocalForceDarkText.current,
) {
    val contentColor = if (forceDark) Color.White else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (forceDark) Color(0xC4FFFFFF) else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier =
            modifier
                .clickable {
                    onClickListener?.invoke()
                }.animateContentSize(),
    ) {
        var title = ""
        var thumb = ""
        var firstSubtitle = ""
        var secondSubtitle = ""
        var thirdRowSubtitle: String? = null

        var shouldPin = false

        firstSubtitle =
            when (data.playlistType()) {
                PlaylistType.Type.YOUTUBE_PLAYLIST -> stringResource(Res.string.playlist)
                PlaylistType.Type.RADIO -> stringResource(Res.string.radio)
                PlaylistType.Type.LOCAL -> stringResource(Res.string.playlist)
                PlaylistType.Type.ALBUM -> stringResource(Res.string.album)
                PlaylistType.Type.PODCAST -> stringResource(Res.string.podcasts)
            }
        when (data) {
            is AlbumEntity -> {
                title = data.title
                thumb = data.thumbnails ?: ""
                secondSubtitle = data.artistName?.connectArtists() ?: ""
                thirdRowSubtitle = data.year
            }

            is PlaylistEntity -> {
                title = data.title
                thumb = data.thumbnails
                secondSubtitle = data.author ?: ""
                if (data.description == "PIN") { // LIKED MUSIC
                    shouldPin = true
                }
            }

            is LocalPlaylistEntity -> {
                title = data.title
                thumb = data.thumbnail ?: ""
                secondSubtitle = stringResource(Res.string.you)
            }

            is PlaylistsResult -> {
                title = data.title
                thumb = data.thumbnails.lastOrNull()?.url ?: ""
                secondSubtitle = data.author
            }

            is AlbumsResult -> {
                title = data.title
                thumb = data.thumbnails.lastOrNull()?.url ?: ""
                secondSubtitle = data.artists.toListName().connectArtists()
                thirdRowSubtitle = data.year
            }

            is PodcastsEntity -> {
                title = data.title
                thumb = data.thumbnail ?: ""
                secondSubtitle = data.authorName
                thirdRowSubtitle = data.description
            }
        }
        Row(
            Modifier
                .padding(vertical = 10.dp, horizontal = 15.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.size(48.dp)) {
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalPlatformContext.current)
                            .data(thumb)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(thumb)
                            .crossfade(true)
                            .build(),
                    placeholder = rememberHolderPainter(),
                    error = rememberHolderPainter(),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clip(rounded(4.dp)),
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 10.dp)
                    .align(Alignment.CenterVertically),
            ) {
                Text(
                    text = title,
                    style = typo().titleSmall,
                    maxLines = 1,
                    color = contentColor,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable(),
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (shouldPin) {
                        Image(
                            imageVector = LimusicIcons.PushPin,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(if (forceDark) Color.Cyan else MaterialTheme.colorScheme.primary),
                            modifier =
                                Modifier
                                    .rotate(30f)
                                    .size(16.dp),
                        )
                    }
                    Text(
                        text = "$firstSubtitle ${if (secondSubtitle.isNotEmpty()) " • $secondSubtitle" else ""}",
                        style = typo().bodySmall,
                        maxLines = 1,
                        color = subtitleColor,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = MarqueeAnimationMode.Immediately,
                                ).focusable(),
                    )
                }

                if (thirdRowSubtitle != null) {
                    Text(
                        text = thirdRowSubtitle,
                        style = typo().bodySmall,
                        maxLines = 1,
                        color = subtitleColor,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = MarqueeAnimationMode.Immediately,
                                ).focusable(),
                    )
                }
            }
            if (rightView != null) {
                rightView()
            }
        }
    }
}

@Composable
fun ArtistFullWidthItems(
    data: ArtistType,
    // YouTube's own second line ("12 songs", "1.2M subscribers"); "Artists" without one.
    subtitle: String? = null,
    onClickListener: (() -> Unit)? = null,
    rightView: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    forceDark: Boolean = LocalForceDarkText.current,
) {
    val contentColor = if (forceDark) Color.White else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (forceDark) Color(0xC4FFFFFF) else MaterialTheme.colorScheme.onSurfaceVariant
    val (name: String, thumbnails: String?) =
        when (data) {
            is ArtistEntity -> Pair(data.name, data.thumbnails)
            is ArtistsResult -> Pair(data.artist, data.thumbnails.lastOrNull()?.url)
            else -> Pair("", null)
        }
    val channelId =
        when (data) {
            is ArtistEntity -> data.channelId
            is ArtistsResult -> data.browseId
            else -> null
        }
    var menuOpen by remember { mutableStateOf(false) }
    if (menuOpen && channelId != null) ArtistMenu(channelId, onClose = { menuOpen = false })
    Box(
        modifier
            .combinedClickable(
                onLongClick = if (channelId != null) ({ menuOpen = true }) else null,
                onClick = { onClickListener?.invoke() },
            ),
    ) {
        Row(
            Modifier
                .padding(vertical = 10.dp, horizontal = 15.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.size(48.dp)) {
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalPlatformContext.current)
                            .data(thumbnails)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(thumbnails)
                            .crossfade(true)
                            .build(),
                    placeholder = rememberHolderPainter(),
                    error = rememberHolderPainter(),
                    contentDescription = null,
                    contentScale = ContentScale.FillHeight,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 10.dp)
                    .align(Alignment.CenterVertically),
            ) {
                Text(
                    text = name,
                    style = typo().titleSmall,
                    maxLines = 1,
                    color = contentColor,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable(),
                )

                Text(
                    text = subtitle ?: stringResource(Res.string.artists),
                    style = typo().bodySmall,
                    maxLines = 1,
                    color = subtitleColor,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately,
                            ).focusable(),
                )
            }
            if (rightView != null) {
                rightView()
            }
        }
    }
}
