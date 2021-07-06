package shell;

import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import shell.tokenization.TokenBlock;

import java.util.List;

import static cTools.KernelWrapper.*;

//TODO cleanup code, change error messages, use logger, add newline to input, change return codes to constants
@Log
@UtilityClass
public class Executor {

    //[✅] [✅] cat test          # Inhalt der Datei test wird auf dem Terminal ausgegeben
    //[✅] [✅] cat < test        # Inhalt der Datei test wird auf dem Terminal ausgegeben
    //[✅] [✅] cat - < test      # Inhalt der Datei test wird auf dem Terminal ausgegeben
    //[✅] [✅] cat test - test < test   # test wird drei mal ausgegeben
    //[✅] [✅] cat test > xyz    # wirkt wie "cp test xyz"
    //[❎] [❎] cat < test > xxx    # wirkt wie "cp test xxx"
    //[❎] [❎] cat > yyy < test    # wirkt wie "cp test yyy"
    //[❎] [❎] cat - > zzz < test  # wirkt wie "cp test zzz"
    public int execute(List<TokenBlock> blocks) {
        log.info("Starting execution blocks are: " + blocks);
        final TokenBlock masterBlock = blocks.get(0);
        log.info("MasterBlock is " + masterBlock);
        if (masterBlock.getReadsFrom() == null && masterBlock.getWritesTo() == null) {
            log.info("MasterBlock neither reads nor writes. Executing it on its own");
            return createProcess(PathResolver.resolveToPath(masterBlock.asCmd()), masterBlock.asArgs(), null, null, new int[2], STDIN_FILENO, STDOUT_FILENO);
        } else if (masterBlock.getReadsFrom() != null && masterBlock.getWritesTo() != null) {
            final TokenBlock readSlaveBlock = masterBlock.getReadsFrom();
            final TokenBlock writeSlaveBlock = masterBlock.getWritesTo();
            log.info("MasterBlock reads from " + readSlaveBlock);
            log.info("MasterBlock writes to " + writeSlaveBlock);
            final int readFd = open(readSlaveBlock.asCmd(), O_RDONLY);
            final int writeFd = open(writeSlaveBlock.asCmd(), O_CREAT | O_WRONLY);
            log.info("Creating Process between " + masterBlock + ", " + readSlaveBlock + " and " + writeSlaveBlock);
            createProcess(PathResolver.resolveToPath(masterBlock.asCmd()), masterBlock.asArgs(), readSlaveBlock.asCmd(), writeSlaveBlock.asCmd(), new int[]{STDIN_FILENO, STDOUT_FILENO}, readFd, writeFd);
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
                createProcess(PathResolver.resolveToPath(masterBlock.asCmd()), masterBlock.asArgs(), slaveBlock.asCmd(), null, new int[]{STDIN_FILENO, STDOUT_FILENO}, STDIN_FILENO, fd);
                log.info("Breaking connection between " + masterBlock + " and " + slaveBlock);
                breakConnection(masterBlock, slaveBlock);
                close(fd);
            }
            if (masterBlock.getWritesTo() != null) {
                final TokenBlock slaveBlock = masterBlock.getWritesTo();
                log.info("MasterBlock writes to " + slaveBlock);
                log.info("Creating Process between " + masterBlock + " and " + slaveBlock);
                final int fd = open(slaveBlock.asCmd(), O_CREAT | O_WRONLY);
                createProcess(PathResolver.resolveToPath(masterBlock.asCmd()), masterBlock.asArgs(), null, slaveBlock.asCmd(), new int[]{STDIN_FILENO, STDOUT_FILENO}, fd, STDOUT_FILENO);
                log.info("Breaking connection between " + masterBlock + " and " + slaveBlock);
                breakConnection(masterBlock, slaveBlock);
                close(fd);
            }
        }
        //for (final TokenBlock block : blocks) {
        //    if (block.equals(masterBlock)) {
        //        log.info("Block is MasterBlock. Skipping.");
        //        continue;
        //    }
        //    if (block.getReadsFrom().size() == 0 && block.getWritesTo().size() == 0) {
        //        log.info("Block neither writes nor reads nor is MasterBlock. Skipping.");
        //        continue;
        //    }
        //    if (block.getReadsFrom().size() > 0) {
        //        log.info("Block " + block + " reads from " + block.getReadsFrom().size() + " source");
        //        block.getReadsFrom().forEach(b -> {
        //            log.info("Creating Process between " + block + " and " + b);
        //            createProcess(block.asCmd(), block.asArgs(), b.asCmd(), null, new int[2], STDIN_FILENO, STDOUT_FILENO);
        //            log.info("Breaking connection between " + block + " and " + b);
        //            breakConnection(masterBlock, b);
        //        });
        //    }
        //    if (block.getWritesTo().size() > 0) {
        //        log.info("Block " + block + " writes to " + block.getWritesTo().size() + " source");
        //        block.getWritesTo().forEach(b -> {
        //            log.info("Creating Process between " + block + " and " + b);
        //            createProcess(block.asCmd(), block.asArgs(), null, b.asCmd(), new int[2], STDIN_FILENO, STDOUT_FILENO);
        //            log.info("Breaking connection between " + block + " and " + b);
        //            breakConnection(masterBlock, b);
        //        });
        //    }
        //}
        log.info("Done executing blocks");
        return ExitCodes.OKAY;
    }

    private void breakConnection(TokenBlock reader, TokenBlock writer) {
        reader.setReadsFrom(null);
        writer.setWritesTo(null);
    }

    /*
     * open returns file-descriptor pointing to file
     */
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
     * @param fIn       The file to read from, relative
     * @param fOut      The file to write to, relative
     * @param pipefd    ?
     * @param newStdin  ?
     * @param newStdout ?
     */
    public int createProcess(String cmd, String[] args, String fIn, String fOut, int[] pipefd, int newStdin, int newStdout) {
        final int[] status = new int[1];
        final int pid = fork();
        switch (pid) {
            case -1:
                log.severe("Error forking");
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
                    //Close read end
                    close(pipefd[0]);
                    //Make STDOUT_FILENO point to pipefd[1]
                    dup2(pipefd[1], STDOUT_FILENO);
                    //Close write end
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
        System.out.println();
        return ExitCodes.TERMINATED;
    }
}
