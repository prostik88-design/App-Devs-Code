package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonCyan

sealed class MarkdownElement {
    data class Header(val level: Int, val text: String) : MarkdownElement()
    data class Paragraph(val text: String) : MarkdownElement()
    data class BulletItem(val text: String) : MarkdownElement()
    data class CodeBlock(val language: String, val code: String) : MarkdownElement()
}

@Composable
fun MarkdownViewer(
    content: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val elements = remember(content) { parseMarkdown(content) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        elements.forEach { element ->
            when (element) {
                is MarkdownElement.Header -> {
                    val style = when (element.level) {
                        1 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = NeonCyan)
                        2 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        else -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = textColor)
                    }
                    Text(
                        text = parseInlineFormatting(element.text, textColor),
                        style = style,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                is MarkdownElement.Paragraph -> {
                    Text(
                        text = parseInlineFormatting(element.text, textColor),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        lineHeight = 20.sp
                    )
                }
                is MarkdownElement.BulletItem -> {
                    Row(
                        modifier = Modifier.padding(start = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "•",
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = parseInlineFormatting(element.text, textColor),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            lineHeight = 20.sp
                        )
                    }
                }
                is MarkdownElement.CodeBlock -> {
                    CodeBlockView(
                        code = element.code,
                        language = element.language
                    )
                }
            }
        }
    }
}

private fun parseMarkdown(raw: String): List<MarkdownElement> {
    val elements = mutableListOf<MarkdownElement>()
    val lines = raw.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // Code block start
        if (line.trim().startsWith("```")) {
            val lang = line.trim().removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            // Skip the closing ```
            if (i < lines.size) i++
            elements.add(MarkdownElement.CodeBlock(lang, codeLines.joinToString("\n")))
            continue
        }

        // Headers
        if (line.startsWith("# ")) {
            elements.add(MarkdownElement.Header(1, line.removePrefix("# ").trim()))
            i++
            continue
        }
        if (line.startsWith("## ")) {
            elements.add(MarkdownElement.Header(2, line.removePrefix("## ").trim()))
            i++
            continue
        }
        if (line.startsWith("### ")) {
            elements.add(MarkdownElement.Header(3, line.removePrefix("### ").trim()))
            i++
            continue
        }
        if (line.startsWith("#### ")) {
            elements.add(MarkdownElement.Header(4, line.removePrefix("#### ").trim()))
            i++
            continue
        }

        // Bullet points
        if (line.trim().startsWith("- ") || line.trim().startsWith("* ")) {
            elements.add(MarkdownElement.BulletItem(line.trim().substring(2)))
            i++
            continue
        }

        // Numbered list
        val numberedMatch = Regex("^\\d+\\.\\s+(.*)").find(line.trim())
        if (numberedMatch != null) {
            elements.add(MarkdownElement.BulletItem(numberedMatch.groupValues[1]))
            i++
            continue
        }

        // Regular paragraph if not empty
        if (line.isNotBlank()) {
            elements.add(MarkdownElement.Paragraph(line.trim()))
        }
        i++
    }

    return elements
}

private fun parseInlineFormatting(text: String, defaultColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val regex = Regex("(\\*\\*([^*]+)\\*\\*)|(`([^`]+)`)|(\\*([^*]+)\\*)")
        val matches = regex.findAll(text)

        for (match in matches) {
            if (match.range.first > cursor) {
                append(text.substring(cursor, match.range.first))
            }

            val fullMatch = match.value
            when {
                fullMatch.startsWith("**") -> {
                    val boldText = fullMatch.removeSurrounding("**")
                    val start = length
                    append(boldText)
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = defaultColor), start, length)
                }
                fullMatch.startsWith("`") -> {
                    val inlineCode = fullMatch.removeSurrounding("`")
                    val start = length
                    append(inlineCode)
                    addStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonCyan,
                            background = Color(0x3338BDF8)
                        ),
                        start,
                        length
                    )
                }
                fullMatch.startsWith("*") -> {
                    val italicText = fullMatch.removeSurrounding("*")
                    val start = length
                    append(italicText)
                    addStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), start, length)
                }
            }
            cursor = match.range.last + 1
        }

        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}
