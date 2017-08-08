package wbs.framework.exception;

import com.google.common.base.Optional;

import wbs.framework.entity.record.Record;
import wbs.framework.logging.TaskLogger;

public
interface GenericExceptionLogger <Resolution> {

	Record <?> logSimple (
			TaskLogger parentTaskLogger,
			String typeCode,
			String source,
			String summary,
			String dump,
			Optional <Long> userId,
			Resolution resolution);

	Record <?> logThrowable (
			TaskLogger parentTaskLogger,
			String typeCode,
			String source,
			Throwable throwable,
			Optional <Long> userId,
			Resolution resolution);

	Record <?> logThrowableWithSummary (
			TaskLogger parentTaskLogger,
			String typeCode,
			String source,
			String summary,
			Throwable throwable,
			Optional <Long> userId,
			Resolution resolution);

}
