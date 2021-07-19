package shell.tokenization;

import lombok.Data;

@Data
public class Token {

    private final TOKEN_TYPE type;
    private final String cmd;

}
