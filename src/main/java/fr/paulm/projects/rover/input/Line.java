package fr.paulm.projects.rover.input;

import fr.paulm.projects.rover.domain.InvalidInputException;

import java.util.ArrayList;
import java.util.List;

// A non-blank line of a text input, trimmed, with its line number in the original content (from 1),
// so that error messages can point to the right line even when blank lines were skipped.
record Line(int number, String text) {

    // The non-blank lines of the content, trimmed.
    static List<Line> nonBlankLines(String content) {
        List<String> rawLines = content.lines().toList();
        List<Line> lines = new ArrayList<>();
        for (int i = 0; i < rawLines.size(); i++) {
            String text = rawLines.get(i).trim();
            if (!text.isEmpty()) {
                lines.add(new Line(i + 1, text));
            }
        }
        return lines;
    }

    InvalidInputException error(String message) {
        return new InvalidInputException("Line " + number + ": " + message);
    }
}
