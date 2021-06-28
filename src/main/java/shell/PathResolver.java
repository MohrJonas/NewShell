package shell;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PathResolver {

	public String resolveToAbsolutePath(String toResolve) {
		return System.getProperty("user.dir") + "/" + toResolve;
	}

}
