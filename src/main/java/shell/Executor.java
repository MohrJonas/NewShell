package shell;

import lombok.experimental.UtilityClass;
import shell.tokenization.TokenBlock;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static cTools.KernelWrapper.*;

@UtilityClass
public class Executor {

	public int execute(List<TokenBlock> blocks) {
		for (final TokenBlock block : blocks) {
			if (block.getReadsFrom() == null && block.getWritesTo() == null) {
				final int pid = fork();
				final int[] status = new int[1];
				switch (pid) {
					case -1:
						System.err.println("Unable to fork");
						System.exit(ExitCodes.TERMINATED);
						break;
					case 0:
						if (execv(PathResolver.resolveToPath(block.getTokens().get(0).getCmd()), block.asArgs()) < 0) {
							System.err.println("Error in execv");
							exit(ExitCodes.TERMINATED);
						}
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
						//write(pipefd[1], new byte[]{0, 1, 2, 3}, 4);
						close(pipefd[1]);
						exit(ExitCodes.OKAY);
						break;
					default:
						close(pipefd[1]);
						final byte[] buffer = new byte[1];
						while (read(pipefd[0], buffer, 1) > 0) {
							final char c = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(buffer)).get();
							System.out.printf("%d -> %c%n", ((int) c), c);
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
