package shell;

import lombok.experimental.UtilityClass;
import shell.tokenization.TokenBlock;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static shell.cTools.KernelWrapper.*;

@UtilityClass
public class Executor {

	public int execute(List<TokenBlock> blocks) {
		for (final TokenBlock block : blocks) {
			if (blocks.size() == 1) {
				final int pid = fork();
				final int[] status = new int[1];
				switch (pid) {
					case -1:
						System.err.println("Unable to fork");
						exit(ExitCodes.TERMINATED);
						break;
					case 0:
						if (execv(PathResolver.resolveToPath(block.getTokens().get(0).getCmd()), block.asArgs()) < 0) {
							System.err.println("Error in execv");
							exit(ExitCodes.TERMINATED);
						}
						exit(ExitCodes.OKAY);
						break;
					default:
						if (waitpid(pid, status, 0) < 0) {
							System.err.println("Error in waitpid");
						}
						return status[0];
				}
			} else if (block.getReadsFrom() != null) {
				System.out.println(block.getReadsFrom().toString());
				final int[] pipefd = new int[2];
				final int[] status = new int[1];
				if (pipe(pipefd) < 0) {
					System.err.println("Error in pipe");
				}
				final int pid = fork();
				switch (pid) {
					case -1:
						System.err.println("Unable to fork");
						System.exit(ExitCodes.TERMINATED);
						break;
					case 0:
						close(pipefd[0]);
						dup2(pipefd[1], STDOUT_FILENO);
						execv(PathResolver.resolveToPath(block.getReadsFrom().getTokens().get(0).getCmd()), block.getReadsFrom().asArgs());
						close(pipefd[1]);
						exit(ExitCodes.OKAY);
						break;
					default:
						close(pipefd[1]);
						final byte[] buffer = new byte[1];
						final List<Character> lineChars = new ArrayList<>();
						while (read(pipefd[0], buffer, 1) > 0) {
							final char c = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(buffer)).get();
							if (((int) c) != 10)
								lineChars.add(c);
							else {
								final StringBuilder builder = new StringBuilder();
								lineChars.forEach(builder::append);
								lineChars.clear();
								System.out.println(builder);
							}
							//System.out.printf("%d -> %c%n", ((int) c), c);
						}
						if (waitpid(pid, status, 0) < 0) {
							System.err.println("Error in waitpid");
						}
						close(pipefd[0]);
						return status[0];
				}
			}
		}
		return ExitCodes.INVALID_ARGUMENT;
	}
}
