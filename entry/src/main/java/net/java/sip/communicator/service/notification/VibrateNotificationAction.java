/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.notification;

/**
 * <code>VibrateNotificationAction</code> is meant to define haptic feedback notification using device's vibrator.<br/>
 * <br/>
 *
 * Given array of <code>long</code> are the duration for which to turn on or off the vibrator in milliseconds.
 * The first value indicates the number of milliseconds to wait before turning the vibrator on. The next value
 * indicates the number of milliseconds for which to keep the vibrator on before turning it off and so on.<br/>
 * <br/>
 *
 * The <code>repeat</code> parameter is an index into the pattern at which it will be looped until the
 * {@link VibrateNotificationHandler#cancel()} method is called.
 *
 * @author Pawel Domas
 */
public class VibrateNotificationAction extends NotificationAction
{
    /**
     * The pattern of off/on intervals in millis that will be played.
     */
    private final long[] pattern;

    /**
     * Repeat index into the pattern(-1 to disable repeat).
     */
    private final int repeat;

    /**
     * Descriptor that can be used to identify action.
     */
    private final String descriptor;

    /**
     * Vibrate constantly for the specified period of time.
     *
     * @param descriptor string identifier of this action.
     * @param millis the number of milliseconds to vibrate.
     */
    public VibrateNotificationAction(String descriptor, long millis)
    {
        super(NotificationAction.ACTION_VIBRATE);
        this.pattern = new long[2];
        pattern[0] = 0;
        pattern[1] = millis;
        repeat = -1;
        this.descriptor = descriptor;
    }

    /**
     * Vibrate using given <code>patter</code> and optionally loop if the <code>repeat</code> index is not <code>-1</code>.
     *
     * @param descriptor the string identifier of this action.
     * @param patter the array containing vibrate pattern intervals.
     * @param repeat the index into the patter at which it will be looped (-1 to disable repeat).
     * @see VibrateNotificationAction
     */
    public VibrateNotificationAction(String descriptor, long[] patter, int repeat)
    {
        super(NotificationAction.ACTION_VIBRATE);
        this.pattern = patter;
        this.repeat = repeat;
        this.descriptor = descriptor;
    }

    /**
     * The string identifier of this action.
     *
     * @return string identifier of this action which can be used to distinguish different actions.
     */
    public String getDescriptor()
    {
        return descriptor;
    }

    /**
     * Returns vibrate pattern array.
     *
     * @return vibrate pattern array.
     */
    public long[] getPattern()
    {
        return pattern;
    }

    /**
     * The index at which the pattern shall be looped during playback or <code>-1</code> to play it once.
     *
     * @return the index at which the pattern will be looped or <code>-1</code> to play it once.
     */
    public int getRepeat()
    {
        return repeat;
    }
}
