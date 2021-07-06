package shell.tokenization;

import lombok.Data;
import my.utils.Utils;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Data
public class TokenBlock {

    private final List<Token> tokens;
    private @Nullable TokenBlock readsFrom;
    private @Nullable TokenBlock writesTo;
    private boolean used = false;
    private boolean readsStdin = false;

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
                ", readsFrom=" + (readsFrom != null) +
                ", writesTo=" + (writesTo != null) +
                '}';
    }
}
