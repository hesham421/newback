import com.erp.tenant.TenantContext;
import java.util.Arrays;

/** JMH-less harness: median ns/op over measured rounds, on a platform and a virtual thread. */
public class Bench {
    static long sink;
    static final int OPS = 5_000_000, WARM = 8, ROUNDS = 15;

    interface Op { long run(int n); }

    static double median(Op op) {
        for (int i = 0; i < WARM; i++) sink += op.run(OPS);
        double[] r = new double[ROUNDS];
        for (int i = 0; i < ROUNDS; i++) {
            long t = System.nanoTime();
            sink += op.run(OPS);
            r[i] = (System.nanoTime() - t) / (double) OPS;
        }
        Arrays.sort(r);
        return r[ROUNDS / 2];
    }

    static long currentLoop(int n) { long s = 0; for (int i = 0; i < n; i++) { Long v = TenantContext.current(); s += v == null ? 0 : v; } return s; }
    static long callAsLoop(int n) { long s = 0; for (int i = 0; i < n; i++) { s += TenantContext.callAs((long) (i & 7) + 1, TenantContext::current); } return s; }
    static long setClearLoop(int n) { long s = 0; for (int i = 0; i < n; i++) { TenantContext.set((long) (i & 7) + 1); s += TenantContext.current(); TenantContext.clear(); } return s; }

    static void suite(String thread) {
        double cur = median(Bench::currentLoop);
        double curIn = TenantContext.callAs(1L, () -> median(Bench::currentLoop));
        double callAs = median(Bench::callAsLoop);
        double setClear = median(Bench::setClearLoop);
        double setClearIn = TenantContext.callAs(1L, () -> median(Bench::setClearLoop));
        System.out.printf("%-9s current(no scope)=%.2f current(in callAs)=%.2f callAs(id,current)=%.2f set/current/clear(no scope)=%.2f set/current/clear(in callAs)=%.2f ns/op%n",
            thread, cur, curIn, callAs, setClear, setClearIn);
    }

    public static void main(String[] a) throws Exception {
        System.out.println("impl=" + a[0] + " java=" + System.getProperty("java.version"));
        suite("platform");
        Thread v = Thread.ofVirtual().start(() -> suite("virtual"));
        v.join();
        System.out.println("sink=" + (sink & 1));
    }
}
