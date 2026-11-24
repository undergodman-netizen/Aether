package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GeminiApiClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

data class ChatMessage(val text: String, val isUser: Boolean)

@Composable
fun TypingIndicator() {
    var step by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(300)
            step = (step + 1) % 4
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = "Tutor is writing",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.width(2.dp))
        repeat(3) { index ->
            val alpha = if (step == index + 1 || (step == 0 && index == 2)) 1.0f else 0.3f
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatSidebar(
    modifier: Modifier = Modifier,
    videoContextProvider: () -> String,
    apiKey: String,
    geminiModel: String = "models/gemini-3.5-flash-lite",
    customSystemPrompt: String = "",
    onSaveResponseAsNote: (String) -> Unit = {},
    onClose: () -> Unit
) {
    var messages by remember { mutableStateOf(listOf(
        ChatMessage("Hello! Ask me any questions or clear your doubts regarding this MBBS medical video.", false)
    )) }
    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var userSentMessage by remember { mutableStateOf(false) }

    // Auto Scroll to bottom upon updates
    LaunchedEffect(messages, isLoading) {
        if (messages.isNotEmpty()) {
            val isAtBottom = if (listState.layoutInfo.visibleItemsInfo.isNotEmpty()) {
                val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.last()
                lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 2
            } else {
                true
            }
            if (isAtBottom || userSentMessage) {
                listState.animateScrollToItem(messages.size - 1)
                userSentMessage = false
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "AI",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Aether Assistant",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            text = "Play-Along Classroom Tutor",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 0.2.sp
                        )
                    }
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Chat History
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(
                        message = message,
                        onSaveMessage = if (!message.isUser && message.text.isNotBlank() && !message.text.startsWith("Hello!")) {
                            { onSaveResponseAsNote(message.text) }
                        } else null
                    )
                }
                
                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 18.dp,
                                            topEnd = 18.dp,
                                            bottomStart = 4.dp,
                                            bottomEnd = 18.dp
                                        )
                                    )
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .border(
                                        width = 0.5.dp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(
                                            topStart = 18.dp,
                                            topEnd = 18.dp,
                                            bottomStart = 4.dp,
                                            bottomEnd = 18.dp
                                        )
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                TypingIndicator()
                            }
                        }
                    }
                }
            }

            // Input Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask your medical tutor...", fontSize = 14.sp) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    textStyle = TextStyle(fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Normal),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (input.isNotBlank() && !isLoading) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .clickable(enabled = input.isNotBlank() && !isLoading) {
                            val userMsg = input
                            input = ""
                            val currentHistory = messages + ChatMessage(userMsg, true)
                            messages = currentHistory
                            isLoading = true
                            userSentMessage = true
                            
                            coroutineScope.launch {
                                val historyList = currentHistory.map { Pair(it.text, it.isUser) }
                                var assistantReply = ""
                                val updatedMessagesWithAssistant = currentHistory + ChatMessage("", false)
                                messages = updatedMessagesWithAssistant
                                
                                try {
                                    GeminiApiClient.chatWithGeminiStream(
                                        history = historyList,
                                        contextText = videoContextProvider(),
                                        apiKey = apiKey,
                                        modelName = geminiModel,
                                        customSystemPrompt = customSystemPrompt
                                    ).collect { chunk ->
                                        isLoading = false
                                        assistantReply += chunk
                                        messages = updatedMessagesWithAssistant.dropLast(1) + ChatMessage(assistantReply, false)
                                    }
                                } catch (e: Exception) {
                                    if (assistantReply.isEmpty()) {
                                        assistantReply = "Error: Failed to stream response. ${e.localizedMessage}"
                                    } else {
                                        assistantReply += "\n\n[Stream Interrupted: ${e.localizedMessage}]"
                                    }
                                    messages = updatedMessagesWithAssistant.dropLast(1) + ChatMessage(assistantReply, false)
                                } finally {
                                    if (assistantReply.isEmpty()) {
                                        assistantReply = "Error: Got empty stream content. Please verify your Gemini API key in settings."
                                        messages = updatedMessagesWithAssistant.dropLast(1) + ChatMessage(assistantReply, false)
                                    }
                                    isLoading = false
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (input.isNotBlank() && !isLoading) MaterialTheme.colorScheme.onPrimary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    onSaveMessage: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (message.isUser) {
            Spacer(modifier = Modifier.weight(0.15f))
        } else {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }
        
        val primaryColor = MaterialTheme.colorScheme.primary
        Box(
            modifier = Modifier
                .weight(0.85f, fill = false)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (message.isUser) 18.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 18.dp
                    )
                )
                .background(
                    if (message.isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
                .border(
                    width = 0.5.dp,
                    color = if (message.isUser) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (message.isUser) 18.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 18.dp
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                val blocks = parseMarkdownBlocks(message.text)
                blocks.forEach { block ->
                    when (block) {
                        is TextBlock -> {
                            Text(
                                text = parseMarkdown(block.content, primaryColor),
                                color = textColor,
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                letterSpacing = 0.3.sp
                            )
                        }
                        is TableBlock -> {
                            MarkdownTable(block.rows, message.isUser)
                        }
                    }
                }
                if (onSaveMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onSaveMessage,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Save key points",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Save to reference",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        if (message.isUser) {
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.weight(0.15f))
        } else {
            Spacer(modifier = Modifier.weight(0.15f))
        }
    }
}

fun cleanMathNotation(input: String): String {
    var cleaned = input
    cleaned = cleaned.replace(Regex("\\$([KNaCaMg]+)\\^\\+\\$"), "$1⁺")
    cleaned = cleaned.replace(Regex("\\$([KNaCaMg]+)\\^\\+\\+\\$"), "$1²⁺")
    cleaned = cleaned.replace(Regex("\\$([KNaCaMg]+)\\^\\-\\s*\\$"), "$1⁻")
    cleaned = cleaned.replace(Regex("\\$([^$]+)\\$")) { match ->
        var inner = match.groupValues[1]
        inner = inner.replace("^{+}", "⁺")
        inner = inner.replace("^-", "⁻")
        inner = inner.replace("^2+", "²⁺")
        inner = inner.replace("^2-", "²⁻")
        inner = inner.replace("{", "").replace("}", "")
        inner = inner.replace("\\", "")
        inner
    }
    return cleaned
}

fun parseMarkdown(text: String, primaryColor: Color): AnnotatedString {
    val cleanedText = cleanMathNotation(text)
    return buildAnnotatedString {
        val lines = cleanedText.split("\n")
        lines.forEachIndexed { index, line ->
            var trimmedLine = line
            var isHeader = false
            var headerLevel = 0
            
            if (line.startsWith("#")) {
                val match = Regex("^(#+)\\s+(.*)$").find(line)
                if (match != null) {
                    headerLevel = match.groupValues[1].length
                    trimmedLine = match.groupValues[2]
                    isHeader = true
                }
            }
            
            var isBullet = false
            if (!isHeader && (trimmedLine.startsWith("* ") || trimmedLine.startsWith("- "))) {
                isBullet = true
                trimmedLine = trimmedLine.substring(2)
            }
            
            if (isHeader) {
                val scale = when (headerLevel) {
                    1 -> 1.3f
                    2 -> 1.2f
                    else -> 1.1f
                }
                pushStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = (14 * scale).sp
                    )
                )
            } else if (isBullet) {
                append("• ")
            }
            
            var i = 0
            val lineLen = trimmedLine.length
            while (i < lineLen) {
                if (i + 1 < lineLen && trimmedLine[i] == '*' && trimmedLine[i + 1] == '*') {
                    val endOfBold = trimmedLine.indexOf("**", i + 2)
                    if (endOfBold != -1) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(trimmedLine.substring(i + 2, endOfBold))
                        pop()
                        i = endOfBold + 2
                    } else {
                        append("**")
                        i += 2
                    }
                } else if (trimmedLine[i] == '*') {
                    val endOfItalic = trimmedLine.indexOf('*', i + 1)
                    if (endOfItalic != -1 && endOfItalic != i + 1) {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(trimmedLine.substring(i + 1, endOfItalic))
                        pop()
                        i = endOfItalic + 1
                    } else {
                        append("*")
                        i += 1
                    }
                } else if (trimmedLine[i] == '`') {
                    val endOfCode = trimmedLine.indexOf('`', i + 1)
                    if (endOfCode != -1 && endOfCode != i + 1) {
                        pushStyle(
                            SpanStyle(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                background = Color(0x26D4AF37), // subtle gold container shadow for code pieces
                                color = primaryColor,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        )
                        append(trimmedLine.substring(i + 1, endOfCode))
                        pop()
                        i = endOfCode + 1
                    } else {
                        append("`")
                        i += 1
                    }
                } else {
                    append(trimmedLine[i])
                    i++
                }
            }
            
            if (isHeader) {
                pop()
            }
            
            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}

sealed class MarkdownBlock
data class TextBlock(val content: String) : MarkdownBlock()
data class TableBlock(val rows: List<String>) : MarkdownBlock()

fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val lines = text.split("\n")
    val blocks = mutableListOf<MarkdownBlock>()
    
    val currentText = StringBuilder()
    val tableLines = mutableListOf<String>()
    var inCodeBlock = false
    
    fun flushText() {
        if (currentText.isNotEmpty()) {
            blocks.add(TextBlock(currentText.toString()))
            currentText.clear()
        }
    }
    
    fun processTableBuffer() {
        if (tableLines.isEmpty()) return
        val isRealTable = tableLines.size > 1 && tableLines.any { row ->
            val cells = row.trim().removePrefix("|").removeSuffix("|").split("|")
            cells.isNotEmpty() && cells.all { cell -> 
                cell.isNotEmpty() && cell.all { ch -> ch == '-' || ch == ':' || ch.isWhitespace() } 
            }
        }
        if (isRealTable) {
            flushText()
            blocks.add(TableBlock(tableLines.toList()))
        } else {
            tableLines.forEach { l ->
                if (currentText.isNotEmpty()) currentText.append("\n")
                currentText.append(l)
            }
        }
        tableLines.clear()
    }
    
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            inCodeBlock = !inCodeBlock
            processTableBuffer()
            if (currentText.isNotEmpty()) currentText.append("\n")
            currentText.append(line)
        } else if (inCodeBlock) {
            if (currentText.isNotEmpty()) currentText.append("\n")
            currentText.append(line)
        } else if (trimmed.contains("|")) {
            tableLines.add(line)
        } else {
            processTableBuffer()
            if (currentText.isNotEmpty() || line.isNotEmpty()) {
                if (currentText.isNotEmpty()) currentText.append("\n")
                currentText.append(line)
            }
        }
    }
    processTableBuffer()
    flushText()
    
    return blocks
}

@Composable
fun MarkdownTable(lines: List<String>, isUser: Boolean) {
    val tableData = lines.mapNotNull { line ->
        val trimmed = line.trim()
        val content = if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
            trimmed.substring(1, trimmed.length - 1)
        } else {
            trimmed
        }
        val cells = content.split("|").map { it.trim() }
        
        val isSeparator = cells.isNotEmpty() && cells.all { cell -> 
            cell.isNotEmpty() && cell.all { it == '-' || it == ':' || it.isWhitespace() }
        }
        if (isSeparator) null else cells
    }

    if (tableData.isEmpty()) return

    val colCount = tableData.maxOfOrNull { it.size } ?: 0
    if (colCount == 0) return

    val borderColor = if (isUser) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val headerBgColor = if (isUser) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    val textColor = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
    ) {
        tableData.forEachIndexed { rowIndex, row ->
            val isHeader = rowIndex == 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .background(if (isHeader) headerBgColor else Color.Transparent)
            ) {
                for (colIndex in 0 until colCount) {
                    val cellText = cleanMathNotation(row.getOrNull(colIndex) ?: "")
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = cellText,
                            color = textColor,
                            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (colIndex < colCount - 1) {
                         Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(borderColor))
                    }
                }
            }
            if (rowIndex < tableData.size - 1) {
                HorizontalDivider(color = borderColor)
            }
        }
    }
}
