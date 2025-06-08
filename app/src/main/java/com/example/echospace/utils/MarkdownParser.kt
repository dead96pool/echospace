package com.example.echospace.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.sp

@Composable
fun parseMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split('\n')
        //var currentIndex = 0

        lines.forEachIndexed { index, line ->
            when {
                // Heading 1 - # Text
                line.startsWith("# ") && !line.startsWith("## ") -> {
                    pushStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    pushStyle(
                        ParagraphStyle(
                            textAlign = TextAlign.Center
                        )
                    )
                    append(line.substring(2)) // Remove "# "
                    pop() // Pop ParagraphStyle
                    pop() // Pop SpanStyle
                }

                // Heading 2 - ## Text
                line.startsWith("## ") -> {
                    pushStyle(
                        SpanStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    pushStyle(
                        ParagraphStyle(
                            textAlign = TextAlign.Center
                        )
                    )
                    append(line.substring(3)) // Remove "## "
                    pop() // Pop ParagraphStyle
                    pop() // Pop SpanStyle
                }

                // Quote - > Text
                line.startsWith("> ") -> {
                    pushStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = FontStyle.Italic
                        )
                    )
                    append("  ▌ ") // Indentation for quote
                    append(line.substring(2)) // Remove "> "
                    pop()
                }

                // Bullet point - * Text
                line.startsWith("* ") -> {
                    append("    • ") // Bullet point with indentation
                    append(parseInlineMarkdown(line.substring(2))) // Remove "* " and parse inline
                }

                // Hashtags at end of file - #tag1 #tag2
                line.startsWith("#") && line.contains("#") && !line.startsWith("# ") && !line.startsWith("## ") -> {
                    pushStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    )
                    append(line)
                    pop()
                }

                // Empty line - preserve spacing
                line.isBlank() -> {
                    // For empty lines, just add the newline without any content
                    // This preserves paragraph spacing
                }

                // Regular text with inline formatting
                else -> {
                    append(parseInlineMarkdown(line))
                }
            }

            // Add newline after every line except the very last one
            // This preserves all spacing including blank lines
            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}

@Composable
private fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                // Inline quotes - "text"
                i < text.length - 1 && text[i] == '"' -> {
                    val endIndex = text.indexOf('"', i + 1)
                    if (endIndex != -1) {
                        // Add opening quote symbol
                        pushStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                fontStyle = FontStyle.Italic
                            )
                        )
                        append('"') // Left double quotation mark
                        append(text.substring(i + 1, endIndex))
                        append('"') // Right double quotation mark
                        pop()
                        i = endIndex + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }

                // Bold-Italic - _**text**_
                i < text.length - 5 &&
                        text.substring(i, i + 3) == "_**" -> {
                    val endIndex = text.indexOf("**_", i + 3)
                    if (endIndex != -1) {
                        pushStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic
                            )
                        )
                        append(text.substring(i + 3, endIndex))
                        pop()
                        i = endIndex + 3
                    } else {
                        append(text[i])
                        i++
                    }
                }

                // Bold - **text**
                i < text.length - 3 &&
                        text.substring(i, i + 2) == "**" -> {
                    val endIndex = text.indexOf("**", i + 2)
                    if (endIndex != -1) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(text.substring(i + 2, endIndex))
                        pop()
                        i = endIndex + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }

                // Italic - _text_
                i < text.length - 1 && text[i] == '_' -> {
                    val endIndex = text.indexOf('_', i + 1)
                    if (endIndex != -1) {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(text.substring(i + 1, endIndex))
                        pop()
                        i = endIndex + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }

                // Regular character
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
