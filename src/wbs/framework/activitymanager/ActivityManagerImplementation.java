package wbs.framework.activitymanager;

import static wbs.framework.utils.etc.Misc.isEmpty;
import static wbs.framework.utils.etc.Misc.isNotNull;
import static wbs.framework.utils.etc.Misc.isNull;
import static wbs.framework.utils.etc.Misc.max;
import static wbs.framework.utils.etc.Misc.notEqual;
import static wbs.framework.utils.etc.StringUtils.stringFormat;
import static wbs.framework.utils.etc.TimeUtils.notShorterThan;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import javax.annotation.PostConstruct;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j;

import org.apache.commons.io.IOUtils;
import org.joda.time.Duration;
import org.joda.time.Instant;

import com.google.common.collect.ImmutableMap;

import wbs.framework.utils.etc.FormatWriter;
import wbs.framework.utils.etc.FormatWriterWriter;

@Log4j
public
class ActivityManagerImplementation
	implements ActivityManager {

	// properties

	@Getter @Setter
	Duration slowTaskDuration =
		Duration.millis (
			1000);

	// state

	String hostname;
	int processId;
	long nextTaskId;

	ThreadLocal<Task> currentTask =
		new ThreadLocal<Task> ();

	Map<Long,Task> activeTasks =
		new LinkedHashMap<Long,Task> ();

	// implementation

	@PostConstruct
	public
	void init () {
		throw new RuntimeException ();
	}

	public
	ActivityManagerImplementation () {

		log.debug (
			stringFormat (
				"Initialising"));

		nextTaskId =
			new Random ().nextLong ();

		log.debug (
			stringFormat (
				"Next task id is %s",
				nextTaskId));

		hostname =
			getHostname ();

		log.debug (
			stringFormat (
				"Host name is %s",
				hostname));

		processId =
			getProcessId ();

		log.debug (
			stringFormat (
				"Process ID is %s",
				processId));

	}

	@SneakyThrows (IOException.class)
	int getProcessId () {

		return Integer.parseInt (
			new File ("/proc/self")
				.getCanonicalFile ()
				.getName ());

	}

	@SneakyThrows (IOException.class)
	String getHostname () {

		Process process =
			Runtime.getRuntime ().exec ("hostname");

		String processOutput =
			IOUtils.toString (
				process.getInputStream ());

		String hostname =
			processOutput.trim ();

		return hostname;

	}

	@Override
	public synchronized
	ActiveTask start (
			@NonNull String taskType,
			@NonNull String summary,
			@NonNull Object owner) {

		return start (
			taskType,
			summary,
			owner,
			ImmutableMap.<String,String>of ());

	}

	@Override
	public synchronized
	ActiveTask start (
			@NonNull String taskType,
			@NonNull String summary,
			@NonNull Object owner,
			@NonNull Map<String,String> parameters) {

		log.debug (
			stringFormat (
				"Begin %s task: %s",
				taskType,
				summary));

		Task task =
			new Task ()

			.taskId (
				nextTaskId ++)

			.parent (
				currentTask.get ())

			.owner (
				owner)

			.taskType (
				taskType)

			.summary (
				summary)

			.hostname (
				hostname)

			.processId (
				processId)

			.threadName (
				Thread.currentThread ().getName ())

			.startTime (
				Instant.now ());

		task.parameters ().putAll (
			parameters);

		ActiveTask activeTask =
			new ActiveTaskImplementation (
				task);

		activeTasks.put (
			task.taskId (),
			task);

		if (
			isNotNull (
				task.parent ())
		) {

			task.parent ().children ().add (
				task);

		}

		currentTask.set (
			task);

		return activeTask;

	}

	private synchronized
	void postProcessTask (
			@NonNull Task task) {

		task.endTime (
			Instant.now ());

		if (
			notEqual (
				task,
				currentTask.get ())
		) {
			throw new RuntimeException ();
		}

		Duration taskDuration =
			new Duration (
				task.startTime (),
				task.endTime ());

		log.debug (
			stringFormat (
				"End %s task %s after %s: %s",
				task.taskType (),
				task.summary (),
				taskDuration.toString (),
				task.state ().toString ()));

		if (

			isNull (
				task.parent ())

			&& taskDuration.isLongerThan (
				slowTaskDuration)

		) {

			StringWriter stringWriter =
				new StringWriter ();

			FormatWriter formatWriter =
				new FormatWriterWriter (
					stringWriter);

			formatWriter.writeFormat (
				"Slow %s task took %s: %s\n",
					task.taskType (),
					taskDuration,
					task.summary ());

			writeTaskRecursive (
				formatWriter,
				"  ",
				Duration.millis (1),
				task);

			log.warn (
				stringWriter.toString ());

		}

		activeTasks.remove (
			task.taskId ());

		currentTask.set (
			task.parent ());

	}

	public synchronized
	void logActiveTasks () {

		StringWriter stringWriter =
			new StringWriter ();

		FormatWriter formatWriter =
			new FormatWriterWriter (
				stringWriter);

		if (
			isEmpty (
				activeTasks)
		) {

			log.info (
				"No active tasks");

		} else {

			formatWriter.writeFormat (
				"Dumping active tasks\n");

			writeActiveTasks (
				formatWriter,
				"  ");

			log.info (
				stringWriter.toString ());

		}

	}

	public synchronized
	void writeActiveTasks (
			@NonNull FormatWriter formatWriter,
			@NonNull String indent) {

		for (
			Task task
				: activeTasks.values ()
		) {

			if (
				isNotNull (
					task.parent ())
			) {
				continue;
			}

			writeTask (
				formatWriter,
				indent,
				task);

		}

	}

	public
	void writeTask (
			@NonNull FormatWriter writer,
			@NonNull String indent,
			@NonNull Task task) {

		long meterLength =
			max (
				0l,
				(long) Math.log (
					task.duration ().getMillis ()));

		char[] meterCharacters =
			new char [
				(int) meterLength];

		Arrays.fill (
			meterCharacters,
			'=');

		writer.writeFormat (
			"%s%s: %s (%sms) [%s]\n",
			indent,
			task.taskType (),
			task.summary (),
			task.duration ().getMillis (),
			new String (
				meterCharacters));

	}

	public
	void writeTaskRecursive (
			@NonNull FormatWriter writer,
			@NonNull String indent,
			@NonNull Duration minDuration,
			@NonNull Task task) {

		writeTask (
			writer,
			indent,
			task);

		String nextIndent =
			indent + "  ";

		task.children ().stream ()

			.filter (
				childTask ->
					notShorterThan (
						childTask.duration (),
						minDuration))

			.forEach (
				childTask ->
					writeTaskRecursive (
						writer,
						nextIndent,
						minDuration,
						childTask));

	}

	@Accessors (fluent = true)
	class ActiveTaskImplementation
		implements ActiveTask {

		Task task;

		boolean concluded = false;
		boolean closed = false;

		public
		ActiveTaskImplementation (
				Task task) {

			this.task =
				task;

		}

		@Override
		public
		void success () {

			synchronized (ActivityManagerImplementation.this) {

				if (concluded)
					throw new IllegalStateException ();

				if (closed)
					throw new IllegalStateException ();

				concluded =
					true;

				task.state (
					Task.State.success);

				postProcessTask (
					task);

			}

		}

		@Override
		public <ExceptionType extends Throwable>
		ExceptionType fail (
				ExceptionType exception) {

			synchronized (ActivityManagerImplementation.this) {

				if (concluded)
					throw new IllegalStateException ();

				if (closed)
					throw new IllegalStateException ();

				concluded =
					true;

				task.state (
					Task.State.failure);

				postProcessTask (
					task);

				return exception;

			}

		}

		@Override
		public
		void close () {

			synchronized (ActivityManagerImplementation.this) {

				if (closed)
					throw new IllegalStateException ();

				closed =
					true;

				if (concluded)
					return;

				task.state (
					Task.State.unknown);

				postProcessTask (
					task);

			}

		}

		@Override
		public
		ActiveTask put (
				@NonNull String key,
				@NonNull String value) {

			task.parameters.put (
				key,
				value);

			return this;

		}

	}

}
