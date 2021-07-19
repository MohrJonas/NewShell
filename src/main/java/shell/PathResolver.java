package shell;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@UtilityClass
public class PathResolver {

    @SneakyThrows
    public String resolveToPath(String toResolve) {
        if (toResolve.startsWith("./")) {
            return System.getProperty("user.dir") + "/" + toResolve.substring(2);
        } else {
            final Optional<Path> opt = Files.list(Path.of(System.getProperty("user.dir"))).filter(path -> path.getFileName().toString().equals(toResolve)).findFirst();
            if (opt.isPresent()) return opt.get().toAbsolutePath().toString();
            final String[] pathFolders = System.getenv("PATH").split(":");
            for (final String pathFolder : pathFolders) {
                if (!Files.exists(Path.of(pathFolder))) continue;
                final Optional<Path> optional = Files.list(Path.of(pathFolder)).filter(path -> Files.isReadable(path) && path.getFileName().toString().equals(toResolve)).findFirst();
                if (optional.isPresent()) return optional.get().toAbsolutePath().toString();
            }
        }
        return String.valueOf(ExitCodes.COMMAND_NOT_FOUND);
    }
}
