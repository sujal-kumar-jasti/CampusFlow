package com.example.ui

import android.app.Application
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.CalculatorHistory
import com.example.util.ScientificCalculatorEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(AppDatabase.getDatabase(application))

    private val _expression = MutableStateFlow(TextFieldValue(""))
    val expression: StateFlow<TextFieldValue> = _expression.asStateFlow()

    private val _result = MutableStateFlow("0")
    val result: StateFlow<String> = _result.asStateFlow()

    private val _isResultFinal = MutableStateFlow(false)
    val isResultFinal: StateFlow<Boolean> = _isResultFinal.asStateFlow()

    private val _angleUnit = MutableStateFlow(ScientificCalculatorEngine.AngleUnit.DEG)
    val angleUnit: StateFlow<ScientificCalculatorEngine.AngleUnit> = _angleUnit.asStateFlow()

    private val _isShiftActive = MutableStateFlow(false)
    val isShiftActive: StateFlow<Boolean> = _isShiftActive.asStateFlow()

    val history: StateFlow<List<CalculatorHistory>> = repository.calculatorHistory
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun updateExpression(value: TextFieldValue) {
        _expression.value = value
        _isResultFinal.value = false
        // Live evaluation as the user types - but DON'T show Errors yet
        if (value.text.isNotEmpty()) {
            val eval = ScientificCalculatorEngine.evaluate(value.text, _angleUnit.value)
            if (!isError(eval)) {
                _result.value = eval
            }
        } else {
            _result.value = "0"
        }
    }

    fun toggleShift() { _isShiftActive.value = !_isShiftActive.value }

    fun onCalcKey(key: String) {
        when (key) {
            "C" -> {
                _expression.value = TextFieldValue("")
                _result.value = "0"
                _isResultFinal.value = false
            }
            "⌫" -> {
                if (_isResultFinal.value) { onCalcKey("C"); return }
                val curr = _expression.value
                if (curr.text.isNotEmpty() && curr.selection.start > 0) {
                    val before = curr.text.substring(0, curr.selection.start - 1)
                    val after = curr.text.substring(curr.selection.end)
                    val nextText = before + after
                    _expression.value = TextFieldValue(nextText, TextRange(before.length))
                    
                    if (nextText.isNotEmpty()) {
                        val eval = ScientificCalculatorEngine.evaluate(nextText, _angleUnit.value)
                        if (!isError(eval)) _result.value = eval
                    } else {
                        _result.value = "0"
                    }
                }
            }
            "=" -> {
                val expr = _expression.value.text
                if (expr.isNotEmpty()) {
                    val res = ScientificCalculatorEngine.evaluate(expr, _angleUnit.value)
                    _result.value = res
                    _isResultFinal.value = true
                    if (!res.startsWith("Error") && !res.contains("Mismatched") && !res.contains("Syntax")) {
                        viewModelScope.launch {
                            repository.addCalculatorHistory(CalculatorHistory(expression = expr, result = res))
                        }
                    }
                }
            }
            "DEG", "RAD" -> {
                _angleUnit.value = if (_angleUnit.value == ScientificCalculatorEngine.AngleUnit.DEG) 
                    ScientificCalculatorEngine.AngleUnit.RAD else ScientificCalculatorEngine.AngleUnit.DEG
                if (_expression.value.text.isNotEmpty()) {
                    val eval = ScientificCalculatorEngine.evaluate(_expression.value.text, _angleUnit.value)
                    if (!isError(eval)) _result.value = eval
                }
            }
            "log" -> {
                val curr = _expression.value
                val nextText = curr.text.substring(0, curr.selection.start) + "log()" + curr.text.substring(curr.selection.end)
                // Position: log[cursor]()
                val newPos = curr.selection.start + 3
                _expression.value = TextFieldValue(nextText, TextRange(newPos))
            }
            "root" -> {
                val curr = _expression.value
                val nextText = curr.text.substring(0, curr.selection.start) + "√()" + curr.text.substring(curr.selection.end)
                // Position: [cursor]√()
                val newPos = curr.selection.start
                _expression.value = TextFieldValue(nextText, TextRange(newPos))
            }
            else -> {
                if (_isResultFinal.value) {
                    val lastRes = _result.value
                    onCalcKey("C")
                    if (key in "+-*/^%cp") {
                        _expression.value = TextFieldValue(lastRes, TextRange(lastRes.length))
                    }
                }
                
                val curr = _expression.value
                val nextText = curr.text.substring(0, curr.selection.start) + key + curr.text.substring(curr.selection.end)
                val newCursor = curr.selection.start + key.length
                
                _expression.value = TextFieldValue(nextText, TextRange(newCursor))
                val eval = ScientificCalculatorEngine.evaluate(nextText, _angleUnit.value)
                if (!isError(eval)) _result.value = eval
            }
        }
    }

    private fun isError(res: String): Boolean {
        return res.startsWith("Error") || res.contains("Syntax Error") || res.contains("Mismatched")
    }

    fun processVoiceCommand(text: String) {
        val processed = text.lowercase()
            .replace("multiplied by", "*").replace("times", "*").replace("into", "*")
            .replace("divided by", "/").replace("over", "/")
            .replace("plus", "+").replace("minus", "-")
            .replace("point", ".").replace("dot", ".")
            .replace("to the power of", "^").replace("power", "^")
            .replace("root", "sqrt(").replace("square root", "sqrt(")
            .replace("sine", "sin(").replace("cosine", "cos(").replace("tangent", "tan(")
            .replace("choose", "c").replace("permutation", "p")
            .replace("factorial", "!")
            .replace(Regex("(\\d)\\s+c\\s+(\\d)"), "$1c$2")
            .replace(Regex("(\\d)\\s+p\\s+(\\d)"), "$1p$2")
            .replace(Regex("(\\d)\\s+(\\d)"), "$1$2")
            .replace(" ", "")

        _expression.value = TextFieldValue(processed, TextRange(processed.length))
        onCalcKey("=")
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearCalculatorHistory() }
    }
}
