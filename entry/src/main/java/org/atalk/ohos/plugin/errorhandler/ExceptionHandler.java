/*
 * aTalk, ohos VoIP and Instant Messaging client
 * Copyright 2024 Eng Chong Meng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.atalk.ohos.plugin.errorhandler;

import ohos.data.DatabaseHelper;
import ohos.data.preferences.Preferences;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.aTalkApp;
import org.atalk.service.fileaccess.FileCategory;

import java.io.File;

import timber.log.Timber;

/**
 * The <code>ExceptionHandler</code> is used to catch unhandled exceptions which occur on the UI
 * <code>Thread</code>. Those exceptions normally cause current <code>Ability</code> to freeze and the
 * process usually must be killed after the Application Not Responding dialog is displayed. This
 * handler kills Jitsi process at the moment when the exception occurs, so that user don't have
 * to wait for ANR dialog. It also marks in <code>Preferences</code> that such crash has
 * occurred. Next time the Jitsi is started it will ask the user if he wants to send the logs.<br/>
 * <p>
 * Usually system restarts Jitsi and it's service automatically after the process was killed.
 * That's because the service was still bound to some <code>Activities</code> at the moment when the
 * exception occurred.<br/>
 * <p>
 * The handler is bound to the <code>Thread</code> in every <code>OSGiAbility</code>.
 *
 * @author Pawel Domas
 */
public class ExceptionHandler implements Thread.UncaughtExceptionHandler
{
	/**
	 * Parent exception handler(system default).
	 */
	private final Thread.UncaughtExceptionHandler parent;

	/**
	 * Creates new instance of <code>ExceptionHandler</code> bound to given <code>Thread</code>.
	 *
	 * @param t
	 * 		the <code>Thread</code> which will be handled.
	 */
	private ExceptionHandler(Thread t)
	{
		parent = t.getUncaughtExceptionHandler();
		t.setUncaughtExceptionHandler(this);
	}

	/**
	 * Checks and attaches the <code>ExceptionHandler</code> if it hasn't been bound already.
	 */
	public static void checkAndAttachExceptionHandler()
	{
		Thread current = Thread.currentThread();
		if (current.getUncaughtExceptionHandler() instanceof ExceptionHandler) {
			return;
		}
		// Creates and binds new handler instance
		new ExceptionHandler(current);
	}

	/**
	 * Marks the crash in <code>Preferences</code> and kills the process.
	 * Storage: /data/data/org.atalk.ohos/files/log/atalk-crash-logcat.txt
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public void uncaughtException(Thread thread, Throwable ex)
	{
		markCrashedEvent();
		parent.uncaughtException(thread, ex);

		Timber.e(ex, "uncaughtException occurred, killing the process...");

		// Save logcat for more information.
		File logcatFile;
		String logcatFN = new File("log", "atalk-crash-logcat.txt").toString();
		try {
			logcatFile = ExceptionHandlerActivator.getFileAccessService()
					.getPrivatePersistentFile(logcatFN, FileCategory.LOG);
			Runtime.getRuntime().exec("logcat -v time -f " + logcatFile.getAbsolutePath());
		}
		catch (Exception e) {
			Timber.e("Couldn't save crash logcat file.");
		}

		android.os.Process.killProcess(android.os.Process.myPid());
		System.exit(10);
	}

	/**
	 * Returns <code>Preferences</code> used to mark the crash event.
	 *
	 * @return <code>Preferences</code> used to mark the crash event.
	 */
	private static Preferences getStorage()
	{
		DatabaseHelper dbHelper = new DatabaseHelper(aTalkApp.getInstance());
		return dbHelper.getPreferences("crash");
	}

	/**
	 * Marks that the crash has occurred in <code>Preferences</code>.
	 */
	private static void markCrashedEvent()
	{
		getStorage().putBoolean("crash", true).flush();
	}

	/**
	 * Returns <code>true</code> if Jitsi crash was detected.
	 *
	 * @return <code>true</code> if Jitsi crash was detected.
	 */
	public static boolean hasCrashed()
	{
		return getStorage().getBoolean("crash", false);
	}

	/**
	 * Clears the "crashed" flag.
	 */
	public static void resetCrashedStatus()
	{
		getStorage().putBoolean("crash", false).flush();
	}
}
