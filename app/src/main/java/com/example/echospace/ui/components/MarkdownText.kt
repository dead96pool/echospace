// ui/components/MarkdownText.kt
package com.example.echospace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    val elements = remember(text) { MarkdownParser.parseMarkdown(text) }

    // Define theme-aware quote color once in the composable
    val quoteColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)

    SelectionContainer {
        Column(
            modifier = modifier.fillMaxWidth()
        ) {
            elements.forEach { element ->
                when (element.type) {
                    MarkdownType.HEADING1 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = element.content,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 26.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.tertiary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    MarkdownType.HEADING2 -> {
                        Text(
                            text = element.content,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 20.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    MarkdownType.BULLET_POINT -> {
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "• ",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = MarkdownParser.parseInlineFormatting(element.content, quoteColor),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    MarkdownType.BLOCKQUOTE -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .padding(vertical = 2.dp)
                        ) {
                            // Left accent bar
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .background(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            )

                            // Blockquote content box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                        shape = RectangleShape
                                    )
                                    .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
                            ) {
                                Text(
                                    text = MarkdownParser.parseInlineFormatting(element.content, quoteColor),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = quoteColor
                                    )
                                )
                            }
                        }
                    }
                    MarkdownType.BLANK_LINE -> {
                        // Preserve blank line spacing
                        Spacer(modifier = Modifier.height(MaterialTheme.typography.bodyLarge.lineHeight.value.dp))
                    }
                    MarkdownType.PARAGRAPH -> {
                        Text(
                            text = MarkdownParser.parseInlineFormatting(element.content, quoteColor),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
