package com.example.echospace.ui.home

//import androidx.compose.foundation.isSystemInDarkTheme
//import androidx.compose.material.icons.filled.Home
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.filled.Sort
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.TopAppBar
//import androidx.compose.runtime.rememberLauncherForActivityResult
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.unit.IntOffset
import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
//import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.ripple
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
//import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
//import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.echospace.R
import com.example.echospace.data.repository.DiaryRepositoryImpl
import com.example.echospace.domain.model.CreateFileResult
import com.example.echospace.domain.model.DiaryEntry
import com.example.echospace.domain.model.FolderItem
import com.example.echospace.domain.repository.DiaryRepository
import com.example.echospace.domain.repository.SortType
import com.example.echospace.ui.components.CreateEntryDialog
import com.example.echospace.ui.components.DeleteConfirmationDialog
import com.example.echospace.ui.components.FolderItemComponent
import com.example.echospace.ui.components.RenameEntryDialog
import com.example.echospace.ui.components.SlimTopAppBar
import com.example.echospace.ui.editor.EditorActivity
import com.example.echospace.utils.SortPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

//@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    diaryRepository: DiaryRepository,
    isDarkTheme: Boolean = false,
    onThemeToggle: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentPath by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var entryToRename by remember { mutableStateOf<DiaryEntry?>(null) }
    var entryToDelete by remember { mutableStateOf<DiaryEntry?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    var createDialogErrorMessage by remember { mutableStateOf("") }
    var showSortMenu by remember { mutableStateOf(false) }

    // Search state variables
    var isSearchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<FolderItem>>(emptyList()) }
    val searchFocusRequester = remember { FocusRequester() }

    val sortPreferences = remember { SortPreferences(context) }
    val currentSortType by sortPreferences.sortType.collectAsState(initial = SortType.NAME_ASC)

    // Handle back button for search mode
    BackHandler(enabled = isSearchMode) {
        isSearchMode = false
        searchQuery = ""
        searchResults = emptyList()
    }


    // Intercept system back when in a subfolder
    BackHandler(enabled = currentPath.isNotEmpty() && !isSearchMode) {
        currentPath = diaryRepository.getParentPath(currentPath) ?: ""
    }

    // Auto-focus search field when entering search mode
    LaunchedEffect(isSearchMode) {
        if (isSearchMode) {
            searchFocusRequester.requestFocus()
        }
    }

    // Perform search when query changes
    LaunchedEffect(searchQuery) {
        searchResults = if (isSearchMode && searchQuery.isNotBlank()) {
            diaryRepository.searchEntries(searchQuery, currentPath)
        } else {
            emptyList()
        }
    }

    // Recompute folderContents whenever currentPath or refreshTrigger changes
    // Compute folder contents for normal mode
    val folderContents by remember(currentPath, refreshTrigger, currentSortType, isSearchMode) {
        derivedStateOf {
            if (!isSearchMode) {
                diaryRepository.getFolderContentsSorted(currentPath, currentSortType)
            } else {
                emptyList()
            }
        }
    }

    // Function to close search mode
    fun closeSearch() {
        isSearchMode = false
        searchQuery = ""
        searchResults = emptyList()
    }

    // Observe Activity lifecycle to refresh when returning from editor
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Pull refresh function
    fun onRefresh() {
        coroutineScope.launch {
            isRefreshing = true
            // Simulate refresh delay (you can remove this delay in production)
            delay(400)
            // Trigger refresh by incrementing the trigger
            refreshTrigger++
            isRefreshing = false
            Toast.makeText(context, "Files refreshed", Toast.LENGTH_SHORT).show()
        }
    }

    // Pull refresh state
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = ::onRefresh
    )

    // Helper function to navigate to nearest parent with content
    fun navigateToNearestParentWithContent() {
        val nearestParent = (diaryRepository as DiaryRepositoryImpl)
            .findNearestParentWithContent(currentPath)
        currentPath = nearestParent
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra("new_path")?.let { newPath ->
                currentPath = newPath
            }
        }
        // Refresh folder contents regardless
        refreshTrigger++
    }

    Scaffold(
        topBar = {
            SlimTopAppBar(
                title = {
                    if (!isSearchMode) {
                        // Normal breadcrumb navigation
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Add spacer only when home icon is present
                            if (currentPath.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp)) // Adjust width as needed
                            }
                            // Assuming currentPath internally uses "#" as separator, e.g., "DIARY#2025#JUN"
                            val pathSegments =
                                if (currentPath.isNotEmpty()) currentPath.split('#') else emptyList()

                            if (pathSegments.isEmpty()) {
                                // If at root, display "EchoSpace"
                                Text(
                                    text = "EchoSpace",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            } else {
                                // If in a subfolder, display clickable breadcrumbs
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    pathSegments.forEachIndexed { index, segment ->
                                        val isLastSegment = index == pathSegments.size - 1
                                        Text(
                                            text = segment, // This will now be "DIARY", then "2025", then "JUN"
                                            style = MaterialTheme.typography.titleMedium,
                                            // Dim the last segment slightly as it's not clickable or represents current location
                                            color = if (isLastSegment) MaterialTheme.colorScheme.onSurface.copy(
                                                alpha = 0.7f
                                            )
                                            else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier
                                                .then(
                                                    if (!isLastSegment) { // Only add clickable if NOT the last segment
                                                        Modifier.clickable {
                                                            // Construct the path up to and including the clicked segment, using '#'
                                                            val newPathToNavigate = pathSegments
                                                                .take(index + 1)
                                                                .joinToString("#")
                                                            currentPath = newPathToNavigate
                                                        }
                                                    } else {
                                                        Modifier // No clickable modifier for the last segment
                                                    }
                                                )
                                                .padding(
                                                    horizontal = 0.dp,
                                                    vertical = 4.dp
                                                ) // Adjusted padding for better touch
                                        )
                                        if (!isLastSegment) { // Don't add separator after the last segment
                                            // Visual separator is " / "
                                            Text(
                                                text = " / ",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                //modifier = Modifier.padding(horizontal = 0.dp) // Padding for the separator
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }           // search if condition ending
                },          // tittle ending
                navigationIcon = {
                    if (currentPath.isNotEmpty() && !isSearchMode) {
                        //val iconTint = if (isDarkTheme) Color(0xFFF0F0F0)
                        //else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .size(36.dp) // Standard touch target size
                                .clip(RoundedCornerShape(10.dp)) // Circular ripple boundary
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(
                                        bounded = true,
                                        radius = 20.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    )
                                ){ currentPath = "" }
                                .padding(1.dp) // This centers the 24dp icon within 48dp touch area
                                .offset(y = (-1).dp), // Move icon left to align with text
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_google_home),
                                contentDescription = "Home",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant, // Apply the same tint as folder/file icons
                                modifier = Modifier.size(46.dp)
                            )
                        }
                    }
                },
                actions = {
                    if (!isSearchMode) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(1.dp), // Exact spacing control
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Search icon
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp)) // Circular ripple boundary
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(
                                            bounded = true,
                                            //radius = 20.dp,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        isSearchMode = true
                                    }
                                    .padding(6.dp), // Minimal touch padding
                                tint = MaterialTheme.colorScheme.onSurface
                            )

                            // Theme toggle icon
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp)) // Circular ripple boundary
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(
                                            bounded = true,
                                            //radius = 20.dp,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        )
                                    ) { onThemeToggle() }
                                    .padding(6.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )

                            // sort button
                            Box {
                                Icon(
                                    Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp)) // Circular ripple boundary
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(
                                                bounded = true,
                                                //radius = 20.dp,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                            )
                                        ) { showSortMenu = true }
                                        .padding(6.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )

                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Name A-Z") },
                                        onClick = {
                                            coroutineScope.launch {
                                                sortPreferences.setSortType(SortType.NAME_ASC)
                                            }
                                            showSortMenu = false
                                        },
                                        leadingIcon = {
                                            if (currentSortType == SortType.NAME_ASC) {
                                                Icon(Icons.Default.Check, contentDescription = null)
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Name Z-A") },
                                        onClick = {
                                            coroutineScope.launch {
                                                sortPreferences.setSortType(SortType.NAME_DESC)
                                            }
                                            showSortMenu = false
                                        },
                                        leadingIcon = {
                                            if (currentSortType == SortType.NAME_DESC) {
                                                Icon(Icons.Default.Check, contentDescription = null)
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Newest First") },
                                        onClick = {
                                            coroutineScope.launch {
                                                sortPreferences.setSortType(SortType.TIME_DESC)
                                            }
                                            showSortMenu = false
                                        },
                                        leadingIcon = {
                                            if (currentSortType == SortType.TIME_DESC) {
                                                Icon(Icons.Default.Check, contentDescription = null)
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Oldest First") },
                                        onClick = {
                                            coroutineScope.launch {
                                                sortPreferences.setSortType(SortType.TIME_ASC)
                                            }
                                            showSortMenu = false
                                        },
                                        leadingIcon = {
                                            if (currentSortType == SortType.TIME_ASC) {
                                                Icon(Icons.Default.Check, contentDescription = null)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    } // search if ending
                },
                // Search mode parameters
                isSearchMode = isSearchMode,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onSearchClose = ::closeSearch,
                searchFocusRequester = searchFocusRequester
            )
        },
        floatingActionButton = {
            if (!isSearchMode) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Entry",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .then(
                    if (!isSearchMode) {
                        Modifier.pullRefresh(pullRefreshState)
                    } else {
                        Modifier // No pull refresh in search mode
                    }
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Show search results or normal folder contents
                val itemsToShow = if (isSearchMode) searchResults else folderContents

                items(itemsToShow) { folderItem ->
                    FolderItemComponent(
                        item = folderItem,
                        isDarkTheme = isDarkTheme,
                        onClick = {
                            if (folderItem.isFolder && !isSearchMode) {
                                currentPath = folderItem.path
                            } else {
                                folderItem.entry?.let { entry ->
                                    val intent = Intent(context, EditorActivity::class.java).apply {
                                        putExtra("file_uri", entry.file.uri.toString())
                                        putExtra("current_path", if (isSearchMode) "" else currentPath) // Pass current path
                                    }
                                    launcher.launch(intent) // Use launcher instead of startActivity
                                }
                            }
                        },
                        onRename = if (!isSearchMode) { item ->
                            item.entry?.let { entryToRename = it }
                        } else null, // Pass null in search mode to disable long press menu
                        onDelete = if (!isSearchMode) { item ->
                            item.entry?.let { entryToDelete = it }
                        } else null
                    )
                }
            }

            if (!isSearchMode) {
                PullRefreshIndicator(
                    refreshing = isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Create dialog
        if (showCreateDialog) {
            CreateEntryDialog(
                onDismiss = {
                    showCreateDialog = false
                    createDialogErrorMessage = "" // Clear error when dismissing
                },
                errorMessage = createDialogErrorMessage,
                onFileNameConfirmed = { fileName ->
                    val (result, file) = diaryRepository.createEntryWithResult(
                        fileName,
                        currentPath
                    )

                    when (result) {
                        CreateFileResult.SUCCESS -> {
                            file?.let {
                                val intent = Intent(context, EditorActivity::class.java).apply {
                                    putExtra("file_uri", it.uri.toString())
                                    putExtra("edit_mode", true)
                                    putExtra("current_path", currentPath)
                                }
                                launcher.launch(intent)
                            }
                            createDialogErrorMessage = ""
                            CreateFileResult.SUCCESS
                        }

                        CreateFileResult.FILENAME_EXISTS -> {
                            createDialogErrorMessage =
                                "Filename already exists!" // Make sure this is set
                            CreateFileResult.FILENAME_EXISTS
                        }

                        CreateFileResult.OTHER_ERROR -> {
                            createDialogErrorMessage = "Failed to create file"
                            CreateFileResult.OTHER_ERROR
                        }
                    }
                }
            )
        }

        // Rename dialog
        entryToRename?.let { entry ->
            RenameEntryDialog(
                entry = entry,
                onDismiss = { entryToRename = null },
                onRename = { e, newName ->
                    val result = (diaryRepository as DiaryRepositoryImpl).renameEntryWithResult(e.file, newName)
                    when (result) {
                        CreateFileResult.SUCCESS -> {
                            Toast.makeText(context, "Entry renamed", Toast.LENGTH_SHORT).show()
                            refreshTrigger++
                            entryToRename = null
                        }
                        CreateFileResult.FILENAME_EXISTS -> {
                            // Error will be handled by the dialog
                        }
                        CreateFileResult.OTHER_ERROR -> {
                            Toast.makeText(context, "Failed to rename entry", Toast.LENGTH_SHORT).show()
                        }
                    }
                    result // Return the result to the dialog
                }
            )
        }

        // Delete dialog with smart navigation
        entryToDelete?.let { entry ->
            DeleteConfirmationDialog(
                entry = entry,
                onDismiss = { entryToDelete = null },
                onConfirm = { e ->
                    if (diaryRepository.deleteEntry(e.file)) {
                        Toast.makeText(context, "Entry deleted", Toast.LENGTH_SHORT).show()
                        refreshTrigger++

                        // Check if current folder still has content after deletion
                        // Use a small delay to ensure the file system has updated
                        coroutineScope.launch {
                            delay(100) // Small delay for file system consistency
                            if (!diaryRepository.hasContentInPath(currentPath)) {
                                navigateToNearestParentWithContent()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Failed to delete entry", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}
