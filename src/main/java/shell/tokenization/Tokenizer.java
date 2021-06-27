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

	private final Map<TOKEN_TYPE, String> tokenMap = ImmutableMap.of(
			TOKEN_TYPE.GREATER, "<",
			TOKEN_TYPE.READ_STDIN, "-",
			TOKEN_TYPE.LESS, ">",
			TOKEN_TYPE.TEXT, ".+"
	);


	public List<TokenBlock> tokenize(String s) {
		final List<Token> tokenList = new ArrayList<>();
		for (final String part : s.split(" ")) {
			final TOKEN_TYPE type = findToken(part);
			tokenList.add(new Token(type, part));
		}
		log.info(tokenList.toString());
		final var blockified = blockify(tokenList);
		log.info(blockified.toString());
		final var io = setIO(blockified);
		log.info(io.toString());
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
		for (int i = 0; i < blocks.size(); i++) {
			if (blocks.get(i).getTokens().size() == 1) {
				if (blocks.get(i).getTokens().get(0).getType() == TOKEN_TYPE.GREATER) {
					blocks.get(i - 1).setWritesTo(blocks.get(i + 1));
					blocks.get(i + 1).setReadsFrom(blocks.get(i - 1));
					blocks.remove(i);
				} else if (blocks.get(i).getTokens().get(0).getType() == TOKEN_TYPE.LESS) {
					blocks.get(i + 1).setWritesTo(blocks.get(i - 1));
					blocks.get(i - 1).setReadsFrom(blocks.get(i + 1));
					blocks.remove(i);
				}
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
