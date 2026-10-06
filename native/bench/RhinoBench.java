import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

/**
 * RhinoBench — runs lcd_bench_rhino.js under Mozilla Rhino, replicating
 * JCM v2.3's execution model (ParsedScript.invokeRenderFunctions):
 *
 *   Context.enter(); cx.setLanguageVersion(Context.VERSION_ES6);
 *   ... func.call(cx, scope, scope, new Object[]{...}) per frame ...
 *
 * This measures the SAME algorithm the native and bun benchmarks run,
 * under the SAME engine family JCM uses in production
 * (com.lx862.mtrscripting.lib.org.mozilla.javascript is a repackaged
 * Rhino fork).
 *
 * Build/run:
 *   javac RhinoBench.java
 *   java -cp .:rhino.jar RhinoBench 20000
 */
public class RhinoBench {
    public static void main(String[] args) throws Exception {
        int framesArg = -1;
        String scriptPath = "lcd_bench_rhino.js";
        int optLevel = Integer.MIN_VALUE;
        for (String a : args) {
            if (a.startsWith("-opt")) {
                optLevel = Integer.parseInt(a.substring(4));
            } else if (framesArg < 0) {
                framesArg = Integer.parseInt(a);
            } else if (scriptPath.equals("lcd_bench_rhino.js")) {
                scriptPath = a;
            }
        }
        final int frames = framesArg < 0 ? 20000 : framesArg;

        String script = new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Path.of(scriptPath)), java.nio.charset.StandardCharsets.UTF_8);

        Context cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_ES6);
        /* JCM does not set an optimization level: Rhino default = 0
           (pure interpreter). Pass "-opt9" for the compiled-mode
           comparison. */
        if (optLevel != Integer.MIN_VALUE) {
            cx.setOptimizationLevel(optLevel);
        }
        try {
            Scriptable scope = cx.initStandardObjects();

            /* JCM-style globals: console.log */
            String shims =
                "var __RHINO_RUNNER__ = true;\n" +
                "var console = { log: function(s) { java.lang.System.out.println(String(s)); } };\n" +
                "var performance = { now: function() { return java.lang.System.nanoTime() / 1000000; } };\n" +
                "var process = { argv: [null, null, String(" + frames + ")] };\n" +
                "\"use strict\";\n";
            cx.evaluateString(scope, shims, "shims", 1, null);

            /* Parse the script (JCM: ScriptResourceUtil.executeScript) */
            cx.evaluateString(scope, script, scriptPath, 1, null);

            /* Grab the render entry point (JCM: scope.get("render", scope)) */
            Object fnObj = scope.get("renderFrameExternal", scope);
            if (!(fnObj instanceof Function)) {
                System.err.println("renderFrameExternal not found!");
                return;
            }
            Function renderFn = (Function) fnObj;

            /* Per-frame arguments, mirroring ParsedScript:
               func.call(cx, scope, scope, new Object[]{ ctx, state, wrapper }) */
            Scriptable ctxObj = cx.newObject(scope);
            Scriptable stateObj = cx.newObject(scope);
            Scriptable wrapperObj = cx.newObject(scope);

            /* Warmup */
            for (int i = 0; i < 200; i++) {
                renderFn.call(cx, scope, scope, new Object[]{ctxObj, stateObj, wrapperObj});
            }

            /* Timed loop */
            long t0 = System.nanoTime();
            for (int i = 0; i < frames; i++) {
                renderFn.call(cx, scope, scope, new Object[]{ctxObj, stateObj, wrapperObj});
            }
            long t1 = System.nanoTime();

            double ms = (t1 - t0) / 1e6;
            System.out.printf("frames=%d total=%.3f ms%n", frames, ms);
            System.out.printf("per-frame=%.2f us (%.0f ns)%n",
                    ms * 1000 / frames, (t1 - t0) / (double) frames);
        } finally {
            Context.exit();
        }
    }
}
