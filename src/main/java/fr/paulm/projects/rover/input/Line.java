package fr.paulm.projects.rover.input;

import fr.paulm.projects.rover.domain.InvalidInputException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// A non-blank line of a text input, trimmed, with its line number in the original content (from 1),
// so that error messages can point to the right line even when blank lines were skipped.
// Also reads the "keyword values" lines of the mission and map formats.
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

    // The first word of the line.
    String keyword() {
        return text.split("\\s+", 2)[0];
    }

    // The rest of the line after its keyword, which must be the given one and be followed by something.
    String value(String key) {
        String[] parts = text.split("\\s+", 2);
        if (!parts[0].equals(key) || parts.length < 2) {
            throw error("Expected: " + key + " followed by a value");
        }
        return parts[1];
    }

    // The numbers after the keyword: exactly count of them.
    double[] numbers(int count) {
        String[] parts = text.split("\\s+");
        if (parts.length != count + 1) {
            throw error("Expected: " + parts[0] + " followed by " + count + " number(s)");
        }
        double[] numbers = new double[count];
        for (int i = 0; i < count; i++) {
            try {
                numbers[i] = Double.parseDouble(parts[i + 1]);
            } catch (NumberFormatException e) {
                throw error("Invalid number: \"" + parts[i + 1] + "\"");
            }
        }
        return numbers;
    }

    double decimal() {
        return numbers(1)[0];
    }

    long integer() {
        String value = value(keyword());
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw error("Invalid integer: \"" + value + "\"");
        }
    }

    // Applies a rule of the domain to a value read on this line, adding the line number to the message of its error.
    <T> T check(Supplier<T> value) {
        try {
            return value.get();
        } catch (InvalidInputException e) {
            throw error(e.getMessage());
        }
    }
}
