package shell;

import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import my.utils.Pair;
import shell.tokenization.TokenBlock;

import java.util.List;
import java.util.stream.Collectors;

import static cTools.KernelWrapper.*;

//TODO cleanup code, change error messages, use logger, add newline to input, change return codes to constants
@Log
@UtilityClass
public class Executor {

    //[✅] cat abc          # Inhalt der Datei abc wird auf dem Terminal ausgegeben
    //[✅] cat < abc        # Inhalt der Datei abc wird auf dem Terminal ausgegeben
    //[✅] cat - < abc      # Inhalt der Datei abc wird auf dem Terminal ausgegeben
    //[✅] cat abc - abc < abc   # abc wird drei mal ausgegeben
    //[✅] cat abc > xyz    # wirkt wie "cp abc xyz"
    //[❎] cat < abc > xxx    # wirkt wie "cp abc xxx"
    //[❎] cat > yyy < abc    # wirkt wie "cp abc yyy"
    //[❎] cat - > zzz < abc  # wirkt wie "cp abc zzz"
    public int execute(List<TokenBlock> blocks) {
        final List<Pair<TokenBlock, Boolean>> consumptionList = blocks.stream().map(block -> new Pair<>(block, false)).collect(Collectors.toUnmodifiableList());
        for (final Pair<TokenBlock, Boolean> pair : consumptionList) {
            final String cmd = PathResolver.resolveToPath(pair.a.asCmd());
            final String[] args = pair.a.asArgs();
            final boolean reads = pair.a.getReadsFrom() != null;
            final boolean writes = pair.a.getWritesTo() != null;
            createProcess(cmd, args,
                    reads ? pair.a.getReadsFrom().asCmd() : null,
                    writes ? pair.a.getWritesTo().asCmd() : null,
                    new int[]{STDIN_FILENO, STDOUT_FILENO},
                    STDIN_FILENO, STDOUT_FILENO);
            pair.b = true;
        }
        return ExitCodes.OKAY;
    }

    //private void recurse(TokenBlock block) {
    //    final String cmd = PathResolver.resolveToPath(block.asCmd());
    //    final String[] args = block.asArgs();
    //    final boolean reads = block.getReadsFrom() != null;
    //    final boolean writes = block.getWritesTo() != null;
    //    createProcess(cmd, args, reads ? block.getReadsFrom().asCmd() : null, writes ? block.getWritesTo().asCmd() : null, new int[]{STDIN_FILENO, STDOUT_FILENO}, STDIN_FILENO, STDOUT_FILENO);
    //    if (writes) {
    //        final TokenBlock writeBlock = block.getWritesTo();
    //        if (writeBlock.getWritesTo() != null && writeBlock.getReadsFrom() != null) recurse(writeBlock);
    //    }
    //    if (reads) {
    //        final TokenBlock readBlock = block.getReadsFrom();
    //        if (readBlock.getWritesTo() != null && readBlock.getReadsFrom() != null) recurse(readBlock);
    //    }
    //}

    private int redirectOutput(String fOut, String fIn) {
        if (fOut != null) {
            close(STDOUT_FILENO);
            final int fd = open(fOut, O_WRONLY | O_CREAT);
            if (fd < 0)
                return ExitCodes.ERROR;
        } else if (fIn != null) {
            close(STDIN_FILENO);
            final int fd = open(fIn, O_RDONLY);
            if (fd < 0)
                return ExitCodes.ERROR;
        }
        return ExitCodes.OKAY;
    }

    /**
     * @param cmd       the command to run as absolute path f.e. /usr/bin/ls
     * @param args      the args for the cmd the first arg is the file name f.e. ls
     * @param fIn       The file to read from
     * @param fOut      The file to write to
     * @param pipefd    ?
     * @param newStdin  ?
     * @param newStdout ?
     */
    private int createProcess(String cmd, String[] args, String fIn, String fOut, int[] pipefd, int newStdin, int newStdout) {
        final int[] status = new int[1];
        final int pid = fork();
        switch (pid) {
            case -1:
                log.warning("Error forking");
                exit(ExitCodes.TERMINATED);
                break;
            case 0:
                if (newStdin != STDIN_FILENO && newStdout != STDOUT_FILENO) {
                    close(newStdout);
                    dup2(newStdin, STDIN_FILENO);
                    close(newStdin);
                    System.err.println("Set Pipe for Read");
                }
                if (fOut != null || fIn != null) {
                    if (redirectOutput(fOut, fIn) != 0) {
                        System.err.println("Error redirecting output");
                        return 1;
                    }
                }
                if (pipefd[0] != STDIN_FILENO && pipefd[1] != STDOUT_FILENO) {
                    close(pipefd[0]);
                    dup2(pipefd[1], STDOUT_FILENO);
                    close(pipefd[1]);
                    System.err.println("Set Pipe for Write");
                }
                if (execv(cmd, args) < 0) {
                    System.err.println("Error: execv");
                    exit(1);
                }
            default:
                if (newStdin != STDIN_FILENO && newStdout != STDOUT_FILENO) {
                    close(newStdin);
                    close(newStdout);
                }

                if (waitpid(pid, status, 0) < 0) {
                    System.err.println("Error: waiting for child");
                    exit(1);
                }
                if (status[0] != 0) {
                    System.err.println("Status: Child returned error code");
                    return 1;
                }
                return ExitCodes.OKAY;
        }
        return ExitCodes.TERMINATED;
    }
}
