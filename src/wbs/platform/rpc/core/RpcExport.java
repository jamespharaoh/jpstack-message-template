package wbs.platform.rpc.core;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention (RetentionPolicy.RUNTIME)
public @interface RpcExport {
	public String value ();
}