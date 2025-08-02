package protocol;

import java.util.Arrays;

public class Protocol {

    public enum DataType {
        SIMPLE_STRING('+'),
        ERROR('-'),
        INTEGER(':'),
        BULK_STRING('$'),
        ARRAY('*'),
        ;

        private final char prefix;

        DataType(char c) {
            prefix = c;
        }

        public static DataType findDataTypeByPrefix(char prefix) {
            return Arrays.stream(DataType.values()).filter(dt -> dt.getPrefix() == prefix).findFirst().orElse(null);
        }

        public char getPrefix() {
            return this.prefix;
        }
    }

    public enum Command {
        PING,
        SET,
        GET,
        ECHO,
        RPUSH,
        LRANGE,
        LPUSH,
        LLEN,
        LPOP,
        BLPOP;

        public static Command findCommand(String command) {
            return Arrays.stream(Command.values()).filter(cmd -> cmd.name().equalsIgnoreCase(command)).findFirst().orElse(null);
        }
    }
}
