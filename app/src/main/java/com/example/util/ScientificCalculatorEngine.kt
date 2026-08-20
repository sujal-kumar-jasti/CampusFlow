package com.example.util

import org.mariuszgromada.math.mxparser.Expression
import org.mariuszgromada.math.mxparser.License
import org.mariuszgromada.math.mxparser.mXparser
import java.util.Locale
import kotlin.math.*

object ScientificCalculatorEngine {

    enum class AngleUnit { DEG, RAD }

    init {
        License.iConfirmNonCommercialUse("CampusPulse")
        mXparser.setDegreesMode() // Default to DEG
    }

    fun evaluate(expression: String, angleUnit: AngleUnit = AngleUnit.DEG): String {
        try {
            if (expression.isBlank()) return "0"
            
            if (angleUnit == AngleUnit.DEG) mXparser.setDegreesMode() 
            else mXparser.setRadiansMode()

            val sanitized = sanitizeForMXParser(expression)
            val e = Expression(sanitized)
            
            if (!e.checkSyntax()) {
                val msg = e.errorMessage.trim()
                return if (msg.contains("parenthesis")) "Mismatched ()" else "Syntax Error"
            }
            
            val result = e.calculate()
            if (result.isNaN()) return "Error"
            
            return formatResult(result)
        } catch (e: Exception) {
            return e.message ?: "Syntax Error"
        }
    }

    private fun sanitizeForMXParser(expr: String): String {
        var sanitized = expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("π", "pi")
            .replace("∛", "cbrt")
            .replace(" ", "")

        // logBASE(value) -> log(base, value)
        val logBaseRegex = Regex("log([a-zA-Z0-9.]+)\\(([^)]+)\\)")
        sanitized = logBaseRegex.replace(sanitized) { match ->
            "log(${match.groupValues[1]},${match.groupValues[2]})"
        }

        // INDEX√value or INDEX√(value) -> root(index, value)
        val rootIndexRegex = Regex("([a-zA-Z0-9.]+)?√(\\(([^)]+)\\)|([a-zA-Z0-9.]+))")
        sanitized = rootIndexRegex.replace(sanitized) { match ->
            val index = if (match.groupValues[1].isEmpty()) "2" else match.groupValues[1]
            val value = if (match.groupValues[3].isNotEmpty()) match.groupValues[3] else match.groupValues[4]
            "root($index,$value)"
        }

        // Support 56c2 -> nCk(56, 2)
        val nCrRegex = Regex("([0-9.]+|\\((?:[^()]|\\([^()]*\\))*\\))[cC]([0-9.]+|\\((?:[^()]|\\([^()]*\\))*\\))")
        sanitized = nCrRegex.replace(sanitized) { match ->
            "nCk(${match.groupValues[1]},${match.groupValues[2]})"
        }

        // Support 2p2 -> nPk(2, 2)
        val nPrRegex = Regex("([0-9.]+|\\((?:[^()]|\\([^()]*\\))*\\))[pP]([0-9.]+|\\((?:[^()]|\\([^()]*\\))*\\))")
        sanitized = nPrRegex.replace(sanitized) { match ->
            "nPk(${match.groupValues[1]},${match.groupValues[2]})"
        }

        android.util.Log.d("ScientificCalc", "Sanitized: $expr -> $sanitized")
        return sanitized
    }

    private fun formatResult(value: Double): String {
        if (value.isNaN()) return "Error"
        if (value.isInfinite()) return "Infinity"
        val longVal = value.toLong()
        if (value == longVal.toDouble()) return longVal.toString()
        if (abs(value) > 1e12 || (abs(value) < 1e-6 && value != 0.0)) {
            return String.format(Locale.getDefault(), "%.6e", value)
        }
        return String.format(Locale.getDefault(), "%.8f", value).trimEnd('0').trimEnd('.')
    }
}
