package protocol;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

public class Protocol {

    @Getter
    @RequiredArgsConstructor
    public enum DataType {
        SIMPLE_STRING('+'),
        ERROR('-'),
        INTEGER(':'),
        BULK_STRING( '$'),
        ARRAY('*'),
        ;

        private final char prefix;

        public static DataType findDataTypeByPrefix(char prefix) {
            return Arrays.stream(DataType.values()).filter(dt -> dt.getPrefix() == prefix).findFirst().orElse(null);
        }
    }

    public enum Command {
        PING,
        SET,
        GET,
        ECHO,
        ;

        public static Command findCommand(String command) {
            return Arrays.stream(Command.values()).filter(cmd -> cmd.name().equalsIgnoreCase(command)).findFirst().orElse(null);
        }
    }
}
