/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.ohos.gui.widgets;

import ohos.agp.components.AttrSet;
import ohos.agp.components.DirectionalLayout;
import ohos.agp.components.TableLayout;
import ohos.app.Context;

/**
 * Custom layout that handles fixes table header, by measuring maximum column widths in both header and table body. Then
 * synchronizes those maximum values in header and body columns widths.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class ScrollingTable extends DirectionalLayout
{
    /**
     * Create a new instance of <code>ScrollingTable</code>
     *
     * @param context the context
     */
    public ScrollingTable(Context context)
    {
        super(context);
    }

    /**
     * Creates a new instance of <code>ScrollingTable</code>.
     *
     * @param context the context
     * @param attrs the attribute set
     */
    public ScrollingTable(Context context, AttrSet attrs)
    {
        super(context, attrs);
    }

    @Override
    protected void onArrange(boolean changed, int left, int top, int right, int bottom)
    {
        super.onArrange(changed, left, top, right, bottom);

        TableLayout header = findComponentById(ResourceTable.Id_table_header);
        TableLayout body = findComponentById(ResourceTable.Id_table_body);

        // Find max column widths
        int[] headerWidths = findMaxWidths(header);
        int[] bodyWidths = findMaxWidths(body);

        if (bodyWidths == null) {
            // Table is empty
            return;
        }

        int[] maxWidths = new int[bodyWidths.length];

        for (int i = 0; i < headerWidths.length; i++) {
            maxWidths[i] = Math.max(headerWidths[i], bodyWidths[i]);
        }

        // Set column widths to max values
        setColumnWidths(header, maxWidths);
        setColumnWidths(body, maxWidths);
    }

    /**
     * Finds maximum columns widths in given table layout.
     *
     * @param table table layout that will be examined for max column widths.
     * @return array of max columns widths for given table, it's length is equal to table's column count.
     */
    private int[] findMaxWidths(TableLayout table)
    {
        int[] colWidths = null;

        for (int rowNum = 0; rowNum < table.getChildCount(); rowNum++) {
            TableRow row = (TableRow) table.getChildAt(rowNum);

            if (colWidths == null)
                colWidths = new int[row.getChildCount()];

            for (int colNum = 0; colNum < row.getChildCount(); colNum++) {
                int cellWidth = row.getChildAt(colNum).getWidth();
                if (cellWidth > colWidths[colNum]) {
                    colWidths[colNum] = cellWidth;
                }
            }
        }

        return colWidths;
    }

    /**
     * Adjust given table columns width to sizes given in <code>widths</code> array.
     *
     * @param table the table layout which columns will be adjusted
     * @param widths array of columns widths to set
     */
    private void setColumnWidths(TableLayout table, int[] widths)
    {
        for (int rowNum = 0; rowNum < table.getChildCount(); rowNum++) {
            TableRow row = (TableRow) table.getChildAt(rowNum);

            for (int colNum = 0; colNum < row.getChildCount(); colNum++) {
                Component column = row.getChildAt(colNum);
                TableRow.LayoutParams params = (TableRow.LayoutParams) column.getLayoutParams();
                params.width = widths[colNum];
            }
        }
    }
}
