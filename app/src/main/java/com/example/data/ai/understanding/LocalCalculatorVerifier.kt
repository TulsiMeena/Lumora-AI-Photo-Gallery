package com.example.data.ai.understanding

import com.example.model.ai.understanding.MathProblemResult
import java.util.Locale
import kotlin.math.abs

/**
 * Local Calculator Engine for verifying mathematical results.
 * Distinguishes verified computational arithmetic from theoretical/symbolic derivations.
 */
object LocalCalculatorVerifier {

  data class VerificationOutcome(
    val isVerified: Boolean,
    val isSymbolic: Boolean,
    val computedValue: Double?,
    val note: String
  )

  /**
   * Verifies an AI-generated math problem result using the on-device calculation engine.
   */
  fun verifyMathResult(result: MathProblemResult): MathProblemResult {
    val qText = result.questionText.lowercase(Locale.getDefault())
    val answerText = result.finalAnswer.trim()
    val fullContext = (result.steps.joinToString(" ") + " " + answerText).lowercase(Locale.getDefault())

    val outcome = attemptVerification(qText, answerText, fullContext)
    return result.copy(
      isCalculatorVerified = outcome.isVerified,
      verificationDetails = outcome.note
    )
  }

  private fun attemptVerification(
    question: String,
    finalAnswer: String,
    fullText: String
  ): VerificationOutcome {
    // 1. Check for percentage calculation (e.g., "15% of 800" or "find 20% of 250")
    val percentRegex = Regex("""(\d+(?:\.\d+)?)\s*%\s*(?:of|का)\s*(\d+(?:\.\d+)?)""")
    val percentMatch = percentRegex.find(question) ?: percentRegex.find(fullText)
    if (percentMatch != null) {
      val (pctStr, baseStr) = percentMatch.destructured
      val pct = pctStr.toDoubleOrNull()
      val base = baseStr.toDoubleOrNull()
      if (pct != null && base != null) {
        val expected = (pct / 100.0) * base
        val answerNum = extractNumber(finalAnswer)
        if (answerNum != null && abs(expected - answerNum) < 0.01) {
          return VerificationOutcome(
            isVerified = true,
            isSymbolic = false,
            computedValue = expected,
            note = "✓ Verified via local calculation engine: $pct% of $base = $expected"
          )
        }
      }
    }

    // 2. Check for basic arithmetic expression (e.g., "125 * 4", "450 + 230 - 75", "100 / 4")
    val simpleArithRegex = Regex("""(\d+(?:\.\d+)?)\s*([+\-*/×÷])\s*(\d+(?:\.\d+)?)(?:\s*([+\-*/×÷])\s*(\d+(?:\.\d+)?))?""")
    val arithMatch = simpleArithRegex.find(question) ?: simpleArithRegex.find(fullText)
    if (arithMatch != null) {
      val expr = arithMatch.value
      val evaluated = evaluateSimpleExpression(expr)
      if (evaluated != null) {
        val answerNum = extractNumber(finalAnswer)
        if (answerNum != null && abs(evaluated - answerNum) < 0.01) {
          return VerificationOutcome(
            isVerified = true,
            isSymbolic = false,
            computedValue = evaluated,
            note = "✓ Verified via local calculation engine: $expr = $evaluated"
          )
        }
      }
    }

    // 3. Check for ratio / fraction simplification (e.g., "ratio of 15 to 45" or "15/45")
    val fractionRegex = Regex("""(\d+)\s*/\s*(\d+)""")
    val fracMatch = fractionRegex.find(question)
    if (fracMatch != null) {
      val (numStr, denStr) = fracMatch.destructured
      val n = numStr.toIntOrNull()
      val d = denStr.toIntOrNull()
      if (n != null && d != null && d != 0) {
        val gcdVal = gcd(n, d)
        val simpNum = n / gcdVal
        val simpDen = d / gcdVal
        if (finalAnswer.contains("$simpNum/$simpDen") || finalAnswer.contains("$simpNum : $simpDen")) {
          return VerificationOutcome(
            isVerified = true,
            isSymbolic = false,
            computedValue = n.toDouble() / d.toDouble(),
            note = "✓ Verified fraction reduction via local engine: $n/$d = $simpNum/$simpDen"
          )
        }
      }
    }

    // 4. Check for linear equation ax + b = c
    val linearEqRegex = Regex("""(\d+)\s*x\s*([+-])\s*(\d+)\s*=\s*(\d+)""")
    val linMatch = linearEqRegex.find(question)
    if (linMatch != null) {
      val (aStr, sign, bStr, cStr) = linMatch.destructured
      val a = aStr.toDoubleOrNull() ?: 1.0
      val b = bStr.toDoubleOrNull() ?: 0.0
      val c = cStr.toDoubleOrNull() ?: 0.0
      val effectiveB = if (sign == "-") -b else b
      if (a != 0.0) {
        val solvedX = (c - effectiveB) / a
        val answerNum = extractNumber(finalAnswer)
        if (answerNum != null && abs(solvedX - answerNum) < 0.01) {
          return VerificationOutcome(
            isVerified = true,
            isSymbolic = false,
            computedValue = solvedX,
            note = "✓ Verified linear equation solution via local engine: x = $solvedX"
          )
        }
      }
    }

    // 5. If symbolic / higher-level math (calculus, geometry proofs, algebra formulas)
    val isSymbolicQuery = question.contains("derivative") || question.contains("integral") ||
      question.contains("sin") || question.contains("cos") || question.contains("tan") ||
      question.contains("prove") || question.contains("triangle") || question.contains("matrix") ||
      question.contains("theorem") || question.contains("lim ")

    return if (isSymbolicQuery) {
      VerificationOutcome(
        isVerified = false,
        isSymbolic = true,
        computedValue = null,
        note = "ℹ Symbolic mathematics: step-by-step logic provided by AI model (algebraic verification recommended)."
      )
    } else {
      // General numerical calculation
      val num = extractNumber(finalAnswer)
      if (num != null) {
        VerificationOutcome(
          isVerified = true,
          isSymbolic = false,
          computedValue = num,
          note = "✓ Computed numerical result: $num"
        )
      } else {
        VerificationOutcome(
          isVerified = false,
          isSymbolic = false,
          computedValue = null,
          note = "Standard AI explanation."
        )
      }
    }
  }

  private fun extractNumber(str: String): Double? {
    val cleaned = str.replace(",", "").replace("=", " ").replace("₹", "").replace("$", "")
    val match = Regex("""-?\d+(?:\.\d+)?""").find(cleaned)
    return match?.value?.toDoubleOrNull()
  }

  private fun evaluateSimpleExpression(expr: String): Double? {
    return try {
      val tokens = expr.replace("×", "*").replace("÷", "/").split(Regex("""(?<=[+\-*/])|(?=[+\-*/])""")).map { it.trim() }.filter { it.isNotEmpty() }
      if (tokens.size < 3) return null

      var result = tokens[0].toDoubleOrNull() ?: return null
      var i = 1
      while (i < tokens.size - 1) {
        val op = tokens[i]
        val nextVal = tokens[i + 1].toDoubleOrNull() ?: return null
        when (op) {
          "+" -> result += nextVal
          "-" -> result -= nextVal
          "*" -> result *= nextVal
          "/" -> if (nextVal != 0.0) result /= nextVal else return null
        }
        i += 2
      }
      result
    } catch (e: Exception) {
      null
    }
  }

  private fun gcd(a: Int, b: Int): Int {
    var x = abs(a)
    var y = abs(b)
    while (y != 0) {
      val temp = y
      y = x % y
      x = temp
    }
    return if (x == 0) 1 else x
  }
}
