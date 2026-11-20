package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.CommunityPost
import com.example.domain.ExportUtils
import com.example.domain.GroundPitchType
import com.example.domain.PostType
import com.example.ui.components.CricketEmptyStateCard
import com.example.ui.components.CricketErrorCard
import com.example.ui.components.CricketLoadingCard
import com.example.ui.components.TeamCrestBadge
import com.example.viewmodel.CommunityViewModel
import com.example.viewmodel.UiState
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommunityScreen(
    currentUserId: String,
    currentUserName: String,
    currentUserPhoto: String?,
    communityViewModel: CommunityViewModel,
    onOpenMatchCenter: (String) -> Unit
) {
    val context = LocalContext.current
    val state by communityViewModel.communityState.collectAsStateWithLifecycle()
    val searchQuery by communityViewModel.searchQuery.collectAsStateWithLifecycle()
    val message by communityViewModel.message.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var discoverCategory by remember { mutableStateOf("PLAYERS") }
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var showAddGroundDialog by remember { mutableStateOf(false) }
    var activeCommentPost by remember { mutableStateOf<CommunityPost?>(null) }
    var reportingPost by remember { mutableStateOf<CommunityPost?>(null) }

    val voiceSearchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                communityViewModel.updateSearchQuery(spoken)
                selectedTab = 1
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showCreatePostDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("create_post_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Publish Post")
                }
            } else if (selectedTab == 1 && discoverCategory == "GROUNDS") {
                FloatingActionButton(
                    onClick = { showAddGroundDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_ground_fab")
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Add Ground")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val unreadCount = (state as? UiState.Success)?.data?.notifications?.count { !it.read } ?: 0
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Feed") },
                    modifier = Modifier.testTag("community_tab_feed")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Discover") },
                    modifier = Modifier.testTag("community_tab_discover")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(if (unreadCount > 0) "Alerts ($unreadCount)" else "Alerts") },
                    modifier = Modifier.testTag("community_tab_alerts")
                )
            }

            if (message != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(message!!, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        TextButton(onClick = { communityViewModel.clearMessage() }) { Text("OK") }
                    }
                }
            }

            when (val ui = state) {
                is UiState.Loading -> CricketLoadingCard("Loading Cricket Community...")
                is UiState.Error -> BoxError(ui.message)
                is UiState.Success -> {
                    val data = ui.data
                    when (selectedTab) {
                        0 -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (data.posts.isEmpty()) {
                                    item {
                                        CricketEmptyStateCard(
                                            title = "Cricket News & Feed",
                                            subtitle = "Share match reports, milestones, and cricket news with your club.",
                                            actionLabel = "Publish First Post",
                                            onAction = { showCreatePostDialog = true }
                                        )
                                    }
                                } else {
                                    items(data.posts, key = { it.id }) { post ->
                                        PostCard(
                                            post = post,
                                            onLike = { communityViewModel.toggleLike(post) },
                                            onComment = { activeCommentPost = post },
                                            onShare = {
                                                ExportUtils.shareText(
                                                    context,
                                                    "CricketPro Post by ${post.authorName}",
                                                    "${post.matchHeadline ?: post.postTypeEnum.label}\n\n${post.content}"
                                                )
                                            },
                                            onReport = { reportingPost = post },
                                            onOpenMatch = { post.matchId?.let(onOpenMatchCenter) }
                                        )
                                    }
                                }
                            }
                        }

                        1 -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { communityViewModel.updateSearchQuery(it) },
                                        label = { Text("Search players, teams, or grounds") },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = {
                                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                        putExtra(
                                                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                                        )
                                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Search cricket players, teams or grounds")
                                                    }
                                                    runCatching { voiceSearchLauncher.launch(intent) }
                                                },
                                                modifier = Modifier.testTag("voice_search_button")
                                            ) {
                                                Icon(Icons.Default.Mic, contentDescription = "Voice Search")
                                            }
                                        },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("discover_search_input")
                                    )
                                }

                                item {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("PLAYERS" to "Players", "TEAMS" to "Teams", "GROUNDS" to "Grounds").forEach { (key, label) ->
                                            FilterChip(
                                                selected = discoverCategory == key,
                                                onClick = { discoverCategory = key },
                                                label = { Text(label) }
                                            )
                                        }
                                    }
                                }

                                val q = searchQuery.trim().lowercase(Locale.US)
                                when (discoverCategory) {
                                    "PLAYERS" -> {
                                        val filtered = data.players.filter {
                                            q.isBlank() || it.displayName.lowercase(Locale.US).contains(q) ||
                                                it.handle.lowercase(Locale.US).contains(q) ||
                                                (it.city?.lowercase(Locale.US)?.contains(q) == true)
                                        }
                                        if (filtered.isEmpty()) {
                                            item { CricketEmptyStateCard("No Players Found", "Try another search query.") }
                                        } else {
                                            items(filtered, key = { it.userId }) { player ->
                                                val following = data.follows.any { it.targetId == player.userId && it.targetType == "PLAYER" }
                                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                    ) {
                                                        TeamCrestBadge(
                                                            shortName = player.displayName.take(2),
                                                            logoData = player.photoData,
                                                            size = 44.dp
                                                        )
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(player.displayName, style = MaterialTheme.typography.titleSmall)
                                                            Text(
                                                                "${player.playingRole} • ${player.runsScored} runs • ${player.wicketsTaken} wkts",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                        if (player.userId != currentUserId) {
                                                            FilledTonalButton(
                                                                onClick = {
                                                                    communityViewModel.toggleFollow(player.userId, "PLAYER", player.displayName)
                                                                }
                                                            ) {
                                                                Text(if (following) "Following" else "Follow")
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    "TEAMS" -> {
                                        val filtered = data.teams.filter {
                                            q.isBlank() || it.name.lowercase(Locale.US).contains(q) ||
                                                it.shortName.lowercase(Locale.US).contains(q)
                                        }
                                        if (filtered.isEmpty()) {
                                            item { CricketEmptyStateCard("No Teams Found", "Create a team in the Hub tab.") }
                                        } else {
                                            items(filtered, key = { it.id }) { team ->
                                                val following = data.follows.any { it.targetId == team.id && it.targetType == "TEAM" }
                                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                    ) {
                                                        TeamCrestBadge(shortName = team.shortName, logoData = team.logoData, size = 44.dp)
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(team.name, style = MaterialTheme.typography.titleSmall)
                                                            Text(
                                                                "Capt: ${team.captainName} • ${team.wins}W-${team.losses}L",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                        FilledTonalButton(
                                                            onClick = {
                                                                communityViewModel.toggleFollow(team.id, "TEAM", team.name)
                                                            }
                                                        ) {
                                                            Text(if (following) "Following" else "Follow")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    else -> {
                                        item {
                                            OutlinedButton(
                                                onClick = { showAddGroundDialog = true },
                                                modifier = Modifier.fillMaxWidth().testTag("discover_add_ground_btn")
                                            ) {
                                                Icon(Icons.Default.LocationOn, contentDescription = null)
                                                Spacer(Modifier.width(6.dp))
                                                Text("Register Cricket Ground")
                                            }
                                        }
                                        val filtered = data.grounds.filter {
                                            q.isBlank() || it.name.lowercase(Locale.US).contains(q) ||
                                                it.city.lowercase(Locale.US).contains(q)
                                        }
                                        if (filtered.isEmpty()) {
                                            item { CricketEmptyStateCard("No Grounds Registered", "Add a local cricket ground to the directory.") }
                                        } else {
                                            items(filtered, key = { it.id }) { ground ->
                                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text(ground.name, style = MaterialTheme.typography.titleSmall)
                                                            Text(
                                                                "${ground.city} • ${ground.pitchType} Pitch",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                        if (ground.floodlights) {
                                                            Text("Floodlights", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (data.notifications.isEmpty()) {
                                    item {
                                        CricketEmptyStateCard(
                                            title = "No Alerts Yet",
                                            subtitle = "Match updates, tournament announcements, and admin broadcasts appear here."
                                        )
                                    }
                                } else {
                                    items(data.notifications, key = { it.id }) { notif ->
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (notif.read) MaterialTheme.colorScheme.surfaceContainer
                                                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { communityViewModel.markNotificationRead(notif.id) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.Top,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Notifications,
                                                    contentDescription = null,
                                                    tint = if (notif.read) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                                                )
                                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(notif.title, style = MaterialTheme.typography.titleSmall)
                                                    Text(notif.body, style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePostDialog) {
        CreatePostDialog(
            onDismiss = { showCreatePostDialog = false },
            onPublish = { type, content ->
                communityViewModel.createPost(currentUserName, currentUserPhoto, content, type)
                showCreatePostDialog = false
            }
        )
    }

    if (showAddGroundDialog) {
        AddGroundDialog(
            onDismiss = { showAddGroundDialog = false },
            onAdd = { name, city, pitch, lights ->
                communityViewModel.addGround(name, city, pitch, lights)
                showAddGroundDialog = false
            }
        )
    }

    if (activeCommentPost != null) {
        CommentsDialog(
            post = activeCommentPost!!,
            communityViewModel = communityViewModel,
            currentUserName = currentUserName,
            onDismiss = { activeCommentPost = null }
        )
    }

    if (reportingPost != null) {
        ReportDialog(
            onDismiss = { reportingPost = null },
            onSubmit = { reason ->
                communityViewModel.submitReport(currentUserName, "POST", reportingPost!!.id, reason)
                reportingPost = null
            }
        )
    }
}

@Composable
private fun BoxError(message: String) {
    Column(modifier = Modifier.padding(16.dp)) { CricketErrorCard(message) }
}

@Composable
private fun PostCard(
    post: CommunityPost,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onReport: () -> Unit,
    onOpenMatch: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TeamCrestBadge(shortName = post.authorName.take(2), logoData = post.authorPhoto, size = 38.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(post.authorName, style = MaterialTheme.typography.titleSmall)
                    Text(post.postTypeEnum.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onReport) {
                    Icon(Icons.Default.Flag, contentDescription = "Report post", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }

            if (!post.matchHeadline.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.clickable { onOpenMatch() }
                ) {
                    Text(
                        text = post.matchHeadline,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Text(post.content, style = MaterialTheme.typography.bodyMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onLike) {
                    Icon(
                        imageVector = if (post.likesCount > 0) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like"
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("${post.likesCount}")
                }
                TextButton(onClick = onComment) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comments")
                    Spacer(Modifier.width(4.dp))
                    Text("${post.commentsCount}")
                }
                TextButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                    Spacer(Modifier.width(4.dp))
                    Text("Share")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreatePostDialog(
    onDismiss: () -> Unit,
    onPublish: (PostType, String) -> Unit
) {
    var postType by remember { mutableStateOf(PostType.UPDATE) }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Publish to Cricket Feed") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PostType.entries.forEach { t ->
                        FilterChip(
                            selected = postType == t,
                            onClick = { postType = t },
                            label = { Text(t.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Share match news, milestones or club updates...") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("post_content_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onPublish(postType, content) },
                enabled = content.trim().isNotEmpty(),
                modifier = Modifier.testTag("publish_post_button")
            ) {
                Text("Publish")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddGroundDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, GroundPitchType, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var pitchType by remember { mutableStateOf(GroundPitchType.TURF) }
    var floodlights by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Cricket Ground") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Ground Name") }, singleLine = true)
                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, singleLine = true)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GroundPitchType.entries.forEach { p ->
                        FilterChip(selected = pitchType == p, onClick = { pitchType = p }, label = { Text(p.label) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = floodlights, onCheckedChange = { floodlights = it })
                    Text("Floodlights Available")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, city, pitchType, floodlights) },
                enabled = name.trim().length >= 2 && city.trim().length >= 2
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CommentsDialog(
    post: CommunityPost,
    communityViewModel: CommunityViewModel,
    currentUserName: String,
    onDismiss: () -> Unit
) {
    val flow = remember(post.id) { communityViewModel.observeComments(post.id) }
    val commentsState by flow.collectAsStateWithLifecycle()
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Comments (${post.commentsCount})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (val ui = commentsState) {
                    is UiState.Loading -> Text("Loading comments...")
                    is UiState.Error -> Text(ui.message, color = MaterialTheme.colorScheme.error)
                    is UiState.Success -> {
                        if (ui.data.isEmpty()) {
                            Text("No comments yet. Be the first!", style = MaterialTheme.typography.bodySmall)
                        } else {
                            ui.data.takeLast(8).forEach { c ->
                                Column {
                                    Text(c.authorName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Text(c.text, style = MaterialTheme.typography.bodySmall)
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Write a comment") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    communityViewModel.addComment(post, currentUserName, text)
                    text = ""
                },
                enabled = text.trim().isNotEmpty()
            ) { Text("Send") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun ReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Content") },
        text = {
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Reason for moderation review") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onSubmit(reason) }, enabled = reason.trim().length >= 3) {
                Text("Submit")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
