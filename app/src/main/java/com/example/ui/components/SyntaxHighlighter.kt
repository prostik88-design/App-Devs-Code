package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.SyntaxAnnotation
import com.example.ui.theme.SyntaxComment
import com.example.ui.theme.SyntaxFunction
import com.example.ui.theme.SyntaxKeyword
import com.example.ui.theme.SyntaxNumber
import com.example.ui.theme.SyntaxString
import com.example.ui.theme.SyntaxTag
import com.example.ui.theme.SyntaxType
import java.util.regex.Pattern

object SyntaxHighlighter {

    private val KOTLIN_KEYWORDS = setOf(
        "package", "import", "class", "interface", "object", "val", "var", "fun", "return",
        "if", "else", "when", "for", "while", "do", "try", "catch", "finally", "throw",
        "private", "protected", "public", "internal", "override", "open", "abstract",
        "data", "sealed", "enum", "companion", "suspend", "inline", "reified", "crossinline",
        "noinline", "tailrec", "operator", "infix", "const", "lateinit", "is", "as", "in",
        "out", "by", "get", "set", "true", "false", "null", "this", "super", "it"
    )

    private val COMMON_TYPES = setOf(
        "String", "Int", "Long", "Double", "Float", "Boolean", "List", "Map", "Set",
        "Flow", "StateFlow", "MutableStateFlow", "LiveData", "CoroutineScope", "Job",
        "Deferred", "Context", "Modifier", "Composable", "Unit", "Any", "Nothing",
        "Array", "ArrayList", "HashMap", "Dao", "Entity", "Database", "ViewModel"
    )

    fun highlight(code: String, language: String = "kotlin"): AnnotatedString {
        return buildAnnotatedString {
            append(code)

            if (code.isEmpty()) return@buildAnnotatedString

            // 1. Strings: "..." or '...'
            val stringPattern = Pattern.compile("\"(\\\\.|[^\"])*\"|'(\\\\.|[^'])*'")
            val stringMatcher = stringPattern.matcher(code)
            while (stringMatcher.find()) {
                addStyle(
                    SpanStyle(color = SyntaxString),
                    stringMatcher.start(),
                    stringMatcher.end()
                )
            }

            // 2. Comments: // ... or /* ... */
            val commentPattern = Pattern.compile("//.*|/\\*[\\s\\S]*?\\*/")
            val commentMatcher = commentPattern.matcher(code)
            while (commentMatcher.find()) {
                addStyle(
                    SpanStyle(color = SyntaxComment, fontWeight = FontWeight.Normal),
                    commentMatcher.start(),
                    commentMatcher.end()
                )
            }

            // 3. Numbers: integers, floats, hex
            val numberPattern = Pattern.compile("\\b(0x[0-9a-fA-F]+|\\d+(\\.\\d+)?([fFL])?)\\b")
            val numberMatcher = numberPattern.matcher(code)
            while (numberMatcher.find()) {
                addStyle(
                    SpanStyle(color = SyntaxNumber),
                    numberMatcher.start(),
                    numberMatcher.end()
                )
            }

            // 4. Annotations: @Composable, @Entity, etc.
            val annotationPattern = Pattern.compile("@[A-Za-z0-9_]+")
            val annotationMatcher = annotationPattern.matcher(code)
            while (annotationMatcher.find()) {
                addStyle(
                    SpanStyle(color = SyntaxAnnotation, fontWeight = FontWeight.SemiBold),
                    annotationMatcher.start(),
                    annotationMatcher.end()
                )
            }

            // 5. Word tokens (Keywords, Types, Functions)
            val wordPattern = Pattern.compile("\\b[A-Za-z_][A-Za-z0-9_]*\\b")
            val wordMatcher = wordPattern.matcher(code)
            while (wordMatcher.find()) {
                val word = wordMatcher.group()
                val start = wordMatcher.start()
                val end = wordMatcher.end()

                if (KOTLIN_KEYWORDS.contains(word)) {
                    addStyle(
                        SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold),
                        start,
                        end
                    )
                } else if (COMMON_TYPES.contains(word) || (word.first().isUpperCase() && !word.all { it.isUpperCase() })) {
                    addStyle(
                        SpanStyle(color = SyntaxType, fontWeight = FontWeight.SemiBold),
                        start,
                        end
                    )
                }
            }

            // 6. XML / HTML tags
            if (language.equals("xml", ignoreCase = true) || language.equals("html", ignoreCase = true)) {
                val tagPattern = Pattern.compile("</?[A-Za-z0-9_:-]+|/?>")
                val tagMatcher = tagPattern.matcher(code)
                while (tagMatcher.find()) {
                    addStyle(
                        SpanStyle(color = SyntaxTag, fontWeight = FontWeight.Bold),
                        tagMatcher.start(),
                        tagMatcher.end()
                    )
                }
            }
        }
    }
}
