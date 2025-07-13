// ui/components/MarkdownParser.kt
package com.example.echospace.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

data class MarkdownElement(
    val type: MarkdownType,
    val content: String,
    val level: Int = 0
)

enum class MarkdownType {
    HEADING1, HEADING2, BULLET_POINT, BLOCKQUOTE, PARAGRAPH, BLANK_LINE
}

object MarkdownParser {
    fun parseMarkdown(text: String): List<MarkdownElement> {
        val lines = text.split("\n")
        val elements = mutableListOf<MarkdownElement>()

        for (line in lines) {
            when {
                line.startsWith("# ") -> {
                    elements.add(MarkdownElement(MarkdownType.HEADING1, line.substring(2)))
                }
                line.startsWith("## ") -> {
                    elements.add(MarkdownElement(MarkdownType.HEADING2, line.substring(3)))
                }
                line.startsWith("- ") -> {
                    elements.add(MarkdownElement(MarkdownType.BULLET_POINT, line.substring(2)))
                }
                line.startsWith("> ") -> {
                    elements.add(MarkdownElement(MarkdownType.BLOCKQUOTE, line.substring(2)))
                }
                line.isBlank() -> {
                    // Preserve blank lines for spacing
                    elements.add(MarkdownElement(MarkdownType.BLANK_LINE, ""))
                }
                else -> {
                    // Non-blank lines become paragraphs
                    elements.add(MarkdownElement(MarkdownType.PARAGRAPH, line))
                }
            }
        }

        return elements
    }

    fun parseInlineFormatting(
        text: String,
        quoteColor: Color? = null
    ): AnnotatedString {
        val builder = AnnotatedString.Builder()
        var currentIndex = 0

        while (currentIndex < text.length) {
            when {
                // Bold formatting **text**
                text.substring(currentIndex).startsWith("**") -> {
                    val endIndex = text.indexOf("**", currentIndex + 2)
                    if (endIndex != -1) {
                        val boldText = text.substring(currentIndex + 2, endIndex)
                        builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(boldText)
                        }
                        currentIndex = endIndex + 2
                    } else {
                        builder.append(text[currentIndex])
                        currentIndex++
                    }
                }
                // Italic formatting *text*
                text.substring(currentIndex).startsWith("*") &&
                        !text.substring(currentIndex).startsWith("**") -> {
                    val endIndex = text.indexOf("*", currentIndex + 1)
                    if (endIndex != -1) {
                        val italicText = text.substring(currentIndex + 1, endIndex)
                        builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(italicText)
                        }
                        currentIndex = endIndex + 1
                    } else {
                        builder.append(text[currentIndex])
                        currentIndex++
                    }
                }
                // Inline quotes "text"
                text[currentIndex] == '"' -> {
                    val endIndex = text.indexOf('"', currentIndex + 1)
                    if (endIndex != -1) {
                        val quotedText = text.substring(currentIndex + 1, endIndex)
                        val quoteStyle = if (quoteColor != null) {
                            SpanStyle(
                                fontStyle = FontStyle.Italic,
                                color = quoteColor
                            )
                        } else {
                            SpanStyle(fontStyle = FontStyle.Italic)
                        }

                        builder.withStyle(quoteStyle) {
                            append("\"$quotedText\"")
                        }
                        currentIndex = endIndex + 1
                    } else {
                        builder.append(text[currentIndex])
                        currentIndex++
                    }
                }
                else -> {
                    builder.append(text[currentIndex])
                    currentIndex++
                }
            }
        }

        return builder.toAnnotatedString()
    }
}
