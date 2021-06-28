package shell;

import lombok.experimental.UtilityClass;
import shell.tokenization.TokenBlock;

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
						if (execv("/usr/bin/ls", new String[]{"ls"}) < 0) {
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
			}
		}
		return ExitCodes.INVALID_ARGUMENT;
	}

	private void recurse(TokenBlock block) {

		//final int pid = fork();
		//switch (pid) {
		//	case -1:
		//		System.err.println("Unable to fork");
		//		System.exit(ExitCodes.TERMINATED);
		//		break;
		//	case 0:
		//
		//		break;
		//	default:
		//		break;
		//}

		if (block.getReadsFrom() != null) recurse(block.getReadsFrom());
	}
}
