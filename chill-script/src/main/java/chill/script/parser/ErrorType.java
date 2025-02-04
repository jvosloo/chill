package chill.script.parser;

public enum ErrorType {
    UNEXPECTED_TOKEN("Unexpected Token"), UNTERMINATED_LIST("Expected close bracket for list"),;

    private final String message;

    ErrorType(String string) {
        message = string;
    }

    @Override
    public String toString() {
        return message;
    }
}
