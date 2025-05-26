package com.example.calculator

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import java.util.*
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    private lateinit var txtDisplay: TextView
    private lateinit var txtHistory: TextView
    private lateinit var sidePanel: GridLayout
    private lateinit var sidePanelToggle: Button
    private lateinit var panelGuideline: Guideline

    private val undoStack = Stack<String>()
    private val redoStack = Stack<String>()
    private var lastResult: String = ""
    private var isResultDisplayed = false
    private var isPanelOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtDisplay = findViewById(R.id.txtDisplay)
        txtHistory = findViewById(R.id.txtHistory)
        sidePanel = findViewById(R.id.sidePanel)
        sidePanelToggle = findViewById(R.id.sidePanelToggle)
        panelGuideline = findViewById(R.id.panelGuideline)

        // Всі кнопки
        val btns: Map<Int, String> = mapOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2", R.id.btn3 to "3", R.id.btn4 to "4",
            R.id.btn5 to "5", R.id.btn6 to "6", R.id.btn7 to "7", R.id.btn8 to "8", R.id.btn9 to "9",
            R.id.btnPlus to "+", R.id.btnMinus to "-", R.id.btnMult to "×", R.id.btnDivide to "÷",
            R.id.btnDot to ",", R.id.btnProcent to "%", R.id.btnEpsilon to "e"
        )

        btns.forEach { (id, str) ->
            findViewById<Button>(id)?.setOnClickListener { insertToDisplay(str) }
        }
        // Додаткові
        findViewById<Button>(R.id.btnSqrt)?.setOnClickListener { insertToDisplay("√") }
        findViewById<Button>(R.id.btnDegree)?.setOnClickListener { insertToDisplay("^") }
        findViewById<Button>(R.id.btnLogariphm)?.setOnClickListener { insertToDisplay("ln") }

        findViewById<Button>(R.id.btnPosNeg)?.setOnClickListener { btnPosNeg_Click() }
        findViewById<Button>(R.id.btnClear)?.setOnClickListener { btnClear_Click() }
        findViewById<Button>(R.id.btnUndo)?.setOnClickListener { btnUndo_Click() }
        findViewById<Button>(R.id.btnRedo)?.setOnClickListener { btnRedo_Click() }
        findViewById<Button>(R.id.btnEquals)?.setOnClickListener { btnEquals_Click() }

        sidePanelToggle.setOnClickListener {
            isPanelOpen = !isPanelOpen
            val panelGuideline = findViewById<Guideline>(R.id.panelGuideline)
            val params = panelGuideline.layoutParams as ConstraintLayout.LayoutParams
            if (isPanelOpen) {
                params.guidePercent = 0.80f // 80% ширини на основну клаву, 20% — на панель
                sidePanel.visibility = View.VISIBLE
            } else {
                params.guidePercent = 1.0f // 100% — тільки основна клава
                sidePanel.visibility = View.GONE
            }
            panelGuideline.layoutParams = params

        }
    }

    // Додає до дисплея новий символ
    private fun insertToDisplay(value: String) {
        val operators = listOf("+", "-", "×", "÷", "^", "%")
        val functions = listOf("ln", "√", "^", "e") // додаємо функції
        val displayText = txtDisplay.text.toString()

        if (isResultDisplayed) {
            // Після результату: якщо вводимо число, починаємо новий вираз
            if (value in "0123456789," || value == "e" || value == "π") {
                undoStack.push(displayText)
                txtDisplay.text = value
            } else if (operators.contains(value)) {
                undoStack.push(displayText)
                txtDisplay.text = displayText + value
            } else {
                undoStack.push(displayText)
                txtDisplay.text = value
            }
            isResultDisplayed = false
        } else {
            // Якщо поле "0" або "Error" — замінити на функцію або цифру
            if ((displayText == "0" || displayText == "Error") &&
                (value in "123456789" || functions.contains(value) || value == "π" || value == "e" || value == "ln" || value == "√")) {
                undoStack.push(displayText)
                txtDisplay.text = value
            } else if ((displayText == "0" || displayText == "Error") && value == "0") {
                // якщо вже 0 і натиснули ще раз 0 — нічого не змінюємо
            } else {
                undoStack.push(displayText)
                txtDisplay.append(value)
            }
        }
        redoStack.clear()
    }



    private fun btnPosNeg_Click() {
        val disp = txtDisplay.text.toString()
        if (disp.isEmpty() || disp == "0" || disp == "Error") return
        undoStack.push(disp)
        txtDisplay.text =
            if (disp.startsWith("-")) disp.substring(1) else "-$disp"
        redoStack.clear()
    }

    private fun btnClear_Click() {
        undoStack.push(txtDisplay.text.toString())
        txtDisplay.text = "0"
        txtHistory.text = ""
        redoStack.clear()
    }

    private fun btnUndo_Click() {
        if (undoStack.isNotEmpty()) {
            redoStack.push(txtDisplay.text.toString())
            val prev = undoStack.pop()
            txtDisplay.text = prev
        }
    }


    private fun btnRedo_Click() {
        if (redoStack.isNotEmpty()) {
            undoStack.push(txtDisplay.text.toString())
            val prev = redoStack.pop()
            txtDisplay.text = prev
        }
    }

    private fun btnEquals_Click() {
        try {
            // Додаємо у undoStack і в redoStack попередній стан, щоб Undo після "=" повертав вираз
            undoStack.push(txtDisplay.text.toString())
            txtHistory.text = txtDisplay.text
            val result = evaluateExpression(txtDisplay.text.toString())
            lastResult = result
            txtDisplay.text = result
            isResultDisplayed = true
            redoStack.clear() // після обчислення redo скидаємо
        } catch (e: Exception) {
            txtDisplay.text = "Error"
        }
    }

    // Аналог C# функції з regex: заміни √, ln, ^, %, π, e
    private fun evaluateExpression(expr: String): String {
        var expression = expr
            .replace(",", ".")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("π", Math.PI.toString())
            .replace("e", Math.E.toString())

        // √number -> sqrt(number)
        expression = Regex("√([\\d.]+)").replace(expression) {
            sqrt(it.groupValues[1].toDouble()).toString()
        }
        // ln(number)
        expression = Regex("ln([\\d.]+)").replace(expression) {
            ln(it.groupValues[1].toDouble()).toString()
        }
        // a^b -> pow(a, b)
        expression = Regex("([\\d.]+)\\^([\\d.]+)").replace(expression) { match: MatchResult ->
            match.groupValues[1].toDouble().pow(match.groupValues[2].toDouble()).toString()
        }

        // X% (число перед %) = X/100
        expression = Regex("([\\d.]+)%").replace(expression) {
            (it.groupValues[1].toDouble() / 100).toString()
        }
        // X + Y% (де Y% після +, -, ×, ÷)
        expression = Regex("([\\d.]+)([+\\-*/])([\\d.]+)%").replace(expression) {
            val x = it.groupValues[1].toDouble()
            val op = it.groupValues[2]
            val y = it.groupValues[3].toDouble()
            val percent = x * y / 100
            "$x$op$percent"
        }
        // Використовуємо простий eval
        return simpleEval(expression)
    }

    //eval
    @SuppressLint("DefaultLocale")
    private fun simpleEval(expr: String): String {
        return try {
            val tokens = expr.replace(" ", "")
                .replace("--", "+")
                .replace("+-", "-")
            val res = evalBasic(tokens)
            // Форматування: якщо ціле — як int, якщо з дробом — як double
            if (res % 1.0 == 0.0) res.toInt().toString()
            else String.format("%.6f", res).trimEnd('0').trimEnd('.')
        } catch (e: Exception) {
            "Error"
        }
    }
    // Примітивний калькулятор (тільки + - * /)
    private fun evalBasic(expr: String): Double {
        var result = 0.0
        var current = ""
        var lastOp = '+'
        expr.forEach { ch ->
            if (ch in "0123456789.") {
                current += ch
            } else if (ch in "+-*/") {
                result = calc(result, current, lastOp)
                lastOp = ch
                current = ""
            }
        }
        return calc(result, current, lastOp)
    }

    private fun calc(a: Double, b: String, op: Char): Double {
        val num = b.toDoubleOrNull() ?: 0.0
        return when (op) {
            '+' -> a + num
            '-' -> a - num
            '*' -> a * num
            '/' -> a / num
            else -> num
        }
    }
}
