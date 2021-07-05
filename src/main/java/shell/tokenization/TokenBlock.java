package shell.tokenization;

import lombok.Data;
import my.utils.Utils;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Data
public class TokenBlock {

    private final List<Token> tokens;
    private boolean readsStdin = false;
    private @Nullable TokenBlock readsFrom = null;
    private @Nullable TokenBlock writesTo = null;

    public String[] asArgs() {
        return Utils.castArray(tokens.stream().map(Token::getCmd).toArray(), String.class);
    }

    public String asCmd() {
        return tokens.get(0).getCmd();
    }

    @Override
    public String toString() {
        return "TokenBlock{" +
                "tokens=" + tokens +
                ", readsStdin=" + readsStdin +
                ", readsFrom=" + (readsFrom == null ? "no" : "yes") +
                ", writesTo=" + (writesTo == null ? "no" : "yes") +
                '}';
    }
}
