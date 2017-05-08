import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.SerDeInfo;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.hive.serde2.columnar.LazyBinaryColumnarSerDe;
import org.apache.thrift.TException;

import java.util.*;
import java.util.concurrent.*;

public class CacheTestDistRW {
    private static Random random = new Random();
    private static CountDownLatch cdl = null;

    public static void main(String[] args) {
        int uplimit = Integer.parseInt(args[0]);
        int cores = Integer.parseInt(args[1]);
        String prefix = args[2];
        int pall = Integer.parseInt(args[3]);
        int pend = Integer.parseInt(args[4]);
        double ratio = Double.parseDouble(args[5]);

        cdl = new CountDownLatch(uplimit);

        BlockingQueue<Runnable> bq = new LinkedBlockingQueue<>();

        System.out.println("Adding tasks..." + new Date());
        for (int i = 0; i < uplimit; ++i) {
            boolean write = random.nextDouble() < ratio;//0.1;
            bq.add(new CacheThread(write));
        }

        System.out.println("Sync..." + new Date());
        CacheManager cacheManager = CacheManager.newInstance("/appcom/hive-config/ehcache-terracotta.xml");
        Ehcache ehcache = cacheManager.getEhcache("basicCache");
        ehcache.put(new Element(prefix + pend, pend));
        Set<Integer> waitToVisit = new HashSet<>();
        waitToVisit.add(pend);
        while (waitToVisit.size() < pall) {
            for (int i = 1; i <= uplimit; ++i) {
                if (!waitToVisit.contains(i)) {
                    if (ehcache.get(prefix + i) != null) {
                        waitToVisit.add(i);
                    }
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        ExecutorService ex = Executors.newFixedThreadPool(cores);
        Date start = new Date();
        System.out.println("Starting..." + start);

        while (!bq.isEmpty()) {
            ex.submit(bq.poll());
        }

        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Date end = new Date();
        System.out.println("Ending..." + end);

        System.out.println("Diff=" + (end.getTime() - start.getTime()) + "ms");
        double delta = (end.getTime() - start.getTime()) / 1000.0;
        double eval = uplimit / delta;
        System.out.println("Eval=" + eval + "vps");

        ex.shutdown();
    }

    static class CacheThread implements Runnable {

        private boolean writeBool = false;

        private static StorageDescriptor storageDescriptor = null;
        private static List<FieldSchema> partColumns = new ArrayList<>();
        private static Map<String, String> tableParameters = new HashMap<>();

        private static HiveConf hiveConf = new HiveConf(CacheTestDistRW.class);
        private static ThreadLocal<HiveMetaStoreClient> clients = new ThreadLocal<>();

        static {
            hiveConf.addResource("hive-site.xml");

            List<FieldSchema> columns = new ArrayList<>();
            columns.add(new FieldSchema("b", "bigint", ""));
            SerDeInfo serdeInfo = new SerDeInfo("LBCSerDe", LazyBinaryColumnarSerDe.class.getCanonicalName(), new HashMap<String, String>());
            storageDescriptor = new StorageDescriptor(columns, null,
                    "org.apache.hadoop.hive.ql.io.RCFileInputFormat", "org.apache.hadoop.hive.ql.io.RCFileOutputFormat",
                    false, 0, serdeInfo, null, null, null);
        }

        CacheThread(boolean writeBool) {
            this.writeBool = writeBool;
        }

        @Override
        public void run() {
            try {
                HiveMetaStoreClient client = clients.get();
                if (client == null) {
                    client = new HiveMetaStoreClient(hiveConf);
                    clients.set(client);
                }

                // generate random tables
                if (writeBool) {
                    String tblname = "xxx" + random.nextInt(Integer.MAX_VALUE);
                    Table table = new Table(tblname, "dbx", "", 0, 0, 0, storageDescriptor, partColumns, tableParameters, "", "", "");
                    client.createTable(table);
                } else {
                    client.getTable("default", "xxx");
                }

                cdl.countDown();
            } catch (TException e) {
                e.printStackTrace();
            }
        }

    }

}
