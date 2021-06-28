package shell;

import com.google.common.base.Stopwatch;
import shell.tokenization.TokenBlock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main {

	private static final Stopwatch stopwatch = Stopwatch.createUnstarted();
	private static int lastExitCode = 0;
	private static long lastExecTime = 0L;

	public static void main(String[] args) {
		while (true) {
			System.out.print(getHeader());
			final List<TokenBlock> blocks = InputManager.readCommand();
			System.out.println(blocks.toString());
			stopwatch.start();
			lastExitCode = Executor.execute(blocks);
			stopwatch.stop();
			lastExecTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
			stopwatch.reset();
		}
	}

	private static String getHeader() {
		final LocalDateTime now = LocalDateTime.now();
		return String.format("%s@%d:%d:%d=%d=%dms=> ", System.getProperty("user.name"), now.getHour(), now.getMinute(), now.getSecond(), lastExitCode, lastExecTime);
	}


}
