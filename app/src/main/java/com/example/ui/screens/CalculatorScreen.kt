package com.example.ui.screens

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.CalculatorViewModel
import com.example.util.ScientificCalculatorEngine
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel) {
    val expression by viewModel.expression.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()
    val angleUnit by viewModel.angleUnit.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val isShift by viewModel.isShiftActive.collectAsStateWithLifecycle()
    val isResultFinal by viewModel.isResultFinal.collectAsStateWithLifecycle()

    var showHistorySheet by remember { mutableStateOf(false) }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == android.app.Activity.RESULT_OK) {
            val data = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            data?.firstOrNull()?.let { viewModel.processVoiceCommand(it) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scientific Lab", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        }
                        voiceLauncher.launch(intent)
                    }) { Icon(imageVector = Icons.Default.Mic, contentDescription = "Voice", tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = { showHistorySheet = true }) { Icon(imageVector = Icons.Default.History, contentDescription = "History") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        val isDark = isSystemInDarkTheme()
        val bgGradient = if (isDark) {
            Brush.verticalGradient(colors = listOf(MaterialTheme.colorScheme.background, Color(0xFF1E1135)))
        } else {
            Brush.verticalGradient(colors = listOf(MaterialTheme.colorScheme.background, Color(0xFFEDE6FA)))
        }

        Box(Modifier.fillMaxSize().background(bgGradient).padding(innerPadding)) {
            Column(Modifier.fillMaxSize()) {
                // --- Display Area ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 28.dp, vertical = 20.dp)
                ) {
                    // Scrollable Expression Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        val scrollState = rememberScrollState()
                        
                        // Auto-scroll to bottom as expression grows
                        LaunchedEffect(expression.text) {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }

                        val primaryColor = MaterialTheme.colorScheme.primary
                        val calcVisualTransformation = remember(primaryColor) {
                            androidx.compose.ui.text.input.VisualTransformation { annotatedString: androidx.compose.ui.text.AnnotatedString ->
                                val textStr = annotatedString.text
                                val builder = androidx.compose.ui.text.AnnotatedString.Builder(textStr)
                                
                                // Subscript for log base - Bigger & Higher
                                val logRegex = Regex("log([a-zA-Z0-9.]+)?\\(")
                                logRegex.findAll(textStr).forEach { match ->
                                    val baseGroup = match.groups[1]
                                    if (baseGroup != null) {
                                        builder.addStyle(
                                            androidx.compose.ui.text.SpanStyle(
                                                baselineShift = androidx.compose.ui.text.style.BaselineShift(-0.15f),
                                                fontSize = 22.sp,
                                                color = primaryColor,
                                                fontWeight = FontWeight.Black
                                            ),
                                            baseGroup.range.first, baseGroup.range.last + 1
                                        )
                                    }
                                }
                                
                                // Superscript for root index - Bigger
                                val rootRegex = Regex("([a-zA-Z0-9.]+)?√")
                                rootRegex.findAll(textStr).forEach { match ->
                                    val indexGroup = match.groups[1]
                                    if (indexGroup != null) {
                                        builder.addStyle(
                                            androidx.compose.ui.text.SpanStyle(
                                                baselineShift = androidx.compose.ui.text.style.BaselineShift(0.4f),
                                                fontSize = 22.sp,
                                                color = primaryColor,
                                                fontWeight = FontWeight.Black
                                            ),
                                            indexGroup.range.first, indexGroup.range.last + 1
                                        )
                                    }
                                    // Make the √ symbol look nice
                                    builder.addStyle(
                                        androidx.compose.ui.text.SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold),
                                        match.range.last, match.range.last + 1
                                    )
                                }

                                // Style operators
                                textStr.forEachIndexed { index, c ->
                                    if (c in "+-*/^") {
                                        builder.addStyle(
                                            androidx.compose.ui.text.SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold),
                                            index, index + 1
                                        )
                                    }
                                }

                                androidx.compose.ui.text.input.TransformedText(
                                    builder.toAnnotatedString(), 
                                    androidx.compose.ui.text.input.OffsetMapping.Identity
                                )
                            }
                        }

                        CustomCursorTextField(
                            value = expression,
                            onValueChange = { viewModel.updateExpression(it) },
                            textStyle = TextStyle(
                                fontSize = if (isResultFinal) 26.sp else 34.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                lineBreak = androidx.compose.ui.text.style.LineBreak.Heading,
                                lineHeight = 64.sp // Massive height to prevent any clipping
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .padding(vertical = 32.dp), // Extra padding for safety
                            visualTransformation = calcVisualTransformation,
                            placeholder = "0"
                        )
                    }
                    
                    Spacer(Modifier.height(4.dp))
                    
                    // Fixed Result Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        val isError = result.contains("Error", ignoreCase = true) || result.contains("Syntax", ignoreCase = true) || result.contains("Mismatched", ignoreCase = true)
                        Text(
                            text = if (isResultFinal && !isError) "= $result" else result,
                            fontSize = if (isError) 14.sp else if (isResultFinal) 44.sp else 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isError) MaterialTheme.colorScheme.error 
                                    else if (isResultFinal) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.End,
                            maxLines = 2, // Allow result to wrap if it's very long
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                // --- Keypad ---
                val keypadGradient = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.02f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                        Color.Transparent
                    )
                )
                Surface(
                    modifier = Modifier.fillMaxWidth().weight(2.8f),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(keypadGradient)
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val sciRowMod = Modifier.fillMaxWidth().height(38.dp)
                        
                        // Scientific Row 1: Trig & 2ND
                        Row(sciRowMod, Arrangement.spacedBy(10.dp)) {
                            // High-Visibility 2ND Button - Moved to Top Left
                            Surface(
                                onClick = { viewModel.toggleShift() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.2f).fillMaxHeight(),
                                color = if (isShift) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "2ND", 
                                        fontSize = 13.sp, 
                                        fontWeight = FontWeight.Black, 
                                        color = if (isShift) Color.White else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            ScientificBtn(if (!isShift) "sin" else "asin", Modifier.weight(1f)) { viewModel.onCalcKey(if (!isShift) "sin(" else "asin(") }
                            ScientificBtn(if (!isShift) "cos" else "acos", Modifier.weight(1f)) { viewModel.onCalcKey(if (!isShift) "cos(" else "acos(") }
                            ScientificBtn(if (!isShift) "tan" else "atan", Modifier.weight(1f)) { viewModel.onCalcKey(if (!isShift) "tan(" else "atan(") }
                            ScientificBtn(if (!isShift) "sinh" else "asinh", Modifier.weight(1f)) { viewModel.onCalcKey(if (!isShift) "sinh(" else "asinh(") }
                        }
                        
                        // Scientific Row 2: Logs & Roots
                        Row(sciRowMod, Arrangement.spacedBy(10.dp)) {
                            ScientificBtn(if (!isShift) "ln" else "eˣ", Modifier.weight(1f)) { viewModel.onCalcKey(if (!isShift) "ln(" else "e^") }
                            ScientificBtn(if (!isShift) "log" else "10ˣ", Modifier.weight(1f)) { viewModel.onCalcKey(if (!isShift) "log(" else "10^") }
                            ScientificBtn(if (!isShift) "logᵧx" else "log₁₀", Modifier.weight(1f)) { viewModel.onCalcKey(if (!isShift) "log" else "log10(") }
                            ScientificBtn(if (!isShift) "ʸ√x" else "∛", Modifier.weight(1f)) { viewModel.onCalcKey(if (!isShift) "root" else "∛(") }
                            ScientificBtn(if (!isShift) "√" else "x²", Modifier.weight(1f)) { viewModel.onCalcKey(if (!isShift) "√(" else "^2") }
                        }
                        
                        // Scientific Row 3: Const & Math
                        Row(sciRowMod, Arrangement.spacedBy(10.dp)) {
                            ScientificBtn("π", Modifier.weight(1f)) { viewModel.onCalcKey("π") }
                            ScientificBtn("e", Modifier.weight(1f)) { viewModel.onCalcKey("e") }
                            ScientificBtn("^", Modifier.weight(1f)) { viewModel.onCalcKey("^") }
                            ScientificBtn("!", Modifier.weight(1f)) { viewModel.onCalcKey("!") }
                            ScientificBtn(",", Modifier.weight(1f)) { viewModel.onCalcKey(",") }
                        }
                        
                        // Scientific Row 4: Structure & Extras
                        Row(sciRowMod, Arrangement.spacedBy(10.dp)) {
                            ScientificBtn(if (angleUnit == ScientificCalculatorEngine.AngleUnit.DEG) "RAD" else "DEG", Modifier.weight(1f)) { viewModel.onCalcKey(if (angleUnit == ScientificCalculatorEngine.AngleUnit.DEG) "RAD" else "DEG") }
                            ScientificBtn(if (!isShift) "abs" else "ceil", Modifier.weight(1f)) { viewModel.onCalcKey(if (!isShift) "abs(" else "ceil(") }
                            ScientificBtn(if (!isShift) "nCr" else "gcd", Modifier.weight(1f)) { viewModel.onCalcKey(if (!isShift) "c" else "gcd(") }
                            ScientificBtn(if (!isShift) "nPr" else "lcm", Modifier.weight(1f)) { viewModel.onCalcKey(if (!isShift) "p" else "lcm(") }
                            ScientificBtn(if (!isShift) "rand" else "floor", Modifier.weight(1f)) { viewModel.onCalcKey(if (!isShift) "rand" else "floor(") }
                        }

                        Spacer(Modifier.height(16.dp))

                        val gridRowMod = Modifier.fillMaxWidth().weight(1.2f)
                        Row(gridRowMod, Arrangement.spacedBy(8.dp)) {
                            DigitBtn("7", Modifier.weight(1f)) { viewModel.onCalcKey("7") }
                            DigitBtn("8", Modifier.weight(1f)) { viewModel.onCalcKey("8") }
                            DigitBtn("9", Modifier.weight(1f)) { viewModel.onCalcKey("9") }
                            OperatorBtn("÷", Modifier.weight(1f)) { viewModel.onCalcKey("/") }
                            ControlBtn("⌫", Modifier.weight(1f), isDelete = true) { viewModel.onCalcKey("⌫") }
                        }
                        Row(gridRowMod, Arrangement.spacedBy(8.dp)) {
                            DigitBtn("4", Modifier.weight(1f)) { viewModel.onCalcKey("4") }
                            DigitBtn("5", Modifier.weight(1f)) { viewModel.onCalcKey("5") }
                            DigitBtn("6", Modifier.weight(1f)) { viewModel.onCalcKey("6") }
                            OperatorBtn("×", Modifier.weight(1f)) { viewModel.onCalcKey("*") }
                            ControlBtn("C", Modifier.weight(1f)) { viewModel.onCalcKey("C") }
                        }
                        Row(gridRowMod, Arrangement.spacedBy(8.dp)) {
                            DigitBtn("1", Modifier.weight(1f)) { viewModel.onCalcKey("1") }
                            DigitBtn("2", Modifier.weight(1f)) { viewModel.onCalcKey("2") }
                            DigitBtn("3", Modifier.weight(1f)) { viewModel.onCalcKey("3") }
                            OperatorBtn("−", Modifier.weight(1f)) { viewModel.onCalcKey("-") }
                            OperatorBtn("+", Modifier.weight(1f)) { viewModel.onCalcKey("+") }
                        }
                        Row(gridRowMod, Arrangement.spacedBy(8.dp)) {
                            DigitBtn("0", Modifier.weight(1f)) { viewModel.onCalcKey("0") }
                            DigitBtn(".", Modifier.weight(1f)) { viewModel.onCalcKey(".") }
                            OperatorBtn("(", Modifier.weight(1f)) { viewModel.onCalcKey("(") }
                            OperatorBtn(")", Modifier.weight(1f)) { viewModel.onCalcKey(")") }
                            Surface(
                                onClick = { viewModel.onCalcKey("=") },
                                modifier = Modifier.weight(1f).fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                shadowElevation = 4.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) { Text("=", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Normal) }
                            }
                        }
                        
                        // SPACE FOR FLOATING BOTTOM NAV
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }

    if (showHistorySheet) {
        ModalBottomSheet(onDismissRequest = { showHistorySheet = false }) {
            Column(Modifier.fillMaxWidth().padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Calculated History", fontWeight = FontWeight.Black, fontSize = 20.sp)
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearHistory() }) {
                            Icon(Icons.Default.DeleteSweep, "Clear All", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                LazyColumn(Modifier.fillMaxHeight(0.6f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(history) { item ->
                        Column(Modifier.fillMaxWidth().clickable { viewModel.onCalcKey("C"); viewModel.updateExpression(
                            TextFieldValue(item.expression, TextRange(item.expression.length))
                        ); showHistorySheet = false }) {
                            Text(item.expression, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("= ${item.result}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DigitBtn(text: String, modifier: Modifier, onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true),
        shape = CircleShape,
        color = if (isDark) Color(0xFF2C2E31) else Color(0xFFF1F3F4)
    ) {
        Box(contentAlignment = Alignment.Center) { 
            Text(
                text = text, 
                fontSize = 26.sp, 
                fontWeight = FontWeight.Normal,
                style = LocalTextStyle.current.copy(
                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                )
            ) 
        }
    }
}

@Composable
fun OperatorBtn(text: String, modifier: Modifier, onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true),
        shape = CircleShape,
        color = if (isDark) Color(0xFF423B50) else Color(0xFFEDE6FA)
    ) {
        Box(contentAlignment = Alignment.Center) { 
            when(text) {
                "+" -> Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                "-" -> Icon(Icons.Default.Remove, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                "−" -> Icon(Icons.Default.Remove, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                "×" -> Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                "÷" -> Text("÷", fontSize = 30.sp, color = MaterialTheme.colorScheme.primary)
                "(" -> Text("(", fontSize = 26.sp, color = MaterialTheme.colorScheme.primary)
                ")" -> Text(")", fontSize = 26.sp, color = MaterialTheme.colorScheme.primary)
                else -> Text(text, fontSize = 26.sp, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun ControlBtn(text: String, modifier: Modifier, isDelete: Boolean = false, onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true),
        shape = CircleShape,
        color = if (isDark) Color(0xFF3C4043) else Color(0xFFF8F9FA)
    ) {
        Box(contentAlignment = Alignment.Center) { 
            if (text == "⌫") {
                Icon(Icons.AutoMirrored.Filled.Backspace, null, tint = Color(0xFFD93025), modifier = Modifier.size(26.dp))
            } else {
                Text(
                    text = text, 
                    fontSize = 22.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = if (isDelete) Color(0xFFD93025) else MaterialTheme.colorScheme.onSurface,
                    style = LocalTextStyle.current.copy(
                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}

@Composable
fun ScientificBtn(text: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Box(contentAlignment = Alignment.Center) { 
            Text(
                text = text, 
                fontSize = 15.sp, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.primary,
                style = LocalTextStyle.current.copy(
                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                )
            ) 
        }
    }
}

@Composable
fun CustomCursorTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
    cursorBrush: Brush = SolidColor(Color.Black),
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    placeholder: String = ""
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val cursorAlpha = remember { Animatable(1f) }
    
    // Blinking animation
    LaunchedEffect(value.selection, value.text) {
        while (true) {
            cursorAlpha.snapTo(1f)
            delay(500.milliseconds)
            cursorAlpha.animateTo(0f, animationSpec = tween(500))
            delay(500.milliseconds)
        }
    }

    val transformedText = remember(value.text, visualTransformation) {
        visualTransformation.filter(androidx.compose.ui.text.AnnotatedString(value.text))
    }

    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    val currentValue = rememberUpdatedState(value)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    textLayoutResult?.let { layout ->
                        val transformedIndex = layout.getOffsetForPosition(offset)
                        val originalIndex = transformedText.offsetMapping.transformedToOriginal(transformedIndex)
                        onValueChange(currentValue.value.copy(selection = TextRange(originalIndex)))
                    }
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulator += dragAmount
                        val sensitivity = 5f 
                        if (kotlin.math.abs(dragAccumulator) > sensitivity) {
                            val direction = if (dragAccumulator > 0) 1 else -1
                            val newIdx = (currentValue.value.selection.start + direction).coerceIn(0, currentValue.value.text.length)
                            onValueChange(currentValue.value.copy(selection = TextRange(newIdx)))
                            dragAccumulator = 0f
                        }
                    }
                )
            }
    ) {
        if (value.text.isEmpty() && placeholder.isNotEmpty()) {
            Text(
                text = placeholder,
                style = textStyle.copy(color = textStyle.color.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Text(
            text = transformedText.text,
            style = textStyle,
            onTextLayout = { textLayoutResult = it },
            modifier = Modifier.fillMaxWidth()
        )

        // Draw custom blinking cursor
        textLayoutResult?.let { layout ->
            if (value.selection.collapsed) {
                val transformedIndex = transformedText.offsetMapping.originalToTransformed(value.selection.start)
                val safeIndex = transformedIndex.coerceIn(0, layout.layoutInput.text.length)
                val cursorRect = layout.getCursorRect(safeIndex)
                
                Box(
                    modifier = Modifier
                        .offset(
                            x = with(LocalDensity.current) { cursorRect.left.toDp() },
                            y = with(LocalDensity.current) { cursorRect.top.toDp() }
                        )
                        .size(
                            width = 2.dp,
                            height = with(LocalDensity.current) { cursorRect.height.toDp() }
                        )
                        .alpha(cursorAlpha.value)
                        .background(cursorBrush)
                )
            }
        }
    }
}
