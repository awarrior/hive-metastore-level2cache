import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStore;
import org.apache.hadoop.hive.metastore.api.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class HMSHandlerCacheTest {

    private static HiveMetaStore.HMSHandler baseHandler = null;

    static {
        HiveConf hiveConf = new HiveConf(HMSHandlerCacheTest.class);
        hiveConf.addResource("hive-site.xml");
        System.out.println("meta->" + hiveConf.get("javax.jdo.option.ConnectionURL"));
        try {
            baseHandler = new HiveMetaStore.HMSHandler("CacheTest", hiveConf, false);
            baseHandler.init();
        } catch (MetaException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        if (args.length != 6) {
            System.err.println("args[0-5]: " +
                    "threadpool min, " +
                    "threadpool max, " +
                    "task num, " +
                    "read ratio, " +
                    "ratio seed1, " +
                    "row seed2");
            return;
        }

        // parse parameters
        int minWorkerThreads = Integer.parseInt(args[0]);
        int maxWorkerThreads = Integer.parseInt(args[1]);
        int tasknum = Integer.parseInt(args[2]);
        double readratio = Double.parseDouble(args[3]);
        Random r1 = new Random(Integer.parseInt(args[4]));
        Random r2 = new Random(Integer.parseInt(args[5]));

        // initialize executor
        ExecutorService ex = new ThreadPoolExecutor(minWorkerThreads, maxWorkerThreads,
                60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
//                new SynchronousQueue<Runnable>());

        // read db,tbl lines
        FileReader fr;
        try {
            // select b.NAME,a.TBL_NAME from TBLS a join DBS b on a.DB_ID=b.DB_ID
            // into outfile '/var/lib/mysql-files/DBxTBL.csv' fields terminated by ',' lines terminated by '\n';
            fr = new FileReader("DBxTBL.csv");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        BufferedReader br = new BufferedReader(fr);
        ArrayList<String> lines = new ArrayList<>();
        try {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        int linesLen = lines.size();

        // add threads to queue
        BlockingQueue<Runnable> bq = new LinkedBlockingQueue<>();
        double readratioUnroll = 2 * readratio - 1;
        for (int i = 0; i < tasknum; ++i) {
            boolean write = r1.nextDouble() < readratioUnroll;
            String[] rstr = lines.get((int) (r2.nextDouble() * linesLen)).split(",");
            if (rstr.length != 2) {
                System.err.println("meta data format must be db,tbl");
                return;
            }
            bq.add(new CacheThread(write, rstr[0], rstr[1]));
        }

        // submit tasks
        Date start = new Date();
        while (!bq.isEmpty()) {
            ex.submit(bq.poll());
        }
        ex.shutdown();
        try {
            ex.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Date end = new Date();

        // statistics
        double delta = (end.getTime() - start.getTime()) / 1000.0;
        double eval = tasknum / delta;
        System.out.println("Finished using " + delta + "s. ");
        System.out.println("Throughput is " + eval + "vps.");
    }

    static class CacheThread implements Runnable {

        private static Random r3 = new Random(0);

        private boolean writeBool = false;
        private String db = "default";
        private String tbl = "dual";

        CacheThread(boolean writeBool, String db, String tbl) {
            this.writeBool = writeBool;
            this.db = db;
            this.tbl = tbl;
        }

        private static HiveConf hiveConf = new HiveConf(HMSHandlerCacheTest.class);
        private static ThreadLocal<HiveMetaStore.HMSHandler> clients = new ThreadLocal<>();

        static {
            hiveConf.addResource("hive-site.xml");
        }

        @Override
        public void run() {
            try {
                Table tbl2 = baseHandler.get_table(db, tbl);
                if (writeBool) {
                    StorageDescriptor sd = tbl2.getSd();
                    List<FieldSchema> cols = sd.getCols();
                    int ridx = (int) (r3.nextDouble() * cols.size());
                    cols.get(ridx).setName("_cache_" + ridx);
                    baseHandler.alter_table(db, tbl, tbl2);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
