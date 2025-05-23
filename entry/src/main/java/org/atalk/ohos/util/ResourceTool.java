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
package org.atalk.ohos.util;

import java.io.IOException;

import ohos.agp.colors.Color;
import ohos.agp.colors.ColorConverter;
import ohos.agp.colors.RgbColor;
import ohos.agp.components.element.Element;
import ohos.agp.components.element.ShapeElement;
import ohos.app.Context;
import ohos.global.resource.NotExistException;
import ohos.global.resource.WrongTypeException;

public class ResourceTool {
    public static int getInt(Context context, int id, int defaultValue) {
        int value = defaultValue;
        try {
            value = context.getResourceManager().getElement(id).getInteger();
        } catch (IOException | NotExistException | WrongTypeException e) {
            e.printStackTrace();
        }
        return value;
    }

    public static float getFloat(Context context, int id, float defaultValue) {
        float value = defaultValue;
        try {
            value = context.getResourceManager().getElement(id).getFloat();
        } catch (IOException | NotExistException | WrongTypeException e) {
            e.printStackTrace();
        }
        return value;
    }

    public static int getColor(Context context, int id, int defaultValue) {
        int color = defaultValue;
        try {
            color = context.getResourceManager().getElement(id).getColor();
        } catch (IOException | NotExistException | WrongTypeException e) {
            e.printStackTrace();
        }
        return color;
    }

    public static ShapeElement getElement(Context context, int bgColorId) {
        return new ShapeElement(new RgbColor(bgColorId));
    }

    public static ShapeElement getElement(Context context, Color color) {
        return new ShapeElement(ColorConverter.toRgb(color));
    }
}