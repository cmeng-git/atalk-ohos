/*
 * aTalk, android VoIP and Instant Messaging client
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
package org.atalk.ohos.gui.widgets;

import ohos.agp.components.AttrSet;
import ohos.agp.components.Component;
import ohos.agp.render.Canvas;
import ohos.agp.render.Paint;
import ohos.agp.utils.Color;
import ohos.app.Context;

import static ohos.agp.utils.LayoutAlignment.CENTER;

public class UnreadCountCustomView extends Component implements Component.DrawTask {

    private int unreadCount;
    private Paint bgPaint, textPaint;
    private int backgroundColor = 0xff1D475D;

    public UnreadCountCustomView(Context context) {
        super(context);
        init();
    }

    public UnreadCountCustomView(Context context, AttrSet attrs) {
        super(context, attrs);
        initXMLAttrs(context, attrs);
        init();
    }

    public UnreadCountCustomView(Context context, AttrSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initXMLAttrs(context, attrs);
        init();
    }

    // Currently not support in aTalk
    private void initXMLAttrs(Context context, AttrSet attrs) {
        // TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.UnreadCountCustomView);
        // setBackgroundColor(a.getColor(a.getIndex(0), ContextCompat.getColor(context, ResourceTable.Color_green700)));
        // a.recycle();
    }

    void init() {
        addDrawTask(this);
        bgPaint = new Paint();
        bgPaint.setColor(new Color(backgroundColor));
        bgPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(CENTER);
        textPaint.setAntiAlias(true);
        textPaint.setFakeBoldText(true);
        // textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    }

    @Override
    public void onDraw(Component component, Canvas canvas) {
        float midx = getWidth() / 2.0f;
        float midy = getHeight() / 2.0f;
        float radius = Math.min(getWidth(), getHeight()) / 2.0f;
        float textOffset = getWidth() / 6.0f;
        textPaint.setTextSize((int) (0.95f * radius));
        canvas.drawCircle(midx, midy, radius * 0.94f, bgPaint);
        canvas.drawText(textPaint, unreadCount > 999 ? "∞" : String.valueOf(unreadCount), midx, midy + textOffset);
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
        invalidate();
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
}