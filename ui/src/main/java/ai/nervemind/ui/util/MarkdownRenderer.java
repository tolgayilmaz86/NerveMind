/*
 * Copyright (c) 2026 NerveMind
 * Licensed under the MIT License
 */
package ai.nervemind.ui.util;

import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Simple Markdown renderer that converts Markdown text to JavaFX nodes.
 * Supports: headers (##), **bold**, `code`, bullet lists (- or *), and tables.
 */
public final class MarkdownRenderer {

    private MarkdownRenderer() {
        // Utility class
    }

    /**
     * Parse Markdown content and convert to JavaFX VBox.
     *
     * @param content the Markdown text to render
     * @return a VBox containing the rendered content
     */
    public static VBox render(String content) {
        VBox container = new VBox(8);
        container.setPadding(new Insets(10, 0, 10, 0));

        if (content == null || content.isBlank()) {
            return container;
        }

        String[] lines = content.split("\n");
        int[] index = { 0 };
        while (index[0] < lines.length) {
            String line = lines[index[0]].trim();

            if (line.isEmpty()) {
                index[0]++;
                continue;
            }

            processLine(container, lines, index, line);
        }

        return container;
    }

    private static void processLine(VBox container, String[] lines, int[] index, String line) {
        if (tryProcessHeader(container, line)) {
            index[0]++;
        } else if (tryProcessTable(container, lines, index, line)) {
            // index already updated in method
        } else if (tryProcessBulletList(container, line)) {
            index[0]++;
        } else if (tryProcessCodeBlock(container, lines, index, line)) {
            // index already updated in method
        } else {
            processRegularParagraph(container, line);
            index[0]++;
        }
    }

    private static boolean tryProcessHeader(VBox container, String line) {
        if (line.startsWith("###")) {
            container.getChildren().add(createHeader(line.substring(3).trim(), 13, FontWeight.BOLD));
            return true;
        }
        if (line.startsWith("##")) {
            container.getChildren().add(createHeader(line.substring(2).trim(), 14, FontWeight.BOLD));
            return true;
        }
        if (line.startsWith("#")) {
            container.getChildren().add(createHeader(line.substring(1).trim(), 16, FontWeight.BOLD));
            return true;
        }
        return false;
    }

    private static boolean tryProcessTable(VBox container, String[] lines, int[] index, String line) {
        if (index[0] + 1 >= lines.length || !lines[index[0] + 1].trim().matches("^\\|[-:\\s|]+\\|$")) {
            return false;
        }
        VBox table = parseTable(lines, index[0]);
        container.getChildren().add(table);
        index[0]++;
        while (index[0] < lines.length && lines[index[0]].trim().startsWith("|")) {
            index[0]++;
        }
        return true;
    }

    private static boolean tryProcessBulletList(VBox container, String line) {
        if (!line.startsWith("- ") && !line.startsWith("* ")) {
            return false;
        }
        HBox bulletLine = new HBox(5);
        bulletLine.setPadding(new Insets(2, 0, 2, 15));
        Text bullet = new Text("• ");
        bullet.setFill(Color.web("#8b949e"));
        TextFlow textFlow = parseLineToTextFlow(line.substring(2));
        bulletLine.getChildren().addAll(bullet, textFlow);
        container.getChildren().add(bulletLine);
        return true;
    }

    private static boolean tryProcessCodeBlock(VBox container, String[] lines, int[] index, String line) {
        if (!line.startsWith("```")) {
            return false;
        }
        StringBuilder codeBlock = new StringBuilder();
        index[0]++; // Skip opening ```
        while (index[0] < lines.length && !lines[index[0]].trim().startsWith("```")) {
            codeBlock.append(lines[index[0]]).append("\n");
            index[0]++;
        }
        index[0]++; // Skip closing ```
        container.getChildren().add(createCodeBlock(codeBlock.toString()));
        return true;
    }

    private static void processRegularParagraph(VBox container, String line) {
        TextFlow textFlow = parseLineToTextFlow(line);
        textFlow.setPadding(new Insets(2, 0, 2, 0));
        textFlow.setLineSpacing(4);
        container.getChildren().add(textFlow);
    }

    private static TextFlow createHeader(String text, int fontSize, FontWeight weight) {
        Text headerText = new Text(text);
        headerText.setFont(Font.font("System", weight, fontSize));
        headerText.setFill(Color.web("#eceff4")); // -nervemind-text-primary
        TextFlow flow = new TextFlow(headerText);
        flow.setPadding(new Insets(8, 0, 4, 0));
        return flow;
    }

    private static VBox createCodeBlock(String code) {
        VBox codeBox = new VBox();
        codeBox.setStyle("""
                -fx-background-color: #3b4252;
                -fx-padding: 10;
                -fx-background-radius: 6;
                """);

        Text codeText = new Text(code.trim());
        codeText.setFont(Font.font("Consolas", 12));
        codeText.setFill(Color.web("#d8dee9")); // -nervemind-text-secondary
        codeBox.getChildren().add(codeText);

        VBox wrapper = new VBox(codeBox);
        wrapper.setPadding(new Insets(5, 0, 5, 0));
        return wrapper;
    }

    private static TextFlow parseLineToTextFlow(String line) {
        TextFlow flow = new TextFlow();
        StringBuilder current = new StringBuilder();
        int i = 0;

        while (i < line.length()) {
            int newIndex = tryParseBold(line, i, current, flow);
            if (newIndex == i) {
                newIndex = tryParseCode(line, i, current, flow);
            }
            if (newIndex == i) {
                current.append(line.charAt(i));
                i++;
            } else {
                i = newIndex;
            }
        }

        if (!current.isEmpty()) {
            Text text = new Text(current.toString());
            text.setFill(Color.web("#d8dee9")); // -nervemind-text-secondary
            flow.getChildren().add(text);
        }

        return flow;
    }

    private static int tryParseBold(String line, int i, StringBuilder current, TextFlow flow) {
        if (i >= line.length() - 1 || line.charAt(i) != '*' || line.charAt(i + 1) != '*') {
            return i;
        }

        int end = line.indexOf("**", i + 2);
        if (end == -1) {
            return i;
        }

        flushCurrentText(current, flow);
        Text boldText = new Text(line.substring(i + 2, end));
        boldText.setFont(Font.font("System", FontWeight.BOLD, 13));
        boldText.setFill(Color.web("#eceff4")); // -nervemind-text-primary
        flow.getChildren().add(boldText);
        return end + 2;
    }

    private static int tryParseCode(String line, int i, StringBuilder current, TextFlow flow) {
        if (line.charAt(i) != '`') {
            return i;
        }

        int end = line.indexOf('`', i + 1);
        if (end == -1) {
            return i;
        }

        flushCurrentText(current, flow);
        Text codeText = new Text(line.substring(i + 1, end));
        codeText.setFont(Font.font("Consolas", 12));
        codeText.setFill(Color.web("#88c0d0")); // Accent color for inline code
        flow.getChildren().add(codeText);
        return end + 1;
    }

    private static void flushCurrentText(StringBuilder current, TextFlow flow) {
        if (!current.isEmpty()) {
            Text text = new Text(current.toString());
            text.setFill(Color.web("#d8dee9")); // -nervemind-text-secondary
            flow.getChildren().add(text);
            current.setLength(0);
        }
    }

    private static VBox parseTable(String[] lines, int startIndex) {
        VBox table = new VBox(0);
        table.setStyle("-fx-background-color: #3b4252; -fx-background-radius: 6;"); // -nervemind-bg-secondary
        table.setPadding(new Insets(5));

        // Parse header
        String headerLine = lines[startIndex].trim();
        String[] headers = parseTableRow(headerLine);

        // Create header row
        HBox headerRow = new HBox(10);
        headerRow.setPadding(new Insets(5, 10, 5, 10));
        headerRow.setStyle("-fx-border-color: #434c5e; -fx-border-width: 0 0 1 0;"); // -nervemind-bg-tertiary
        for (String header : headers) {
            Text headerText = new Text(header.trim());
            headerText.setFont(Font.font("System", FontWeight.BOLD, 12));
            headerText.setFill(Color.web("#eceff4")); // -nervemind-text-primary
            headerRow.getChildren().add(headerText);
        }
        table.getChildren().add(headerRow);

        // Skip separator line
        int i = startIndex + 2;

        // Parse data rows
        while (i < lines.length && lines[i].trim().startsWith("|")) {
            String[] cells = parseTableRow(lines[i].trim());
            HBox dataRow = new HBox(10);
            dataRow.setPadding(new Insets(5, 10, 5, 10));
            for (String cell : cells) {
                Text cellText = new Text(cell.trim());
                cellText.setFill(Color.web("#d8dee9")); // -nervemind-text-secondary
                dataRow.getChildren().add(cellText);
            }
            table.getChildren().add(dataRow);
            i++;
        }

        VBox wrapper = new VBox(table);
        wrapper.setPadding(new Insets(10, 0, 10, 0));
        return wrapper;
    }

    private static String[] parseTableRow(String row) {
        if (row.startsWith("|")) {
            row = row.substring(1);
        }
        if (row.endsWith("|")) {
            row = row.substring(0, row.length() - 1);
        }
        return row.split("\\|");
    }
}
