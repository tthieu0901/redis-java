package server.cron;

import java.io.IOException;

public interface ICron {
    void run() throws IOException;
}
