package shell;

import shell.tokenization.TokenBlock;
import shell.tokenization.Tokenizer;

import java.util.List;
import java.util.Scanner;

public class InputManager {

	private static final Scanner scanner = new Scanner(System.in);

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(scanner::close));
	}

	private InputManager() {
	}

	public static List<TokenBlock> readCommand() {
		return Tokenizer.tokenize(scanner.nextLine());

	}
}
