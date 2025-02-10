/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.service.osgi;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.atalk.ohos.BaseFragment;
import org.osgi.framework.BundleContext;

/**
 * Class can be used to build {@link Fragment}s that require OSGI services access.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class OSGiFragment extends BaseFragment implements OSGiUiPart {
    private OSGiActivity osGiActivity;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        osGiActivity = (OSGiActivity) getActivity();
        if (osGiActivity != null)
            osGiActivity.registerOSGiFragment(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDetach() {
        osGiActivity.unregisterOSGiFragment(this);
        super.onDetach();
    }

    /**
     * {@inheritDoc}
     */
    public void start(BundleContext bundleContext)
            throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    public void stop(BundleContext bundleContext)
            throws Exception {
    }
}
