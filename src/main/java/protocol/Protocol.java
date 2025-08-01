package protocol;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
    }
}
