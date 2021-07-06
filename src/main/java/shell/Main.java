package shell;

import com.google.common.base.Stopwatch;
import shell.tokenization.TokenBlock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private static final Stopwatch stopwatch = Stopwatch.createUnstarted();
    private static int lastExitCode = 0;
    private static long lastExecTime = 0L;

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {
        Logger.getLogger(Executor.class.getName()).setLevel(Level.WARNING);
        while (true) {
            System.out.print(getHeader());
            final List<TokenBlock> blocks = InputManager.readCommand();
            stopwatch.start();
            lastExitCode = Executor.execute(blocks);
            stopwatch.stop();
            lastExecTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
            stopwatch.reset();
        }
    }

    private static String getHeader() {
        final LocalDateTime now = LocalDateTime.now();
        return String.format("%s%s%s@%s%d:%d:%d%s=%s%d%s=%s%d%sms=> ",
                Colors.ANSI_BLUE,
                System.getProperty("user.name"),
                Colors.ANSI_RESET,
                Colors.ANSI_CYAN,
                now.getHour(),
                now.getMinute(),
                now.getSecond(),
                Colors.ANSI_RESET,
                Colors.ANSI_RED,
                lastExitCode,
                Colors.ANSI_RESET,
                Colors.ANSI_GREEN,
                lastExecTime,
                Colors.ANSI_RESET
        );
    }
}
