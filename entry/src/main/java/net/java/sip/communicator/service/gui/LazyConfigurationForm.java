/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

import net.java.sip.communicator.service.gui.internal.GuiServiceActivator;
import net.java.sip.communicator.service.resources.ResourceManagementServiceUtils;

import org.atalk.service.resources.ResourceManagementService;

import java.lang.reflect.UndeclaredThrowableException;

/**
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 */
public class LazyConfigurationForm implements ConfigurationForm
{
    /**
     * The <code>ResourceManagementService</code> used to obtain any resources.
     */
    private static ResourceManagementService resources;

    /**
     * Returns an instance of the <code>ResourceManagementService</code>, which
     * could be used to obtain any resources.
     *
     * @return an instance of the <code>ResourceManagementService</code>
     */
    private static ResourceManagementService getResources()
    {
        if (resources == null)
            resources = ResourceManagementServiceUtils.getService(GuiServiceActivator.getBundleContext());
        return resources;
    }

    /**
     * The form class loader.
     */
    private final ClassLoader formClassLoader;

    /**
     * The class name of the form.
     */
    private final String formClassName;

    /**
     * The identifier of the icon.
     */
    private final String iconID;

    /**
     * The index of the form in the parent container.
     */
    private final int index;

    /**
     * The title identifier.
     */
    private final String titleID;

    /**
     * Indicates if this form is advanced.
     */
    private final boolean isAdvanced;

    /**
     * Creates an instance of <code>LazyConfigurationForm</code>.
     *
     * @param formClassName the class name of the configuration form
     * @param formClassLoader the class loader
     * @param iconID the identifier of the form icon
     * @param titleID the identifier of the form title
     */
    public LazyConfigurationForm(String formClassName, ClassLoader formClassLoader, String iconID, String titleID)
    {
        this(formClassName, formClassLoader, iconID, titleID, -1, false);
    }

    /**
     * Creates an instance of <code>LazyConfigurationForm</code>.
     *
     * @param formClassName the class name of the configuration form
     * @param formClassLoader the class loader
     * @param iconID the identifier of the form icon
     * @param titleID the identifier of the form title
     * @param index the index of the form in the parent container
     */
    public LazyConfigurationForm(String formClassName,
            ClassLoader formClassLoader, String iconID, String titleID, int index)
    {
        this(formClassName, formClassLoader, iconID, titleID, index, false);
    }

    /**
     * Creates an instance of <code>LazyConfigurationForm</code>.
     *
     * @param formClassName the class name of the configuration form
     * @param formClassLoader the class loader
     * @param iconID the identifier of the form icon
     * @param titleID the identifier of the form title
     * @param index the index of the form in the parent container
     * @param isAdvanced indicates if the form is advanced configuration form
     */
    public LazyConfigurationForm(String formClassName,
            ClassLoader formClassLoader,
            String iconID,
            String titleID,
            int index,
            boolean isAdvanced)
    {
        this.formClassName = formClassName;
        this.formClassLoader = formClassLoader;
        this.iconID = iconID;
        this.titleID = titleID;
        this.index = index;
        this.isAdvanced = isAdvanced;
    }

    /**
     * Returns the form component.
     *
     * @return the form component
     */
    public Object getForm()
    {
        Exception exception;
        try {
            return Class
                    .forName(getFormClassName(), true, getFormClassLoader())
                    .newInstance();
        } catch (ClassNotFoundException ex) {
            exception = ex;
        } catch (IllegalAccessException ex) {
            exception = ex;
        } catch (InstantiationException ex) {
            exception = ex;
        }
        throw new UndeclaredThrowableException(exception);
    }

    /**
     * Returns the form class loader.
     *
     * @return the form class loader
     */
    protected ClassLoader getFormClassLoader()
    {
        return formClassLoader;
    }

    /**
     * Returns the form class name.
     *
     * @return the form class name
     */
    public String getFormClassName()
    {
        return formClassName;
    }

    /**
     * Returns the icon of the form.
     *
     * @return a byte array containing the icon of the form
     */
    public byte[] getIcon()
    {
        return getResources().getImageInBytes(getIconID());
    }

    /**
     * Returns the identifier of the icon.
     *
     * @return the identifier of the icon
     */
    protected String getIconID()
    {
        return iconID;
    }

    /**
     * Returns the index of the form in its parent container.
     *
     * @return the index of the form in its parent container
     */
    public int getIndex()
    {
        return index;
    }

    /**
     * Returns the title of the form.
     *
     * @return the title of the form
     */
    public String getTitle()
    {
        return getResources().getI18NString(getTitleID());
    }

    /**
     * Returns the identifier of the title of the form.
     *
     * @return the identifier of the title of the form
     */
    protected String getTitleID()
    {
        return titleID;
    }

    /**
     * Indicates if the form is an advanced form.
     *
     * @return <code>true</code> to indicate that this is an advanced form, otherwise returns <code>false</code>
     */
    public boolean isAdvanced()
    {
        return isAdvanced;
    }
}
