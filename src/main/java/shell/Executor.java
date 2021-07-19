package shell;

import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import shell.tokenization.TokenBlock;

import java.util.List;

import static cTools.KernelWrapper.*;

@Log
@UtilityClass
public class Executor {

    public int execute(List<TokenBlock> blocks) {
        log.info("Starting execution blocks are: " + blocks);
        final TokenBlock masterBlock = blocks.get(0);
        log.info("MasterBlock is " + masterBlock);
        if (masterBlock.getReadsFrom() == null && masterBlock.getWritesTo() == null) {
            log.info("MasterBlock neither reads nor writes. Executing it on its own");
            return createProcess(PathResolver.resolveToPath(masterBlock.asCmd()), masterBlock.asArgs(), null, null, STDIN_FILENO, STDOUT_FILENO);
        } else if (masterBlock.getReadsFrom() != null && masterBlock.getWritesTo() != null) {
            final TokenBlock readSlaveBlock = masterBlock.getReadsFrom();
            final TokenBlock writeSlaveBlock = masterBlock.getWritesTo();
            log.info("MasterBlock reads from " + readSlaveBlock);
            log.info("MasterBlock writes to " + writeSlaveBlock);
            final int readFd = open(readSlaveBlock.asCmd(), O_RDONLY);
            final int writeFd = open(writeSlaveBlock.asCmd(), O_CREAT | O_WRONLY);
            log.info("Creating Process between " + masterBlock + ", " + readSlaveBlock + " and " + writeSlaveBlock);
            createProcess(PathResolver.resolveToPath(masterBlock.asCmd()), masterBlock.asArgs(), readSlaveBlock.asCmd(), writeSlaveBlock.asCmd(), readFd, writeFd);
            log.info("Breaking connection between " + masterBlock + ", " + readSlaveBlock + " and " + writeSlaveBlock);
            breakConnection(masterBlock, readSlaveBlock);
            breakConnection(masterBlock, writeSlaveBlock);
            close(readFd);
            close(writeFd);
        } else {
            if (masterBlock.getReadsFrom() != null) {
                final TokenBlock slaveBlock = masterBlock.getReadsFrom();
                log.info("MasterBlock reads from " + slaveBlock);
                log.info("Creating Process between " + masterBlock + " and " + slaveBlock);
                final int fd = open(slaveBlock.asCmd(), O_RDONLY);
                createProcess(PathResolver.resolveToPath(masterBlock.asCmd()), masterBlock.asArgs(), slaveBlock.asCmd(), null, STDIN_FILENO, fd);
                log.info("Breaking connection between " + masterBlock + " and " + slaveBlock);
                breakConnection(masterBlock, slaveBlock);
                close(fd);
            }
            if (masterBlock.getWritesTo() != null) {
                final TokenBlock slaveBlock = masterBlock.getWritesTo();
                log.info("MasterBlock writes to " + slaveBlock);
                log.info("Creating Process between " + masterBlock + " and " + slaveBlock);
                final int fd = open(slaveBlock.asCmd(), O_CREAT | O_WRONLY);
                createProcess(PathResolver.resolveToPath(masterBlock.asCmd()), masterBlock.asArgs(), null, slaveBlock.asCmd(), fd, STDOUT_FILENO);
                log.info("Breaking connection between " + masterBlock + " and " + slaveBlock);
                breakConnection(masterBlock, slaveBlock);
                close(fd);
            }
        }
        log.info("Done executing blocks");
        return ExitCodes.OKAY;
    }

    private void breakConnection(TokenBlock reader, TokenBlock writer) {
        reader.setReadsFrom(null);
        writer.setWritesTo(null);
    }

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

    public int createProcess(String cmd, String[] args, String fIn, String fOut, int newStdin, int newStdout) {
        final int[] status = new int[1];
        final int pid = fork();
        switch (pid) {
            case -1:
                log.severe("Error forking");
                return ExitCodes.ERROR;
            case 0:
                if (newStdin != STDIN_FILENO && newStdout != STDOUT_FILENO) {
                    close(newStdout);
                    dup2(newStdin, STDIN_FILENO);
                    close(newStdin);
                }
                if (fOut != null || fIn != null) {
                    if (redirectOutput(fOut, fIn) != 0) {
                        log.severe("Error redirecting output");
                        exit(ExitCodes.ERROR);
                    }
                }
                if (execv(cmd, args) < 0) {
                    log.severe("Error running program");
                    exit(ExitCodes.TERMINATED);
                }
            default:
                if (newStdin != STDIN_FILENO && newStdout != STDOUT_FILENO) {
                    close(newStdin);
                    close(newStdout);
                }
                if (waitpid(pid, status, 0) < 0) {
                    log.severe("Error while waiting for child");
                    return ExitCodes.ERROR;
                }
                if (status[0] != 0) {
                    log.severe("Status: Child returned error code");
                    return status[0];
                }
                return ExitCodes.OKAY;
        }
    }
}
