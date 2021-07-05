package shell;

import lombok.experimental.UtilityClass;
import shell.tokenization.TokenBlock;
import shell.tokenization.Tokenizer;

import java.util.List;
import java.util.Scanner;

import static cTools.KernelWrapper.*;

@UtilityClass
public class InputManager {

	private final Scanner scanner = new Scanner(System.in);

	static {
		//Runtime.getRuntime().addShutdownHook(new Thread(scanner::close));
	}

	public List<TokenBlock> readCommand() {
		final String line = scanner.nextLine();
		if (line.equals("exit")) {
			System.out.print("Bye Bye");
			exit(ExitCodes.OKAY);
		}
		return Tokenizer.tokenize(line);
	}
}
