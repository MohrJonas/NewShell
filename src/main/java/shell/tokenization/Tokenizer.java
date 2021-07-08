package shell.tokenization;

import com.google.common.collect.ImmutableMap;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Log
@UtilityClass
public class Tokenizer {

    private final ImmutableMap<TOKEN_TYPE, String> tokenMap = ImmutableMap.of(
            TOKEN_TYPE.GREATER, "^>$",
            TOKEN_TYPE.READ_STDIN, "^-$",
            TOKEN_TYPE.LESS, "^<$",
            TOKEN_TYPE.TEXT, ".+"
    );


    public List<TokenBlock> tokenize(String s) {
        final List<Token> tokenList = new ArrayList<>();
        for (final String part : s.split(" ")) {
            final TOKEN_TYPE type = findToken(part);
            tokenList.add(new Token(type, part));
        }
        return setIO(blockify(tokenList));
    }

    private List<TokenBlock> blockify(List<Token> tokens) {
        final List<TokenBlock> blocks = new ArrayList<>();
        final List<Token> blockTokens = new ArrayList<>();
        boolean readStdin = false;
        for (Token token : tokens) {
            switch (token.getType()) {
                case GREATER:
                case LESS:
                    final TokenBlock block = new TokenBlock(new ArrayList<>(blockTokens));
                    block.setReadsStdin(readStdin);
                    blocks.add(block);
                    blockTokens.clear();
                    readStdin = false;
                    blocks.add(new TokenBlock(List.of(token)));
                    break;
                case READ_STDIN:
                    readStdin = true;
                case FILE:
                case PROGRAM:
                case TEXT:
                case ERROR:
                case OOB:
                    if (blockTokens.size() == 0 && blocks.size() == 0)
                        blockTokens.add(new Token(TOKEN_TYPE.PROGRAM, token.getCmd()));
                    else
                        blockTokens.add(token);
            }
        }
        if (!blockTokens.isEmpty()) {
            final TokenBlock block = new TokenBlock(blockTokens);
            block.setReadsStdin(readStdin);
            blocks.add(block);
        }
        return blocks;
    }

    @SuppressWarnings("SuspiciousListRemoveInLoop")
    private List<TokenBlock> setIO(List<TokenBlock> blocks) {
        for (int i = 1; i < blocks.size(); i++) {
            if (blocks.get(i).getTokens().size() != 1) continue;
            switch (blocks.get(i).getTokens().get(0).getType()) {
                case GREATER:
                    blocks.get(0).setWritesTo(blocks.get(i + 1));
                    blocks.get(i + 1).setReadsFrom(blocks.get(0));
                    blocks.remove(i);
                    break;
                case LESS:
                    blocks.get(0).setReadsFrom(blocks.get(i + 1));
                    blocks.get(i + 1).setWritesTo(blocks.get(0));
                    blocks.remove(i);
                    break;
            }

        }
        return blocks;
    }

    private TOKEN_TYPE findToken(String s) {
        return tokenMap.entrySet()
                .stream()
                .filter(entry -> s.matches(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(TOKEN_TYPE.ERROR);
    }
}
