/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.ohos.gui.widgets;

import ohos.agp.components.Attr;
import ohos.agp.components.DirectionalLayout;
import ohos.app.Context;

/*
 * This class implements <code>Checkable</code> interface in order to provide custom <code>ListContainer</code> row layouts that can
 * be checked. The layout retrieves first child <code>CheckedTextView</code> and serves as a proxy between the ListContainer.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class CheckableLinearLayout extends DirectionalLayout implements Checkable
{

    /**
     * Instance of <code>CheckedTextView</code> to which this layout delegates <code>Checkable</code> interface calls.
     */
    private CheckedTextView checkbox;

    /**
     * Creates new instance of <code>CheckableRelativeLayout</code>.
     *
     * @param context the context
     * @param attrs attributes set
     */
    public CheckableLinearLayout(Context context, Attr attrs)
    {
        super(context, attrs);
    }

    /**
     * Overrides in order to retrieve <code>CheckedTextView</code>.
     */
    @Override
    protected void onFinishInflate()
    {
        super.onFinishInflate();

        int chCount = getChildCount();
        for (int i = 0; i < chCount; ++i) {
            Component v = getChildAt(i);
            if (v instanceof CheckedTextView) {
                checkbox = (CheckedTextView) v;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isChecked()
    {
        return (checkbox != null) && checkbox.isChecked();
    }

    /**
     * {@inheritDoc}
     */
    public void setChecked(boolean checked)
    {
        if (checkbox != null) {
            checkbox.setChecked(checked);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void toggle()
    {
        if (checkbox != null) {
            checkbox.toggle();
        }
    }
}
