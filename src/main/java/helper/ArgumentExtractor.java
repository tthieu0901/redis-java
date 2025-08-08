package helper;

public class ArgumentExtractor {
    public record Pair(String key, String value) {
    }

    public static Pair extractByKey(String[] args, String key) {
        for (int i = 0; i < args.length; i++) {
            var arg = args[i];
            if (arg.equals(key) && i + 1 < args.length) {
                return new Pair(key, args[i + 1]);
            }
        }
        return null;
    }
}
