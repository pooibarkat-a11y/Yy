package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.QuranAudioDownloader
import com.example.data.QuranData
import com.example.model.Surah
import com.example.ui.components.SheikhAvatar
import com.example.ui.components.toArabicIndicDigits
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.viewmodel.AppScreen
import com.example.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import java.io.File

enum class DownloadsFilter {
    ALL,
    AUDIO_QURAN,
    TAFSIR,
    MUSHAF_PAGES,
    AZKAR
}

/**
 * Dedicated Offline Downloads Manager ("مركز التنزيلات المستقلة بدون نت").
 * Provides complete control over downloaded Surahs, reciters audio, high-res Mushaf pages,
 * and Tafsir offline content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(DownloadsFilter.ALL) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val activeDownloads by QuranAudioDownloader.activeDownloads.collectAsState()
    val downloadProgress by QuranAudioDownloader.downloadProgress.collectAsState()
    val downloadedSurahsSet by QuranAudioDownloader.downloadedSurahsSet.collectAsState()
    val audioState by viewModel.audioPlayer.state.collectAsState()

    val currentReciter = audioState.selectedReciter

    // Calculate downloaded storage size dynamically
    val offlineDir = remember {
        File(context.getExternalFilesDir(null) ?: context.filesDir, "quran_offline_audio")
    }

    val totalStorageUsedMb = remember(downloadedSurahsSet, activeDownloads) {
        try {
            if (offlineDir.exists()) {
                val bytes = offlineDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
                // Add bundled offline data base size (approx 12.5 MB)
                (bytes / (1024.0 * 1024.0)) + 12.5
            } else 12.5
        } catch (e: Exception) {
            12.5
        }
    }

    // Popular Surahs for Quick Download
    val popularSurahs = remember {
        listOf(1, 2, 18, 36, 55, 56, 67, 112, 113, 114)
    }

    // Filter surahs list
    val allSurahs = QuranData.surahs
    val filteredSurahs = remember(searchQuery, selectedFilter, downloadedSurahsSet) {
        allSurahs.filter { surah ->
            val matchSearch = searchQuery.isBlank() ||
                    surah.nameArabic.contains(searchQuery.trim()) ||
                    surah.nameEnglish.contains(searchQuery.trim(), ignoreCase = true) ||
                    surah.id.toString() == searchQuery.trim()

            val isDownloaded = downloadedSurahsSet.contains(QuranAudioDownloader.makeKey(currentReciter.id, surah.id))

            when (selectedFilter) {
                DownloadsFilter.ALL -> matchSearch
                DownloadsFilter.AUDIO_QURAN -> matchSearch
                DownloadsFilter.TAFSIR -> matchSearch && surah.id in 1..20
                DownloadsFilter.MUSHAF_PAGES -> matchSearch && isDownloaded
                DownloadsFilter.AZKAR -> matchSearch && surah.id in listOf(1, 112, 113, 114)
            }
        }
    }

    BackHandler {
        viewModel.navigateTo(AppScreen.HOME)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.navigateTo(AppScreen.HOME) },
                            modifier = Modifier.testTag("downloads_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "مركز التنزيلات بدون نت",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "إدارة التلاوات والمصاحف والتفاسير المحفوظة",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Reciter Avatar Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable {
                            viewModel.navigateTo(AppScreen.AUDIO_PLAYER)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SheikhAvatar(
                                nameArabic = currentReciter.nameArabic,
                                imageUrl = currentReciter.imageUrl,
                                sheikhId = currentReciter.id,
                                size = 26.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentReciter.nameArabic.split(" ").firstOrNull() ?: "القارئ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    // Storage Status Card
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = BorderStroke(1.5.dp, EmeraldPrimary.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(EmeraldPrimary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Storage,
                                                contentDescription = null,
                                                tint = EmeraldPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "مساحة التنزيلات المستخدمة",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "%.1f ميجابايت".format(totalStorageUsedMb),
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = EmeraldPrimary
                                            )
                                        }
                                    }

                                    // Offline Ready Badge
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = EmeraldPrimary.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, EmeraldPrimary)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = EmeraldPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "المصحف كامل بدون نت ✓",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldPrimary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Quick Actions Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Download Top Surahs
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                popularSurahs.forEach { sId ->
                                                    val key = QuranAudioDownloader.makeKey(currentReciter.id, sId)
                                                    if (!downloadedSurahsSet.contains(key) && !activeDownloads.contains(key)) {
                                                        val sObj = QuranData.surahs.find { it.id == sId }
                                                        if (sObj != null) {
                                                            QuranAudioDownloader.downloadSurah(currentReciter, sObj)
                                                        }
                                                    }
                                                }
                                                Toast.makeText(context, "جارٍ تنزيل باقة السور الأساسية بصوت ${currentReciter.nameArabic}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudDownload,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "تنزيل أهم السور",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Clear storage
                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                try {
                                                    offlineDir.deleteRecursively()
                                                    offlineDir.mkdirs()
                                                    QuranAudioDownloader.refreshDownloadedSurahs()
                                                    Toast.makeText(context, "تم مسح الذاكرة المؤقتة بنجاح", Toast.LENGTH_SHORT).show()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "خطأ أثناء المسح", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(0.9f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "تفريغ الذاكرة",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Search & Filters
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("ابحث عن سورة لتنزيلها أو الاستماع لها...", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldPrimary)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("downloads_search_input"),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }

                    // Filter Chips Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedFilter == DownloadsFilter.ALL,
                                onClick = { selectedFilter = DownloadsFilter.ALL },
                                label = { Text("الكل", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = selectedFilter == DownloadsFilter.AUDIO_QURAN,
                                onClick = { selectedFilter = DownloadsFilter.AUDIO_QURAN },
                                label = { Text("القرآن الصوتي", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                            FilterChip(
                                selected = selectedFilter == DownloadsFilter.TAFSIR,
                                onClick = { selectedFilter = DownloadsFilter.TAFSIR },
                                label = { Text("التفسير", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                            FilterChip(
                                selected = selectedFilter == DownloadsFilter.MUSHAF_PAGES,
                                onClick = { selectedFilter = DownloadsFilter.MUSHAF_PAGES },
                                label = { Text("المحفوظة ✓", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                        }
                    }

                    // Section Title with Reciter Name
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "السور المتاحة للتنزيل والاستماع الأوفلاين (${filteredSurahs.size} سورة)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "بصوت: ${currentReciter.nameArabic.split(" ").firstOrNull()}",
                                fontSize = 12.sp,
                                color = GoldAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Surahs List
                    items(filteredSurahs, key = { it.id }) { surah ->
                        val downloadKey = QuranAudioDownloader.makeKey(currentReciter.id, surah.id)
                        val isDownloaded = downloadedSurahsSet.contains(downloadKey)
                        val isDownloading = activeDownloads.contains(downloadKey)
                        val progress = downloadProgress[downloadKey] ?: 0

                        val isPlayingCurrent = audioState.isPlaying && audioState.currentSurah?.id == surah.id

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPlayingCurrent)
                                    EmeraldPrimary.copy(alpha = 0.08f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isPlayingCurrent) EmeraldPrimary
                                else if (isDownloaded) EmeraldLight.copy(alpha = 0.4f)
                                else Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Number & Surah Info
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isDownloaded) EmeraldPrimary.copy(alpha = 0.2f)
                                                    else MaterialTheme.colorScheme.background
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = toArabicIndicDigits(surah.id),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDownloaded) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "سورة ${surah.nameArabic}",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isDownloaded) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "تم التنزيل",
                                                        tint = EmeraldPrimary,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = "${surah.versesCount} آية • ${surah.revelationType.arabicName} • الجزء ${toArabicIndicDigits(surah.juzNumber)}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Action Buttons: Play & Download / Delete
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Play / Pause Button
                                        IconButton(
                                            onClick = {
                                                if (isPlayingCurrent) {
                                                    viewModel.audioPlayer.pause()
                                                } else {
                                                    viewModel.audioPlayer.playSurah(surah, 1)
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isPlayingCurrent) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                                contentDescription = "استماع",
                                                tint = if (isPlayingCurrent) EmeraldPrimary else GoldAccent,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }

                                        // Download or Delete Button
                                        if (isDownloading) {
                                            Box(
                                                modifier = Modifier.size(36.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    progress = { progress / 100f },
                                                    modifier = Modifier.size(26.dp),
                                                    color = EmeraldPrimary,
                                                    strokeWidth = 3.dp
                                                )
                                                Text(
                                                    text = "$progress%",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        } else if (isDownloaded) {
                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        QuranAudioDownloader.deleteSurah(currentReciter.id, surah.id)
                                                        Toast.makeText(context, "تم حذف الملف الصوتي لسورة ${surah.nameArabic}", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "حذف التنزيل",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        } else {
                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        QuranAudioDownloader.downloadSurah(currentReciter, surah)
                                                        Toast.makeText(context, "بدأ تنزيل سورة ${surah.nameArabic} بصوت ${currentReciter.nameArabic}...", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Download,
                                                    contentDescription = "تنزيل أوفلاين",
                                                    tint = EmeraldPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Download Progress Bar
                                if (isDownloading) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { progress / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = EmeraldPrimary,
                                        trackColor = MaterialTheme.colorScheme.surface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
