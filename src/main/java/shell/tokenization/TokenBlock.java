package shell.tokenization;

import lombok.Data;
import my.utils.Utils;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Data
public class TokenBlock {

	private final List<Token> tokens;
	private boolean readsStdin;
	private @Nullable TokenBlock readsFrom;
	private @Nullable TokenBlock writesTo;

	@Override
	public String toString() {
		return "TokenBlock{" +
		       "tokens=" + tokens +
		       ", readsStdin=" + readsStdin +
		       ", readsFrom=" + (readsFrom == null ? "yes" : "no") +
		       ", writesTo=" + (writesTo == null ? "yes" : "no") +
		       '}';
	}

	public String[] asArgs() {
		return Utils.castArray(tokens.stream().map(Token::getCmd).toArray(), String.class);
	}
}
