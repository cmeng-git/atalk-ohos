package timber.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohos.hiviewdfx.HiLog;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import kotlin.Deprecated;
import kotlin.Metadata;
import kotlin.ReplaceWith;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.JvmName;
import kotlin.jvm.JvmStatic;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.text.StringsKt;

@Metadata(
        mv = {1, 7, 0},
        k = 1,
        d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0005\u0018\u0000 \u00042\u00020\u0001:\u0003\u0003\u0004\u0005B\u0007\b\u0002¢\u0006\u0002\u0010\u0002¨\u0006\u0006"},
        d2 = {"Ltimber/log/Timber;", "", "()V", "DebugTree", "Forest", "Tree", "sources for library Gradle: com.jakewharton.timber:timber:5.0.1@aar"}
)

public final class Timber {
    private static final ArrayList trees = new ArrayList();
    private static volatile Tree[] treeArray = new Tree[0];
    @NotNull
    public static final Forest Forest = new Forest((DefaultConstructorMarker) null);

    private Timber() {
        // throw (Throwable)(new AssertionError());
    }

    public static void v(@NonNls @Nullable String message, @NotNull Object... args) {
        Forest.v(message, args);
    }

    public static void v(@Nullable Throwable t, @NonNls @Nullable String message, @NotNull Object... args) {
        Forest.v(t, message, args);
    }

    public static void v(@Nullable Throwable t) {
        Forest.v(t);
    }

    public static void d(@NonNls @Nullable String message, @NotNull Object... args) {
        Forest.d(message, args);
    }

    public static void d(@Nullable Throwable t, @NonNls @Nullable String message, @NotNull Object... args) {
        Forest.d(t, message, args);
    }

    public static void d(@Nullable Throwable t) {
        Forest.d(t);
    }

    public static void i(@NonNls @Nullable String message, @NotNull Object... args) {
        Forest.i(message, args);
    }

    public static void i(@Nullable Throwable t, @NonNls @Nullable String message, @NotNull Object... args) {
        Forest.i(t, message, args);
    }

    public static void i(@Nullable Throwable t) {
        Forest.i(t);
    }

    public static void w(@NonNls @Nullable String message, @NotNull Object... args) {
        Forest.w(message, args);
    }

    public static void w(@Nullable Throwable t, @NonNls @Nullable String message, @NotNull Object... args) {
        Forest.w(t, message, args);
    }

    public static void w(@Nullable Throwable t) {
        Forest.w(t);
    }

    public static void e(@NonNls @Nullable String message, @NotNull Object... args) {
        Forest.e(message, args);
    }

    public static void e(@Nullable Throwable t, @NonNls @Nullable String message, @NotNull Object... args) {
        Forest.e(t, message, args);
    }

    public static void e(@Nullable Throwable t) {
        Forest.e(t);
    }

    public static void wtf(@NonNls @Nullable String message, @NotNull Object... args) {
        Forest.wtf(message, args);
    }

    public static void wtf(@Nullable Throwable t, @NonNls @Nullable String message, @NotNull Object... args) {
        Forest.wtf(t, message, args);
    }

    public static void wtf(@Nullable Throwable t) {
        Forest.wtf(t);
    }

    public static void log(int priority, @NonNls @Nullable String message, @NotNull Object... args) {
        Forest.log(priority, message, args);
    }

    public static void log(int priority, @Nullable Throwable t, @NonNls @Nullable String message, @NotNull Object... args) {
        Forest.log(priority, t, message, args);
    }

    public static void log(int priority, @Nullable Throwable t) {
        Forest.log(priority, t);
    }

    @NotNull
    public static Tree asTree() {
        int $i$f$asTree = 0;
        return Forest.asTree();
    }

    @NotNull
    public static final Tree tag(@NotNull String tag) {
        return Forest.tag(tag);
    }

    public static final void plant(@NotNull Tree tree) {
        Forest.plant(tree);
    }

    public static final void plant(@NotNull Tree... trees) {
        Forest.plant(trees);
    }

    public static final void uproot(@NotNull Tree tree) {
        Forest.uproot(tree);
    }

    public static final void uprootAll() {
        Forest.uprootAll();
    }

    @NotNull
    public static final List forest() {
        return Forest.forest();
    }

    @JvmName(
            name = "treeCount"
    )
    public static final int treeCount() {
        return Forest.treeCount();
    }

    @Metadata(
            mv = {1, 7, 0},
            k = 1,
            d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u0011\n\u0002\b\u0002\n\u0002\u0010\u0003\n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\b\n\u0002\b\b\b&\u0018\u00002\u00020\u0001B\u0005¢\u0006\u0002\u0010\u0002J/\u0010\u000b\u001a\u00020\f2\b\u0010\r\u001a\u0004\u0018\u00010\u00052\u0016\u0010\u000e\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00010\u000f\"\u0004\u0018\u00010\u0001H\u0016¢\u0006\u0002\u0010\u0010J\u0012\u0010\u000b\u001a\u00020\f2\b\u0010\u0011\u001a\u0004\u0018\u00010\u0012H\u0016J9\u0010\u000b\u001a\u00020\f2\b\u0010\u0011\u001a\u0004\u0018\u00010\u00122\b\u0010\r\u001a\u0004\u0018\u00010\u00052\u0016\u0010\u000e\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00010\u000f\"\u0004\u0018\u00010\u0001H\u0016¢\u0006\u0002\u0010\u0013J/\u0010\u0014\u001a\u00020\f2\b\u0010\r\u001a\u0004\u0018\u00010\u00052\u0016\u0010\u000e\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00010\u000f\"\u0004\u0018\u00010\u0001H\u0016¢\u0006\u0002\u0010\u0010J\u0012\u0010\u0014\u001a\u00020\f2\b\u0010\u0011\u001a\u0004\u0018\u00010\u0012H\u0016J9\u0010\u0014\u001a\u00020\f2\b\u0010\u0011\u001a\u0004\u0018\u00010\u00122\b\u0010\r\u001a\u0004\u0018\u00010\u00052\u0016\u0010\u000e\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00010\u000f\"\u0004\u0018\u00010\u0001H\u0016¢\u0006\u0002\u0010\u0013J'\u0010\u0015\u001a\u00020\u00052\u0006\u0010\r\u001a\u00020\u00052\u0010\u0010\u000e\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00010\u000fH\u0014¢\u0006\u0002\u0010\u0016J\u0010\u0010\u0017\u001a\u00020\u00052\u0006\u0010\u0011\u001a\u00020\u0012H\u0002J/\u0010\u0018\u001a\u00020\f2\b\u0010\r\u001a\u0004\u0018\u00010\u00052\u0016\u0010\u000e\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00010\u000f\"\u0004\u0018\u00010\u0001H\u0016¢\u0006\u0002\u0010\u0010J\u0012\u0010\u0018\u001a\u00020\f2\b\u0010\u0011\u001a\u0004\u0018\u00010\u0012H\u0016J9\u0010\u0018\u001a\u00020\f2\b\u0010\u0011\u001a\u0004\u0018\u00010\u00122\b\u0010\r\u001a\u0004\u0018\u00010\u00052\u0016\u0010\u000e\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00010\u000f\"\u0004\u0018\u00010\u0001H\u0016¢\u0006\u0002\u0010\u0013J\u0010\u0010\u0019\u001a\u00020\u001a2\u0006\u0010\u001b\u001a\u00020\u001cH\u0015J\u001a\u0010\u0019\u001a\u00020\u001a2\b\u0010\b\u001a\u0004\u0018\u00010\u00052\u0006\u0010\u001b\u001a\u00020\u001cH\u0014J7\u0010\u001d\u001a\u00020\f2\u0006\u0010\u001b\u001a\u00020\u001c2\b\u0010\r\u001a\u0004\u0018\u00010\u00052\u0016\u0010\u000e\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00010\u000f\"\u0004\u0018\u00010\u0001H\u0016¢\u0006\u0002\u0010\u001eJ,\u0010\u001d\u001a\u00020\f2\u0006\u0010\u001b\u001a\u00020\u001c2\b\u0010\b\u001a\u0004\u0018\u00010\u00052\u0006\u0010\r\u001a\u00020\u00052\b\u0010\u0011\u001a\u0004\u0018\u00010\u0012H$J\u001a\u0010\u001d\u001a\u00020\f2\u0006\u0010\u001b\u001a\u00020\u001c2\b\u0010\u0011\u001a\u0004\u0018\u00010\u0012H\u0016JA\u0010\u001d\u001a\u00020\f2\u0006\u0010\u001b\u001a\u00020\u001c2\b\u0010\u0011\u001a\u0004\u0018\u00010\u00122\b\u0010\r\u001a\u0004\u0018\u00010\u00052\u0016\u0010\u000e\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00010\u000f\"\u0004\u0018\u00010\u0001H\u0016¢\u0006\u0002\u0010\u001fJA\u0010 \u001a\u00020\f2\u0006\u0010\u001b\u001a\u00020\u001c2\b\u0010\u0011\u001a\u0004\u0018\u00010\u00122\b\u0010\r\u001a\u0004\u0018\u00010\u00052\u0016\u0010\u000e\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00010\u000f\"\u0004\u0018\u00010\u0001H\u0002¢\u0006\u0002\u0010\u001fJ/\u0010!\u001a\u00020\f2\b\u0010\r\u001a\u0004\u0018\u00010\u00052\u0016\u0010\u000e\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00010\u000f\"\u0004\u0018\u00010\u0001H\u0016¢\u0006\u0002\u0010\u0010J\u0012\u0010!\u001a\u00020\f2\b\u0010\u0011\u001a\u0004\u0018\u00010\u0012H\u0016J9\u0010!\u001a\u00020\f2\b\u0010\u0011\u001a\u0004\u0018\u00010\u00122\b\u0010\r\u001a\u0004\u0018\u00010\u00052\u0016\u0010\u000e\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00010\u000f\"\u0004\u0018\u00010\u0001H\u0016¢\u0006\u0002\u0010\u0013J/\u0010\"\u001a\u00020\f2\b\u0010\r\u001a\u0004\u0018\u00010\u00052\u0016\u0010\u000e\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00010\u000f\"\u0004\u0018\u00010\u0001H\u0016¢\u0006\u0002\u0010\u0010J\u0012\u0010\"\u001a\u00020\f2\b\u0010\u0011\u001a\u0004\u0018\u00010\u0012H\u0016J9\u0010\"\u001a\u00020\f2\b\u0010\u0011\u001a\u0004\u0018\u00010\u00122\b\u0010\r\u001a\u0004\u0018\u00010\u00052\u0016\u0010\u000e\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00010\u000f\"\u0004\u0018\u00010\u0001H\u0016¢\u0006\u0002\u0010\u0013J/\u0010#\u001a\u00020\f2\b\u0010\r\u001a\u0004\u0018\u00010\u00052\u0016\u0010\u000e\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00010\u000f\"\u0004\u0018\u00010\u0001H\u0016¢\u0006\u0002\u0010\u0010J\u0012\u0010#\u001a\u00020\f2\b\u0010\u0011\u001a\u0004\u0018\u00010\u0012H\u0016J9\u0010#\u001a\u00020\f2\b\u0010\u0011\u001a\u0004\u0018\u00010\u00122\b\u0010\r\u001a\u0004\u0018\u00010\u00052\u0016\u0010\u000e\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00010\u000f\"\u0004\u0018\u00010\u0001H\u0016¢\u0006\u0002\u0010\u0013R\u001c\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u00048@X\u0080\u0004¢\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007R\u0016\u0010\b\u001a\u0004\u0018\u00010\u00058PX\u0090\u0004¢\u0006\u0006\u001a\u0004\b\t\u0010\n¨\u0006$"},
            d2 = {"Ltimber/log/Timber$Tree;", "", "()V", "explicitTag", "Ljava/lang/ThreadLocal;", "", "getExplicitTag$sources_for_library_Gradle__com_jakewharton_timber_timber_5_0_1_aar", "()Ljava/lang/ThreadLocal;", "tag", "getTag$sources_for_library_Gradle__com_jakewharton_timber_timber_5_0_1_aar", "()Ljava/lang/String;", "d", "", "message", "args", "", "(Ljava/lang/String;[Ljava/lang/Object;)V", "t", "", "(Ljava/lang/Throwable;Ljava/lang/String;[Ljava/lang/Object;)V", "e", "formatMessage", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", "getStackTraceString", "i", "isLoggable", "", "priority", "", "log", "(ILjava/lang/String;[Ljava/lang/Object;)V", "(ILjava/lang/Throwable;Ljava/lang/String;[Ljava/lang/Object;)V", "prepareLog", "v", "w", "wtf", "sources for library Gradle: com.jakewharton.timber:timber:5.0.1@aar"}
    )
    public abstract static class Tree {
        @NotNull
        private final ThreadLocal explicitTag = new ThreadLocal();

        // $FF: synthetic method
        public final ThreadLocal getExplicitTag$sources_for_library_Gradle__com_jakewharton_timber_timber_5_0_1_aar() {
            return this.explicitTag;
        }

        // $FF: synthetic method
        public String getTag$sources_for_library_Gradle__com_jakewharton_timber_timber_5_0_1_aar() {
            String tag = (String) this.explicitTag.get();
            if (tag != null) {
                this.explicitTag.remove();
            }

            return tag;
        }

//        public void v(@Nullable String message, @NotNull Object... args) {
//            Intrinsics.checkNotNullParameter(args, "args");
//            this.prepareLog(Log.VERBOSE, (Throwable) null, message, Arrays.copyOf(args, args.length));
//        }
//
//        public void v(@Nullable Throwable t, @Nullable String message, @NotNull Object... args) {
//            Intrinsics.checkNotNullParameter(args, "args");
//            this.prepareLog(Log.VERBOSE, t, message, Arrays.copyOf(args, args.length));
//        }
//
//        public void v(@Nullable Throwable t) {
//            this.prepareLog(Log.VERBOSE, t, (String) null);
//        }

        public void d(@Nullable String message, @NotNull Object... args) {
            Intrinsics.checkNotNullParameter(args, "args");
            this.prepareLog(HiLog.DEBUG, (Throwable) null, message, Arrays.copyOf(args, args.length));
        }

        public void d(@Nullable Throwable t, @Nullable String message, @NotNull Object... args) {
            Intrinsics.checkNotNullParameter(args, "args");
            this.prepareLog(HiLog.DEBUG, t, message, Arrays.copyOf(args, args.length));
        }

        public void d(@Nullable Throwable t) {
            this.prepareLog(HiLog.DEBUG, t, (String) null);
        }

        public void i(@Nullable String message, @NotNull Object... args) {
            Intrinsics.checkNotNullParameter(args, "args");
            this.prepareLog(HiLog.INFO, (Throwable) null, message, Arrays.copyOf(args, args.length));
        }

        public void i(@Nullable Throwable t, @Nullable String message, @NotNull Object... args) {
            Intrinsics.checkNotNullParameter(args, "args");
            this.prepareLog(HiLog.INFO, t, message, Arrays.copyOf(args, args.length));
        }

        public void i(@Nullable Throwable t) {
            this.prepareLog(HiLog.INFO, t, (String) null);
        }

        public void w(@Nullable String message, @NotNull Object... args) {
            Intrinsics.checkNotNullParameter(args, "args");
            this.prepareLog(HiLog.WARN, (Throwable) null, message, Arrays.copyOf(args, args.length));
        }

        public void w(@Nullable Throwable t, @Nullable String message, @NotNull Object... args) {
            Intrinsics.checkNotNullParameter(args, "args");
            this.prepareLog(HiLog.WARN, t, message, Arrays.copyOf(args, args.length));
        }

        public void w(@Nullable Throwable t) {
            this.prepareLog(HiLog.WARN, t, (String) null);
        }

        public void e(@Nullable String message, @NotNull Object... args) {
            Intrinsics.checkNotNullParameter(args, "args");
            this.prepareLog(HiLog.ERROR, (Throwable) null, message, Arrays.copyOf(args, args.length));
        }

        public void e(@Nullable Throwable t, @Nullable String message, @NotNull Object... args) {
            Intrinsics.checkNotNullParameter(args, "args");
            this.prepareLog(HiLog.ERROR, t, message, Arrays.copyOf(args, args.length));
        }

        public void e(@Nullable Throwable t) {
            this.prepareLog(HiLog.ERROR, t, (String) null);
        }

        public void wtf(@Nullable String message, @NotNull Object... args) {
            Intrinsics.checkNotNullParameter(args, "args");
            this.prepareLog(HiLog.FATAL, (Throwable) null, message, Arrays.copyOf(args, args.length));
        }

        public void wtf(@Nullable Throwable t, @Nullable String message, @NotNull Object... args) {
            Intrinsics.checkNotNullParameter(args, "args");
            this.prepareLog(HiLog.FATAL, t, message, Arrays.copyOf(args, args.length));
        }

        public void wtf(@Nullable Throwable t) {
            this.prepareLog(HiLog.FATAL, t, (String) null);
        }

        public void log(int priority, @Nullable String message, @NotNull Object... args) {
            Intrinsics.checkNotNullParameter(args, "args");
            this.prepareLog(priority, (Throwable) null, message, Arrays.copyOf(args, args.length));
        }

        public void log(int priority, @Nullable Throwable t, @Nullable String message, @NotNull Object... args) {
            Intrinsics.checkNotNullParameter(args, "args");
            this.prepareLog(priority, t, message, Arrays.copyOf(args, args.length));
        }

        public void log(int priority, @Nullable Throwable t) {
            this.prepareLog(priority, t, (String) null);
        }

        /**
         * @deprecated
         */
        @Deprecated(
                message = "Use isLoggable(String, int)",
                replaceWith = @ReplaceWith(
                        imports = {},
                        expression = "this.isLoggable(null, priority)"
                )
        )
        protected boolean isLoggable(int priority) {
            return true;
        }

        protected boolean isLoggable(@Nullable String tag, int priority) {
            return this.isLoggable(priority);
        }

        private final void prepareLog(int priority, Throwable t, String message, Object... args) {
            String tag = this.getTag$sources_for_library_Gradle__com_jakewharton_timber_timber_5_0_1_aar();
            if (this.isLoggable(tag, priority)) {
                if (message == null || message.length() == 0) {
                    if (t == null) {
                        return;
                    }

                    message = this.getStackTraceString(t);
                } else {
                    if (args.length != 0) {
                        message = this.formatMessage(message, args);
                    }

                    if (t != null) {
                        message = message + "\n" + this.getStackTraceString(t);
                    }
                }

                this.log(priority, tag, message, t);
            }
        }

        @NotNull
        protected String formatMessage(@NotNull String message, @NotNull Object[] args) {
            Intrinsics.checkNotNullParameter(message, "message");
            Intrinsics.checkNotNullParameter(args, "args");
            Object[] var4 = Arrays.copyOf(args, args.length);
            String var10000 = String.format(message, Arrays.copyOf(var4, var4.length));
            Intrinsics.checkNotNullExpressionValue(var10000, "format(this, *args)");
            return var10000;
        }

        private final String getStackTraceString(Throwable t) {
            StringWriter sw = new StringWriter(256);
            PrintWriter pw = new PrintWriter((Writer) sw, false);
            t.printStackTrace(pw);
            pw.flush();
            String var10000 = sw.toString();
            Intrinsics.checkNotNullExpressionValue(var10000, "sw.toString()");
            return var10000;
        }

        protected abstract void log(int var1, @Nullable String var2, @NotNull String var3, @Nullable Throwable var4);
    }

    @Metadata(
            mv = {1, 7, 0},
            k = 1,
            d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u0003\n\u0002\b\u0002\b\u0016\u0018\u0000 \u00142\u00020\u0001:\u0001\u0014B\u0005¢\u0006\u0002\u0010\u0002J\u0012\u0010\n\u001a\u0004\u0018\u00010\u00052\u0006\u0010\u000b\u001a\u00020\fH\u0014J,\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u00102\b\u0010\u0007\u001a\u0004\u0018\u00010\u00052\u0006\u0010\u0011\u001a\u00020\u00052\b\u0010\u0012\u001a\u0004\u0018\u00010\u0013H\u0014R\u001c\u0010\u0003\u001a\u0010\u0012\f\u0012\n \u0006*\u0004\u0018\u00010\u00050\u00050\u0004X\u0082\u0004¢\u0006\u0002\n\u0000R\u0016\u0010\u0007\u001a\u0004\u0018\u00010\u00058PX\u0090\u0004¢\u0006\u0006\u001a\u0004\b\b\u0010\t¨\u0006\u0015"},
            d2 = {"Ltimber/log/Timber$DebugTree;", "Ltimber/log/Timber$Tree;", "()V", "fqcnIgnore", "", "", "kotlin.jvm.PlatformType", "tag", "getTag$sources_for_library_Gradle__com_jakewharton_timber_timber_5_0_1_aar", "()Ljava/lang/String;", "createStackElementTag", "element", "Ljava/lang/StackTraceElement;", "log", "", "priority", "", "message", "t", "", "Companion", "sources for library Gradle: com.jakewharton.timber:timber:5.0.1@aar"}
    )
    @SourceDebugExtension({"SMAP\nTimber.kt\nKotlin\n*S Kotlin\n*F\n+ 1 Timber.kt\ntimber/log/Timber$DebugTree\n+ 2 _Arrays.kt\nkotlin/collections/ArraysKt___ArraysKt\n+ 3 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,456:1\n1109#2,2:457\n1#3:459\n*S KotlinDebug\n*F\n+ 1 Timber.kt\ntimber/log/Timber$DebugTree\n*L\n206#1:457,2\n*E\n"})
    public static class DebugTree extends Tree {
        private final List fqcnIgnore = CollectionsKt.listOf(new String[]{Timber.class.getName(), Forest.class.getName(), Tree.class.getName(), DebugTree.class.getName()});
        private static final int MAX_LOG_LENGTH = 4000;
        private static final int MAX_TAG_LENGTH = 23;
        private static final Pattern ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$");
        @NotNull
        public static final Companion Companion = new Companion((DefaultConstructorMarker) null);

        @Nullable
        public String getTag$sources_for_library_Gradle__com_jakewharton_timber_timber_5_0_1_aar() {
            String var10000 = super.getTag$sources_for_library_Gradle__com_jakewharton_timber_timber_5_0_1_aar();
            if (var10000 == null) {
                StackTraceElement[] var9 = (new Throwable()).getStackTrace();
                Intrinsics.checkNotNullExpressionValue(var9, "Throwable().stackTrace");
                Object[] $this$first$iv = var9;
                int $i$f$first = false;
                int var3 = 0;
                int var4 = $this$first$iv.length;

                while (true) {
                    if (var3 >= var4) {
                        throw new NoSuchElementException("Array contains no element matching the predicate.");
                    }

                    Object element$iv = $this$first$iv[var3];
                    int var7 = false;
                    List var10 = this.fqcnIgnore;
                    Intrinsics.checkNotNullExpressionValue(element$iv, "it");
                    if (!var10.contains(element$iv.getClassName())) {
                        boolean var8 = false;
                        var10000 = ((DebugTree) this).createStackElementTag(element$iv);
                        break;
                    }

                    ++var3;
                }
            }

            return var10000;
        }

        @Nullable
        protected String createStackElementTag(@NotNull StackTraceElement element) {
            String className = element.getClassName();
            String tag = className.substring(className.lastIndexOf("."));

            Matcher m = ANONYMOUS_CLASS.matcher((CharSequence) tag);
            if (m.find()) {
                className = m.replaceAll("");
                Intrinsics.checkNotNullExpressionValue(className, "m.replaceAll(\"\")");
                tag = className;
            }

            if (tag.length() > 23 && VERSION.SDK_INT < 26) {
                byte var5 = 0;
                byte var6 = 23;
                className = tag.substring(var5, var6);
                Intrinsics.checkNotNullExpressionValue(className, "this as java.lang.String…ing(startIndex, endIndex)");
            } else {
                className = tag;
            }

            return className;
        }

        protected void log(int priority, @Nullable String tag, @NotNull String message, @Nullable Throwable t) {
            Intrinsics.checkNotNullParameter(message, "message");
            if (message.length() < 4000) {
                if (priority == 7) {
                    Log.wtf(tag, message);
                } else {
                    Log.println(priority, tag, message);
                }

            } else {
                int i = 0;

                int end;
                for (int length = message.length(); i < length; i = end + 1) {
                    int newline = StringsKt.indexOf$default((CharSequence) message, '\n', i, false, 4, (Object) null);
                    newline = newline != -1 ? newline : length;

                    do {
                        end = Math.min(newline, i + 4000);
                        String var10000 = message.substring(i, end);
                        Intrinsics.checkNotNullExpressionValue(var10000, "this as java.lang.String…ing(startIndex, endIndex)");
                        String part = var10000;
                        if (priority == 7) {
                            Log.wtf(tag, part);
                        } else {
                            Log.println(priority, tag, part);
                        }

                        i = end;
                    } while (end < newline);
                }

            }
        }

        @Metadata(
                mv = {1, 7, 0},
                k = 1,
                d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002¢\u0006\u0002\u0010\u0002R\u0016\u0010\u0003\u001a\n \u0005*\u0004\u0018\u00010\u00040\u0004X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082T¢\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0007X\u0082T¢\u0006\u0002\n\u0000¨\u0006\t"},
                d2 = {"Ltimber/log/Timber$DebugTree$Companion;", "", "()V", "ANONYMOUS_CLASS", "Ljava/util/regex/Pattern;", "kotlin.jvm.PlatformType", "MAX_LOG_LENGTH", "", "MAX_TAG_LENGTH", "sources for library Gradle: com.jakewharton.timber:timber:5.0.1@aar"}
        )
        public static final class Companion {
            private Companion() {
            }

            // $FF: synthetic method
            public Companion(DefaultConstructorMarker $constructor_marker) {
                this();
            }
        }
    }

    @Metadata(
            mv = {1, 7, 0},
            k = 1,
            d1 = {"\u0000H\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0011\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0003\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\b\u000f\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002¢\u0006\u0002\u0010\u0002J\t\u0010\u000b\u001a\u00020\u0001H\u0097\bJ1\u0010\f\u001a\u00020\r2\n\b\u0001\u0010\u000e\u001a\u0004\u0018\u00010\u000f2\u0016\u0010\u0010\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00110\u0004\"\u0004\u0018\u00010\u0011H\u0017¢\u0006\u0002\u0010\u0012J\u0012\u0010\f\u001a\u00020\r2\b\u0010\u0013\u001a\u0004\u0018\u00010\u0014H\u0017J;\u0010\f\u001a\u00020\r2\b\u0010\u0013\u001a\u0004\u0018\u00010\u00142\n\b\u0001\u0010\u000e\u001a\u0004\u0018\u00010\u000f2\u0016\u0010\u0010\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00110\u0004\"\u0004\u0018\u00010\u0011H\u0017¢\u0006\u0002\u0010\u0015J1\u0010\u0016\u001a\u00020\r2\n\b\u0001\u0010\u000e\u001a\u0004\u0018\u00010\u000f2\u0016\u0010\u0010\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00110\u0004\"\u0004\u0018\u00010\u0011H\u0017¢\u0006\u0002\u0010\u0012J\u0012\u0010\u0016\u001a\u00020\r2\b\u0010\u0013\u001a\u0004\u0018\u00010\u0014H\u0017J;\u0010\u0016\u001a\u00020\r2\b\u0010\u0013\u001a\u0004\u0018\u00010\u00142\n\b\u0001\u0010\u000e\u001a\u0004\u0018\u00010\u000f2\u0016\u0010\u0010\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00110\u0004\"\u0004\u0018\u00010\u0011H\u0017¢\u0006\u0002\u0010\u0015J\u000e\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00010\u0018H\u0007J1\u0010\u0019\u001a\u00020\r2\n\b\u0001\u0010\u000e\u001a\u0004\u0018\u00010\u000f2\u0016\u0010\u0010\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00110\u0004\"\u0004\u0018\u00010\u0011H\u0017¢\u0006\u0002\u0010\u0012J\u0012\u0010\u0019\u001a\u00020\r2\b\u0010\u0013\u001a\u0004\u0018\u00010\u0014H\u0017J;\u0010\u0019\u001a\u00020\r2\b\u0010\u0013\u001a\u0004\u0018\u00010\u00142\n\b\u0001\u0010\u000e\u001a\u0004\u0018\u00010\u000f2\u0016\u0010\u0010\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00110\u0004\"\u0004\u0018\u00010\u0011H\u0017¢\u0006\u0002\u0010\u0015J9\u0010\u001a\u001a\u00020\r2\u0006\u0010\u001b\u001a\u00020\u00072\n\b\u0001\u0010\u000e\u001a\u0004\u0018\u00010\u000f2\u0016\u0010\u0010\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00110\u0004\"\u0004\u0018\u00010\u0011H\u0017¢\u0006\u0002\u0010\u001cJ,\u0010\u001a\u001a\u00020\r2\u0006\u0010\u001b\u001a\u00020\u00072\b\u0010\u001d\u001a\u0004\u0018\u00010\u000f2\u0006\u0010\u000e\u001a\u00020\u000f2\b\u0010\u0013\u001a\u0004\u0018\u00010\u0014H\u0014J\u001a\u0010\u001a\u001a\u00020\r2\u0006\u0010\u001b\u001a\u00020\u00072\b\u0010\u0013\u001a\u0004\u0018\u00010\u0014H\u0017JC\u0010\u001a\u001a\u00020\r2\u0006\u0010\u001b\u001a\u00020\u00072\b\u0010\u0013\u001a\u0004\u0018\u00010\u00142\n\b\u0001\u0010\u000e\u001a\u0004\u0018\u00010\u000f2\u0016\u0010\u0010\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00110\u0004\"\u0004\u0018\u00010\u0011H\u0017¢\u0006\u0002\u0010\u001eJ!\u0010\u001f\u001a\u00020\r2\u0012\u0010\t\u001a\n\u0012\u0006\b\u0001\u0012\u00020\u00010\u0004\"\u00020\u0001H\u0007¢\u0006\u0002\u0010 J\u0010\u0010\u001f\u001a\u00020\r2\u0006\u0010!\u001a\u00020\u0001H\u0007J\u0010\u0010\u001d\u001a\u00020\u00012\u0006\u0010\u001d\u001a\u00020\u000fH\u0007J\u0010\u0010\"\u001a\u00020\r2\u0006\u0010!\u001a\u00020\u0001H\u0007J\b\u0010#\u001a\u00020\rH\u0007J1\u0010$\u001a\u00020\r2\n\b\u0001\u0010\u000e\u001a\u0004\u0018\u00010\u000f2\u0016\u0010\u0010\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00110\u0004\"\u0004\u0018\u00010\u0011H\u0017¢\u0006\u0002\u0010\u0012J\u0012\u0010$\u001a\u00020\r2\b\u0010\u0013\u001a\u0004\u0018\u00010\u0014H\u0017J;\u0010$\u001a\u00020\r2\b\u0010\u0013\u001a\u0004\u0018\u00010\u00142\n\b\u0001\u0010\u000e\u001a\u0004\u0018\u00010\u000f2\u0016\u0010\u0010\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00110\u0004\"\u0004\u0018\u00010\u0011H\u0017¢\u0006\u0002\u0010\u0015J1\u0010%\u001a\u00020\r2\n\b\u0001\u0010\u000e\u001a\u0004\u0018\u00010\u000f2\u0016\u0010\u0010\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00110\u0004\"\u0004\u0018\u00010\u0011H\u0017¢\u0006\u0002\u0010\u0012J\u0012\u0010%\u001a\u00020\r2\b\u0010\u0013\u001a\u0004\u0018\u00010\u0014H\u0017J;\u0010%\u001a\u00020\r2\b\u0010\u0013\u001a\u0004\u0018\u00010\u00142\n\b\u0001\u0010\u000e\u001a\u0004\u0018\u00010\u000f2\u0016\u0010\u0010\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00110\u0004\"\u0004\u0018\u00010\u0011H\u0017¢\u0006\u0002\u0010\u0015J1\u0010&\u001a\u00020\r2\n\b\u0001\u0010\u000e\u001a\u0004\u0018\u00010\u000f2\u0016\u0010\u0010\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00110\u0004\"\u0004\u0018\u00010\u0011H\u0017¢\u0006\u0002\u0010\u0012J\u0012\u0010&\u001a\u00020\r2\b\u0010\u0013\u001a\u0004\u0018\u00010\u0014H\u0017J;\u0010&\u001a\u00020\r2\b\u0010\u0013\u001a\u0004\u0018\u00010\u00142\n\b\u0001\u0010\u000e\u001a\u0004\u0018\u00010\u000f2\u0016\u0010\u0010\u001a\f\u0012\b\b\u0001\u0012\u0004\u0018\u00010\u00110\u0004\"\u0004\u0018\u00010\u0011H\u0017¢\u0006\u0002\u0010\u0015R\u0016\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00010\u0004X\u0082\u000e¢\u0006\u0004\n\u0002\u0010\u0005R\u0011\u0010\u0006\u001a\u00020\u00078G¢\u0006\u0006\u001a\u0004\b\u0006\u0010\bR\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00010\nX\u0082\u0004¢\u0006\u0002\n\u0000¨\u0006'"},
            d2 = {"Ltimber/log/Timber$Forest;", "Ltimber/log/Timber$Tree;", "()V", "treeArray", "", "[Ltimber/log/Timber$Tree;", "treeCount", "", "()I", "trees", "Ljava/util/ArrayList;", "asTree", "d", "", "message", "", "args", "", "(Ljava/lang/String;[Ljava/lang/Object;)V", "t", "", "(Ljava/lang/Throwable;Ljava/lang/String;[Ljava/lang/Object;)V", "e", "forest", "", "i", "log", "priority", "(ILjava/lang/String;[Ljava/lang/Object;)V", "tag", "(ILjava/lang/Throwable;Ljava/lang/String;[Ljava/lang/Object;)V", "plant", "([Ltimber/log/Timber$Tree;)V", "tree", "uproot", "uprootAll", "v", "w", "wtf", "sources for library Gradle: com.jakewharton.timber:timber:5.0.1@aar"}
    )
    @SourceDebugExtension({"SMAP\nTimber.kt\nKotlin\n*S Kotlin\n*F\n+ 1 Timber.kt\ntimber/log/Timber$Forest\n+ 2 _Arrays.kt\nkotlin/collections/ArraysKt___ArraysKt\n+ 3 fake.kt\nkotlin/jvm/internal/FakeKt\n+ 4 ArraysJVM.kt\nkotlin/collections/ArraysKt__ArraysJVMKt\n*L\n1#1,456:1\n13579#2,2:457\n13579#2,2:459\n13579#2,2:461\n13579#2,2:463\n13579#2,2:465\n13579#2,2:467\n13579#2,2:469\n13579#2,2:471\n13579#2,2:473\n13579#2,2:475\n13579#2,2:477\n13579#2,2:479\n13579#2,2:481\n13579#2,2:483\n13579#2,2:485\n13579#2,2:487\n13579#2,2:489\n13579#2,2:491\n13579#2,2:493\n13579#2,2:495\n13579#2,2:497\n1#3:499\n37#4,2:500\n37#4,2:502\n37#4,2:504\n*S KotlinDebug\n*F\n+ 1 Timber.kt\ntimber/log/Timber$Forest\n*L\n277#1:457,2\n282#1:459,2\n287#1:461,2\n292#1:463,2\n297#1:465,2\n302#1:467,2\n307#1:469,2\n312#1:471,2\n317#1:473,2\n322#1:475,2\n327#1:477,2\n332#1:479,2\n337#1:481,2\n342#1:483,2\n347#1:485,2\n352#1:487,2\n357#1:489,2\n362#1:491,2\n367#1:493,2\n373#1:495,2\n378#1:497,2\n409#1:500,2\n421#1:502,2\n429#1:504,2\n*E\n"})
    public static final class Forest extends Tree {
        public void v(@NonNls @Nullable String message, @NotNull Object... args) {
            Intrinsics.checkNotNullParameter(args, "args");
            Object[] $this$forEach$iv = Timber.treeArray;
            int $i$f$forEach = false;
            int var5 = 0;

            for (int var6 = $this$forEach$iv.length; var5 < var6; ++var5) {
                Object element$iv = $this$forEach$iv[var5];
                int var9 = false;
                element$iv.v(message, Arrays.copyOf(args, args.length));
            }

        }

        public void v(@Nullable Throwable t, @NonNls @Nullable String message, @NotNull Object... args) {
            Intrinsics.checkNotNullParameter(args, "args");
            Object[] $this$forEach$iv = Timber.treeArray;
            int $i$f$forEach = false;
            int var6 = 0;

            for (int var7 = $this$forEach$iv.length; var6 < var7; ++var6) {
                Object element$iv = $this$forEach$iv[var6];
                int var10 = false;
                element$iv.v(t, message, Arrays.copyOf(args, args.length));
            }

        }

        public void v(@Nullable Throwable t) {
            Object[] $this$forEach$iv = Timber.treeArray;
            int $i$f$forEach = false;
            int var4 = 0;

            for (int var5 = $this$forEach$iv.length; var4 < var5; ++var4) {
                Object element$iv = $this$forEach$iv[var4];
                int var8 = false;
                element$iv.v(t);
            }

        }

        public void d(@NonNls @Nullable String message, @NotNull Object... args) {
            Intrinsics.checkNotNullParameter(args, "args");
            Object[] $this$forEach$iv = Timber.treeArray;
            int $i$f$forEach = false;
            int var5 = 0;

            for (int var6 = $this$forEach$iv.length; var5 < var6; ++var5) {
                Object element$iv = $this$forEach$iv[var5];
                int var9 = false;
                element$iv.d(message, Arrays.copyOf(args, args.length));
            }

        }

        @JvmStatic
        public void d(@Nullable Throwable t, @NonNls @Nullable String message, @NotNull Object... args) {
            Intrinsics.checkNotNullParameter(args, "args");
            Object[] $this$forEach$iv = Timber.treeArray;
            int $i$f$forEach = false;
            int var6 = 0;

            for (int var7 = $this$forEach$iv.length; var6 < var7; ++var6) {
                Object element$iv = $this$forEach$iv[var6];
                int var10 = false;
                element$iv.d(t, message, Arrays.copyOf(args, args.length));
            }

        }

        @JvmStatic
        public void d(@Nullable Throwable t) {
            Object[] $this$forEach$iv = Timber.treeArray;
            int $i$f$forEach = false;
            int var4 = 0;

            for (int var5 = $this$forEach$iv.length; var4 < var5; ++var4) {
                Object element$iv = $this$forEach$iv[var4];
                int var8 = false;
                element$iv.d(t);
            }

        }

        @JvmStatic
        public void i(@NonNls @Nullable String message, @NotNull Object... args) {
            Intrinsics.checkNotNullParameter(args, "args");
            Object[] $this$forEach$iv = Timber.treeArray;
            int $i$f$forEach = false;
            int var5 = 0;

            for (int var6 = $this$forEach$iv.length; var5 < var6; ++var5) {
                Object element$iv = $this$forEach$iv[var5];
                int var9 = false;
                element$iv.i(message, Arrays.copyOf(args, args.length));
            }

        }

        @JvmStatic
        public void i(@Nullable Throwable t, @NonNls @Nullable String message, @NotNull Object... args) {
            Intrinsics.checkNotNullParameter(args, "args");
            Object[] $this$forEach$iv = Timber.treeArray;
            int $i$f$forEach = false;
            int var6 = 0;

            for (int var7 = $this$forEach$iv.length; var6 < var7; ++var6) {
                Object element$iv = $this$forEach$iv[var6];
                int var10 = false;
                element$iv.i(t, message, Arrays.copyOf(args, args.length));
            }

        }

        @JvmStatic
        public void i(@Nullable Throwable t) {
            Object[] $this$forEach$iv = Timber.treeArray;
            int $i$f$forEach = false;
            int var4 = 0;

            for (int var5 = $this$forEach$iv.length; var4 < var5; ++var4) {
                Object element$iv = $this$forEach$iv[var4];
                int var8 = false;
                element$iv.i(t);
            }

        }

        @JvmStatic
        public void w(@NonNls @Nullable String message, @NotNull Object... args) {
            Intrinsics.checkNotNullParameter(args, "args");
            Object[] $this$forEach$iv = Timber.treeArray;
            int $i$f$forEach = false;
            int var5 = 0;

            for (int var6 = $this$forEach$iv.length; var5 < var6; ++var5) {
                Object element$iv = $this$forEach$iv[var5];
                int var9 = false;
                element$iv.w(message, Arrays.copyOf(args, args.length));
            }

        }

        @JvmStatic
        public void w(@Nullable Throwable t, @NonNls @Nullable String message, @NotNull Object... args) {
            Intrinsics.checkNotNullParameter(args, "args");
            Object[] $this$forEach$iv = Timber.treeArray;
            int $i$f$forEach = false;
            int var6 = 0;

            for (int var7 = $this$forEach$iv.length; var6 < var7; ++var6) {
                Object element$iv = $this$forEach$iv[var6];
                int var10 = false;
                element$iv.w(t, message, Arrays.copyOf(args, args.length));
            }

        }

        @JvmStatic
        public void w(@Nullable Throwable t) {
            Object[] $this$forEach$iv = Timber.treeArray;
            int $i$f$forEach = false;
            int var4 = 0;

            for (int var5 = $this$forEach$iv.length; var4 < var5; ++var4) {
                Object element$iv = $this$forEach$iv[var4];
                int var8 = false;
                element$iv.w(t);
            }

        }

        @JvmStatic
        public void e(@NonNls @Nullable String message, @NotNull Object... args) {
            Intrinsics.checkNotNullParameter(args, "args");
            Object[] $this$forEach$iv = Timber.treeArray;
            int $i$f$forEach = false;
            int var5 = 0;

            for (int var6 = $this$forEach$iv.length; var5 < var6; ++var5) {
                Object element$iv = $this$forEach$iv[var5];
                int var9 = false;
                element$iv.e(message, Arrays.copyOf(args, args.length));
            }

        }

        @JvmStatic
        public void e(@Nullable Throwable t, @NonNls @Nullable String message, @NotNull Object... args) {
            Intrinsics.checkNotNullParameter(args, "args");
            Object[] $this$forEach$iv = Timber.treeArray;
            int $i$f$forEach = false;
            int var6 = 0;

            for (int var7 = $this$forEach$iv.length; var6 < var7; ++var6) {
                Object element$iv = $this$forEach$iv[var6];
                int var10 = false;
                element$iv.e(t, message, Arrays.copyOf(args, args.length));
            }

        }

        @JvmStatic
        public void e(@Nullable Throwable t) {
            Object[] $this$forEach$iv = Timber.treeArray;
            int $i$f$forEach = false;
            int var4 = 0;

            for (int var5 = $this$forEach$iv.length; var4 < var5; ++var4) {
                Object element$iv = $this$forEach$iv[var4];
                int var8 = false;
                element$iv.e(t);
            }

        }

        @JvmStatic
        public void wtf(@NonNls @Nullable String message, @NotNull Object... args) {
            Intrinsics.checkNotNullParameter(args, "args");
            Object[] $this$forEach$iv = Timber.treeArray;
            int $i$f$forEach = false;
            int var5 = 0;

            for (int var6 = $this$forEach$iv.length; var5 < var6; ++var5) {
                Object element$iv = $this$forEach$iv[var5];
                int var9 = false;
                element$iv.wtf(message, Arrays.copyOf(args, args.length));
            }

        }

        @JvmStatic
        public void wtf(@Nullable Throwable t, @NonNls @Nullable String message, @NotNull Object... args) {
            Intrinsics.checkNotNullParameter(args, "args");
            Object[] $this$forEach$iv = Timber.treeArray;
            int $i$f$forEach = false;
            int var6 = 0;

            for (int var7 = $this$forEach$iv.length; var6 < var7; ++var6) {
                Object element$iv = $this$forEach$iv[var6];
                int var10 = false;
                element$iv.wtf(t, message, Arrays.copyOf(args, args.length));
            }

        }

        @JvmStatic
        public void wtf(@Nullable Throwable t) {
            Object[] $this$forEach$iv = Timber.treeArray;
            int $i$f$forEach = false;
            int var4 = 0;

            for (int var5 = $this$forEach$iv.length; var4 < var5; ++var4) {
                Object element$iv = $this$forEach$iv[var4];
                int var8 = false;
                element$iv.wtf(t);
            }

        }

        @JvmStatic
        public void log(int priority, @NonNls @Nullable String message, @NotNull Object... args) {
            Intrinsics.checkNotNullParameter(args, "args");
            Object[] $this$forEach$iv = Timber.treeArray;
            int $i$f$forEach = false;
            int var6 = 0;

            for (int var7 = $this$forEach$iv.length; var6 < var7; ++var6) {
                Object element$iv = $this$forEach$iv[var6];
                int var10 = false;
                element$iv.log(priority, message, Arrays.copyOf(args, args.length));
            }

        }

        @JvmStatic
        public void log(int priority, @Nullable Throwable t, @NonNls @Nullable String message, @NotNull Object... args) {
            Intrinsics.checkNotNullParameter(args, "args");
            Object[] $this$forEach$iv = Timber.treeArray;
            int $i$f$forEach = false;
            int var7 = 0;

            for (int var8 = $this$forEach$iv.length; var7 < var8; ++var7) {
                Object element$iv = $this$forEach$iv[var7];
                int var11 = false;
                element$iv.log(priority, t, message, Arrays.copyOf(args, args.length));
            }

        }

        @JvmStatic
        public void log(int priority, @Nullable Throwable t) {
            Object[] $this$forEach$iv = Timber.treeArray;
            int $i$f$forEach = false;
            int var5 = 0;

            for (int var6 = $this$forEach$iv.length; var5 < var6; ++var5) {
                Object element$iv = $this$forEach$iv[var5];
                int var9 = false;
                element$iv.log(priority, t);
            }

        }

        protected void log(int priority, @Nullable String tag, @NotNull String message, @Nullable Throwable t) {
            Intrinsics.checkNotNullParameter(message, "message");
            throw (Throwable) (new AssertionError());
        }

        @JvmStatic
        @NotNull
        public Tree asTree() {
            int $i$f$asTree = 0;
            return (Tree) this;
        }

        @JvmStatic
        @NotNull
        public final Tree tag(@NotNull String tag) {
            Intrinsics.checkNotNullParameter(tag, "tag");
            Tree[] var4 = Timber.treeArray;
            int var5 = var4.length;

            for (int var3 = 0; var3 < var5; ++var3) {
                Tree tree = var4[var3];
                tree.getExplicitTag$sources_for_library_Gradle__com_jakewharton_timber_timber_5_0_1_aar().set(tag);
            }

            return (Tree) this;
        }

        @JvmStatic
        public final void plant(@NotNull Tree tree) {
            Intrinsics.checkNotNullParameter(tree, "tree");
            boolean var2 = tree != (Forest) this;
            if (!var2) {
                int var3 = false;
                String var10 = "Cannot plant Timber into itself.";
                throw new IllegalArgumentException(var10.toString());
            } else {
                ArrayList var9 = Timber.trees;
                synchronized (var9) {
                    int var4 = false;
                    Timber.trees.add(tree);
                    Collection $this$toTypedArray$iv = (Collection) Timber.trees;
                    int $i$f$toTypedArray = false;
                    Timber.treeArray = (Tree[]) $this$toTypedArray$iv.toArray(new Tree[0]);
                    Unit var11 = Unit.INSTANCE;
                }
            }
        }

        @JvmStatic
        public final void plant(@NotNull Tree... trees) {
            Intrinsics.checkNotNullParameter(trees, "trees");
            Tree[] var4 = trees;
            int var5 = trees.length;

            boolean $i$f$toTypedArray;
            for (int var3 = 0; var3 < var5; ++var3) {
                Tree tree = var4[var3];
                if (tree == null) {
                    $i$f$toTypedArray = false;
                    String var13 = "trees contained null";
                    throw new IllegalArgumentException(var13.toString());
                }

                $i$f$toTypedArray = tree != (Forest) this;
                if (!$i$f$toTypedArray) {
                    int var7 = false;
                    String var14 = "Cannot plant Timber into itself.";
                    throw new IllegalArgumentException(var14.toString());
                }
            }

            ArrayList var9 = Timber.trees;
            synchronized (var9) {
                int var10 = false;
                Collections.addAll((Collection) Timber.trees, (Tree[]) Arrays.copyOf(trees, trees.length));
                Collection $this$toTypedArray$iv = (Collection) Timber.trees;
                $i$f$toTypedArray = false;
                Timber.treeArray = (Tree[]) $this$toTypedArray$iv.toArray(new Tree[0]);
                Unit var11 = Unit.INSTANCE;
            }
        }

        @JvmStatic
        public final void uproot(@NotNull Tree tree) {
            Intrinsics.checkNotNullParameter(tree, "tree");
            ArrayList var2 = Timber.trees;
            synchronized (var2) {
            }

            try {
                int var4 = false;
                boolean var5 = Timber.trees.remove(tree);
                boolean $i$f$toTypedArray;
                if (!var5) {
                    $i$f$toTypedArray = false;
                    String var12 = "Cannot uproot tree which is not planted: " + tree;
                    throw new IllegalArgumentException(var12.toString());
                }

                Collection $this$toTypedArray$iv = (Collection) Timber.trees;
                $i$f$toTypedArray = false;
                Timber.treeArray = (Tree[]) $this$toTypedArray$iv.toArray(new Tree[0]);
                Unit var10 = Unit.INSTANCE;
            } finally {
                ;
            }

        }

        public final void uprootAll() {
            ArrayList var1 = Timber.trees;
            synchronized (var1) {
                int var3 = false;
                Timber.trees.clear();
                Timber.treeArray = new Tree[0];
                Unit var5 = Unit.INSTANCE;
            }
        }

        @JvmStatic
        @NotNull
        public final List forest() {
            // $FF: Couldn't be decompiled
        }

        @JvmName(
                name = "treeCount"
        )
        public final int treeCount() {
            return Timber.treeArray.length;
        }

        private Forest() {
        }

        // $FF: synthetic method
        public Forest(DefaultConstructorMarker $constructor_marker) {
            this();
        }
    }
}

